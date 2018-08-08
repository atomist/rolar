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
                                "$batch.$index",
                                null
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
                        "log message ${lk.toS3Key()}",
                        lk.host,
                        null
                )
        )
    }.whenever(s3LogReader).readLogContent(any())

    val s3LogWriter: S3LogWriter = mock()

    val logService = LogsService(s3LogReader, s3LogWriter, Duration.ZERO)

    "write a log message with string formatted timestamp" {
        val logContent = listOf(
                LogLine(
                        "info",
                        "log message",
                        "07/31/2018 14:05:23.136",
                        null
                )
        )
        logService.writeLogs(listOf("a", "b", "c"),
                IncomingLog(
                        "mbp",
                        logContent
                ))
        verify(s3LogWriter).write(LogKey(
                listOf("a", "b", "c"),
                "mbp",
                1533045923136,
                0,
                false
            ),
            logContent
        )
    }

    "write a log message with millis timestamp" {
        val logContent = listOf(
                LogLine(
                        "info",
                        "log message",
                        "07/31/2018 14:05:23.136",
                        1533045923137
                )
        )
        logService.writeLogs(listOf("a", "b", "c"),
                IncomingLog(
                        "mbp",
                        logContent
                ))
        verify(s3LogWriter).write(LogKey(
                listOf("a", "b", "c"),
                "mbp",
                1533045923137,
                0,
                false
        ),
                logContent
        )
    }

    "read a single log message" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any(), isNull())).doReturn(
                lkm.nextLogKeyBatch(1, 1)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"))
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1,  1))
                .verifyComplete()
    }

    "read many log message across batches" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any(), isNull())).doReturn(
                lkm.nextLogKeyBatch(1, 2, false)
        )
        whenever(s3LogReader.readLogKeys(any(), any())).doReturn(
                lkm.nextLogKeyBatch(2, 1, true)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"))
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 1, false))
                .expectNext(constructLogResults(1, 2, false))
                .expectNext(constructLogResults(2, 1, true))
                .verifyComplete()
    }

    "prioritize recent logs, ensure close is only on last one received" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any(), isNull())).doReturn(
                lkm.nextLogKeyBatch(1, 7, true)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 2)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 6, false))
                .expectNext(constructLogResults(1, 7, false))
                .expectNext(constructLogResults(1, 5, false, true))
                .expectNext(constructLogResults(1, 4, false, true))
                .expectNext(constructLogResults(1, 3, false, true))
                .expectNext(constructLogResults(1, 2, false, true))
                .expectNext(constructLogResults(1, 1, true, true))
                .verifyComplete()
    }

    "prioritize recent logs and accept next set of logs" {
        val lkm = LogKeyMaker()
        whenever(s3LogReader.readLogKeys(any(), isNull())).doReturn(
                lkm.nextLogKeyBatch(1, 7, false)
        )
        whenever(s3LogReader.readLogKeys(any(), any())).doReturn(
                lkm.nextLogKeyBatch(2, 2, true)
        )

        val logResults = logService.logResultEvents(listOf("a", "b", "c"), 2)
        StepVerifier.create(logResults)
                .expectNext(constructLogResults(1, 6, false))
                .expectNext(constructLogResults(1, 7, false))
                .expectNext(constructLogResults(1, 5, false, true))
                .expectNext(constructLogResults(1, 4, false, true))
                .expectNext(constructLogResults(1, 3, false, true))
                .expectNext(constructLogResults(1, 2, false, true))
                .expectNext(constructLogResults(1, 1, false, true))
                .expectNext(constructLogResults(2, 1, false))
                .expectNext(constructLogResults(2, 2, true))
                .verifyComplete()
    }

})

class LogKeyMaker {

    fun nextLogKeyBatch(batch: Int, count: Int, close: Boolean = true): List<LogKey> {
        return (1..count).map { i ->
            constructLogKey(batch, i, close && i == count)
        }
    }

    fun constructLogKey(batch: Int, index: Int, closed: Boolean = true, prepend: Boolean = false): LogKey {
        return LogKey(
            listOf("a", "b", "c"),
            "$batch.$index",
            1531742400000 + (batch * 1000) + index,
            1531742400000 + (batch * 1000) + index,
            closed,
            prepend,
            "$batch.$index"
        )
    }
}