package com.atomist.rolar

import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogLine
import com.atomist.rolar.domain.model.LogResults
import com.atomist.rolar.infra.s3.S3LogReader
import com.atomist.rolar.infra.s3.S3LogService
import com.atomist.rolar.infra.s3.S3LogWriter
import com.atomist.rolar.infra.s3.toS3Key
import com.nhaarman.mockito_kotlin.*
import io.kotlintest.specs.StringSpec
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.function.Consumer

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

    val logService = S3LogService(s3LogReader, s3LogWriter)

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
                Mono.just(IncomingLog(
                        "mbp",
                        logContent
                ))).subscribe()
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
                Mono.just(IncomingLog(
                        "mbp",
                        logContent
                ))).subscribe()
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

        val logResults = logService.streamResultEvents(listOf("a", "b", "c"))
        logResults.subscribe { r -> println(r.lastKey) }
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

        val logResults = logService.streamResultEvents(listOf("a", "b", "c"), 2)
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

        val logResults = logService.streamResultEvents(listOf("a", "b", "c"), 2)
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
