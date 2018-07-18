package com.atomist.rolar

import com.nhaarman.mockito_kotlin.*
import io.kotlintest.specs.StringSpec
import reactor.test.StepVerifier
import java.time.Duration

class LogServiceTest : StringSpec({

    fun constructLogResults(batch: Int, index: Int, closed: Boolean = true, prepend: Boolean = false): LogResults {
        return LogResults(
                LogKeyMaker().constructLogKey(batch, index, closed, prepend),
                listOf(
                        LogLine(
                                "info",
                                "log message $batch.$index",
                                "$batch.$index"
                        )
                )
        )
    }

    val s3LogReader: S3LogReader = mock()

    doAnswer {
        val lk = it.arguments[0] as LogKey
        listOf(
                LogLine(
                        "info",
                        "log message ${lk.host}",
                        lk.host
                )
        )
    }.whenever(s3LogReader).readLogContent(any())

    val s3LogWriter: S3LogWriter = mock()

    val logService = LogsService(s3LogReader, s3LogWriter, Duration.ZERO)

    "read a single log message" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any())).doReturn(
                lkm.nextLogKeyBatch(1)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 0)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1,  1))
                .verifyComplete()
    }

    "read many log message across batches" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any())).doReturn(
                lkm.nextLogKeyBatch(2, false),
                lkm.nextLogKeyBatch(1, true)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 0)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 1, false))
                .expectNext(constructLogResults(1, 2, false))
                .expectNext(constructLogResults(2, 1, true))
                .verifyComplete()
    }

    "prioritize recent logs, ensure close is only on last one received" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any())).doReturn(
                lkm.nextLogKeyBatch(7, true)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 0, 2)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 6, false))
                .expectNext(constructLogResults(1, 7, false))
                .expectNext(constructLogResults(1, 1, false, true))
                .expectNext(constructLogResults(1, 2, false, true))
                .expectNext(constructLogResults(1, 3, false, true))
                .expectNext(constructLogResults(1, 4, false, true))
                .expectNext(constructLogResults(1, 5, true, true))
                .verifyComplete()
    }

    "prioritize recent logs and accept next set of logs" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any())).doReturn(
                lkm.nextLogKeyBatch(7, false),
                lkm.nextLogKeyBatch(2, true)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 0, 2)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 6, false))
                .expectNext(constructLogResults(1, 7, false))
                .expectNext(constructLogResults(1, 1, false, true))
                .expectNext(constructLogResults(1, 2, false, true))
                .expectNext(constructLogResults(1, 3, false, true))
                .expectNext(constructLogResults(1, 4, false, true))
                .expectNext(constructLogResults(1, 5, false, true))
                .expectNext(constructLogResults(2, 1, false))
                .expectNext(constructLogResults(2, 2, true))
                .verifyComplete()
    }

})

class LogKeyMaker {
    var nextKeyBatch = 0

    fun nextLogKeyBatch(count: Int, close: Boolean = true): List<LogKey> {
        nextKeyBatch++
        return (1..count).map { i ->
            constructLogKey(nextKeyBatch, i, close && i == count)
        }
    }

    fun constructLogKey(batch: Int, index: Int, closed: Boolean = true, prepend: Boolean = false): LogKey {
        return LogKey(
            listOf("a", "b", "c"),
            "$batch.$index",
            1531742400000 + (batch * 1000) + index,
            closed,
            prepend
        )
    }
}