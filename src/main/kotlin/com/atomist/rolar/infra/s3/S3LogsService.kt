package com.atomist.rolar.infra.s3

import com.atomist.rolar.domain.LogKeysAfter
import com.atomist.rolar.domain.LogService
import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogLine
import com.atomist.rolar.domain.model.LogResults
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@Service
class S3LogService
constructor(private val s3LogReader: S3LogReader,
            private val s3LogWriter: S3LogWriter) : LogService {

    val FORCE_CLOSED_KEY = "___ATOMIST_FLUX_CLOSED"

    private val streamExecutor = Executors.newScheduledThreadPool(10)
    private val latchExecutor = Executors.newCachedThreadPool()

    override fun writeLogs(path: List<String>, incomingLog: IncomingLog, isClosed: Boolean): Long {
            if (incomingLog.content.isEmpty()) {
                return ((-1).toLong())
            } else {
                val firstLog = incomingLog.content.first()
                val firstTimestamp: Long = if (firstLog.timestampMillis != null) {
                    firstLog.timestampMillis
                } else {
                    val utcDateParser = SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS")
                    utcDateParser.timeZone = TimeZone.getTimeZone("GMT")
                    utcDateParser.parse(firstLog.timestamp).time
                }
                val key = LogKey(path, incomingLog.host, firstTimestamp, 0, isClosed)
                s3LogWriter.write(key, incomingLog.content)
                return firstTimestamp
            }
    }

    override fun streamResultEvents(path: List<String>,
                                    prioritizeRecent: Int,
                                    historyLimit: Int,
                                    logResultConsumer: Consumer<LogResults>) {
        val started = LocalDateTime.now()
        val latch = CountDownLatch(1)
        var lastS3Key: String? = null
        var currentLastKey: LogKey? = null
        val task = streamExecutor.scheduleAtFixedRate({
            if (currentLastKey == null || !(currentLastKey!!.isClosed && currentLastKey!!.path == path)) {
                if(started.plusMinutes(30).isBefore(LocalDateTime.now())) {
                    getLogKeys(
                            LogKeysAfter(
                                    listOf(
                                            LogKey(listOf(),
                                                    "",
                                                    System.currentTimeMillis(),
                                                    System.currentTimeMillis(),
                                                    false,
                                                    false,
                                                    FORCE_CLOSED_KEY)
                                    ),
                                    FORCE_CLOSED_KEY
                            ), prioritizeRecent, historyLimit).forEach { handleLogKey(it, logResultConsumer) }
                    latch.countDown()
                }
                val logKeys = s3LogReader.readLogKeys(path, lastS3Key)
                if (logKeys.isEmpty()) {
                    getLogKeys(LogKeysAfter(listOf(), lastS3Key), prioritizeRecent, historyLimit)
                            .forEach { handleLogKey(it, logResultConsumer) }
                } else {
                    val truncatedKeys = if (historyLimit == 0) logKeys else logKeys.takeLast(historyLimit)
                    getLogKeys(LogKeysAfter(truncatedKeys, lastS3Key), prioritizeRecent, historyLimit)
                            .forEach { handleLogKey(it, logResultConsumer) }
                    currentLastKey = logKeys.last()
                    lastS3Key = currentLastKey!!.toS3Key()
                }
            } else {
                latch.countDown()
            }
        }, 0L, 2, TimeUnit.SECONDS)
        latchExecutor.execute { latch.await(); task.cancel(true) }
    }

    private fun handleLogKey(logKey: LogKey, logResultConsumer: Consumer<LogResults>) {
        if (logKey.key == FORCE_CLOSED_KEY) {
            logResultConsumer.accept(LogResults(logKey, listOf(LogLine("INFO", "The logging stream has closed after 30 minutes. Please press refresh to resume streaming", LocalDateTime.now().toString(), System.currentTimeMillis()))))
        } else {
            val logLines = s3LogReader.readLogContent(logKey)
            logResultConsumer.accept(LogResults(logKey, logLines))
        }
    }

    private fun getLogKeys(it: LogKeysAfter, prioritizeRecent: Int, historyLimit: Int): List<LogKey> {
        if (it.lastKey == FORCE_CLOSED_KEY) {
            return it.keys
        } else {
            return if (it.lastKey == null &&
                    prioritizeRecent != 0 && (historyLimit == 0 || prioritizeRecent < historyLimit)) {
                val isLogClosed = it.keys.isNotEmpty() && it.keys.last().isClosed
                val recentLogs = it.keys.takeLast(prioritizeRecent)
                val reversedHistory = it.keys.take(Math.max(0, it.keys.size - prioritizeRecent))
                        .reversed()
                        .map { it.copy(prepend = true) }
                val orderedLogKeys = recentLogs + reversedHistory
                if (isLogClosed) {
                    orderedLogKeys.mapIndexed { i, lk -> lk.copy(isClosed = i + 1 >= orderedLogKeys.size) }
                } else {
                    orderedLogKeys
                }
            } else {
                it.keys
            }
        }
    }

    override fun logResultEvents(path: List<String>,
                                 prioritizeRecent: Int,
                                 historyLimit: Int): List<LogResults> {
        val logKeys = s3LogReader.readLogKeys(path, null)
        val truncatedKeys = if (historyLimit == 0) logKeys else logKeys.takeLast(historyLimit)
        return truncatedKeys.map {
            logKey ->
            val logLines = s3LogReader.readLogContent(logKey)
            LogResults(logKey, logLines)
        }
    }

}
