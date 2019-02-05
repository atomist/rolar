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
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.concurrent.timerTask

@Service
class S3LogService
constructor(private val s3LogReader: S3LogReader,
            private val s3LogWriter: S3LogWriter) : LogService {

    private final val delay: Duration = Duration.ofSeconds(2)
    private final val logger: Logger = LoggerFactory.getLogger("s3logservice")

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
        val logEvents = Flux.create<LogKeysAfter> { sink ->
            run {
                var currentLastKey: LogKey? = null
                var lastS3Key: String? = null
                while (currentLastKey == null || !(currentLastKey.isClosed && currentLastKey.path == path)) {
                    logger.info("Reading keys for ${path.joinToString("/")} from ${lastS3Key}")
                    val logKeys = s3LogReader.readLogKeys(path, lastS3Key)
                    if (logKeys.isEmpty()) {
                        sink.next(LogKeysAfter(listOf(), lastS3Key))
                    } else {
                        val truncatedKeys = if (historyLimit == 0) logKeys else logKeys.takeLast(historyLimit)
                        sink.next(LogKeysAfter(truncatedKeys, lastS3Key))
                        currentLastKey = logKeys.last()
                        lastS3Key = currentLastKey.toS3Key()
                    }
                    Thread.sleep(delay.toMillis())
                }
                sink.complete()
            }
        }
        return logEvents.flatMapSequential {
            val logKeys = if (it.lastKey == null &&
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
