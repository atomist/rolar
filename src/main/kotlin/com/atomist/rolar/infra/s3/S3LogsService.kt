package com.atomist.rolar.infra.s3

import com.atomist.rolar.domain.LogKeysAfter
import com.atomist.rolar.domain.LogService
import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogResults
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*

@Service
class S3LogService
constructor(private val s3LogReader: S3LogReader,
            private val s3LogWriter: S3LogWriter) : LogService {

    private final val delay: Duration = Duration.ofSeconds(2)

    override fun writeLogs(path: List<String>, incomingLog: Mono<IncomingLog>, isClosed: Boolean): Mono<Long> {
        return incomingLog.map {
            if (it.content.isEmpty()) {
                return@map ((-1).toLong())
            } else {
                val firstLog = it.content.first()
                val firstTimestamp: Long = if (firstLog.timestampMillis != null) {
                    firstLog.timestampMillis
                } else {
                    val utcDateParser = SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS")
                    utcDateParser.timeZone = TimeZone.getTimeZone("GMT")
                    utcDateParser.parse(firstLog.timestamp).time
                }
                val key = LogKey(path, it.host, firstTimestamp, 0, isClosed)
                s3LogWriter.write(key, it.content)
                return@map firstTimestamp
            }
        }

    }

    override fun streamResultEvents(path: List<String>,
                                    prioritizeRecent: Int,
                                    historyLimit: Int): Flux<LogResults> {
        val logFileRefGroupEvents = Flux.generate<LogKeysAfter, String>(
                { null }
        ) { lastS3Key, sink ->
            val logKeys = s3LogReader.readLogKeys(path, lastS3Key)
            if (logKeys.isEmpty()) {
                sink.next(LogKeysAfter(listOf(), lastS3Key))
                lastS3Key
            } else {
                val truncatedKeys  = if (historyLimit == 0) logKeys else logKeys.takeLast(historyLimit)
                sink.next(LogKeysAfter(truncatedKeys, lastS3Key))
                val lastKey = logKeys.last()
                if (lastKey.isClosed && lastKey.path == path) {
                    sink.complete()
                }
                lastKey.toS3Key()
            }
        }
        return logFileRefGroupEvents.delayElements(delay)
                .flatMapSequential { lka ->
                    val logKeys = if (lka.lastKey == null &&
                            prioritizeRecent != 0 && (historyLimit == 0 || prioritizeRecent < historyLimit)) {
                        val isLogClosed = lka.keys.isNotEmpty() &&  lka.keys.last().isClosed
                        val recentLogs = lka.keys.takeLast(prioritizeRecent)
                        val reversedHistory = lka.keys.take(Math.max(0, lka.keys.size - prioritizeRecent))
                                .reversed()
                                .map { it.copy(prepend = true)}
                        val orderedLogKeys = recentLogs + reversedHistory
                        if (isLogClosed) {
                            orderedLogKeys.mapIndexed { i, lk -> lk.copy(isClosed = i + 1 >= orderedLogKeys.size) }
                        } else {
                            orderedLogKeys
                        }
                    } else {
                        lka.keys
                    }
                    Flux.fromIterable(logKeys)
                }
                .map { logKey ->
                    val logLines = s3LogReader.readLogContent(logKey)
                    LogResults(logKey, logLines)
                }
    }

    override fun logResultEvents(path: List<String>,
                                 prioritizeRecent: Int,
                                 historyLimit: Int): Flux<LogResults> {
        val logKeys = s3LogReader.readLogKeys(path, null)
        val truncatedKeys = if (historyLimit == 0) logKeys else logKeys.takeLast(historyLimit)
        return Flux.fromIterable(truncatedKeys)
                .map { logKey ->
                    val logLines = s3LogReader.readLogContent(logKey)
                    LogResults(logKey, logLines)
                }
    }

}
