package com.atomist.rolar

import com.nhaarman.mockito_kotlin.*
import io.kotlintest.specs.StringSpec
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

class LogServiceTest : StringSpec({

    fun constructLogResults(batch: Int, index: Int, closed: Boolean = true, prepend: Boolean = false): LogResults {
        return LogResults(
                LogKey(
                        listOf("a", "b", "c"),
                        "mbp",
                        1531742400000 + (batch * 1000) + index,
                        closed
                ),
                listOf(
                        LogLine(
                                "info",
                                "log message $batch.$index",
                                "$batch.$index"
                        )
                ),
                prepend
        )
    }

    val s3LogReader: S3LogReader = mock()

    doAnswer {
        val lfr = it.arguments[0] as LogFileRef
        listOf(
                LogLine(
                        "info",
                        "log message ${lfr.etag}",
                        lfr.etag
                )
        )
    }.whenever(s3LogReader).readLogFileContent(any())

    val s3LogWriter: S3LogWriter = mock()

    val logService = LogsService(s3LogReader, s3LogWriter, Duration.ZERO)

    "read a single log message" {
        val lfrm = LogFileRefMaker()
        whenever(s3LogReader.readLogFileRefs(any())).doReturn(
                lfrm.nextLogFileRef(1)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 0)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1,  1))
                .verifyComplete()
    }

    "read many log message across batches" {
        val lfrm = LogFileRefMaker()
        whenever(s3LogReader.readLogFileRefs(any())).doReturn(
                lfrm.nextLogFileRef(2, false),
                lfrm.nextLogFileRef(1, true)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 0)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 1, false))
                .expectNext(constructLogResults(1, 2, false))
                .expectNext(constructLogResults(2, 1, true))
                .verifyComplete()
    }

    "prioritize recent logs" {
        val lfrm = LogFileRefMaker()
        whenever(s3LogReader.readLogFileRefs(any())).doReturn(
                lfrm.nextLogFileRef(7)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 0, 2)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 1, false, true))
                .expectNext(constructLogResults(1, 2, false, true))
                .expectNext(constructLogResults(1, 3, false, true))
                .expectNext(constructLogResults(1, 4, false, true))
                .expectNext(constructLogResults(1, 5, false, true))
                .expectNext(constructLogResults(1, 6, false))
                .expectNext(constructLogResults(1, 7, true))
                .verifyComplete()
    }
})

class LogFileRefMaker {
    var nextLogFileRefBatch = 0

    fun nextLogFileRef(count: Int, close: Boolean = true): List<LogFileRef> {
        nextLogFileRefBatch++
        return (1..count).map { i ->
            val batch = nextLogFileRefBatch.toString().padStart(2, '0')
            val index = i.toString().padStart(3, '0')
            val closed = if (close && i == count) "_CLOSED" else ""
            LogFileRef(
                    "bucket",
                    "a/b/c/2018-07-16_12.00.$batch.${index}Z_mbp$closed.log",
                    1,
                    Date(),
                    "$nextLogFileRefBatch.$i"
            )
        }
    }
}