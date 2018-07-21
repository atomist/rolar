package com.atomist.rolar

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import reactor.core.publisher.Flux
import java.text.SimpleDateFormat
import java.time.Duration

@Service
class LogsService @Autowired
constructor(private val s3LogReader: S3LogReader,
            private val s3LogWriter: S3LogWriter,
            private val delay: Duration = Duration.ofSeconds(2)) {

    fun writeLogs(path: List<String>, incomingLog: IncomingLog, isClosed: Boolean = false): Long {
        val creationTime = Date().time
        val key = LogKey(path, incomingLog.host, creationTime, 0, isClosed)
        s3LogWriter.write(key, incomingLog.content)
        return creationTime
    }

    fun logResultEvents(path: List<String>,
                        prioritizeRecent: Int = 0,
                        historyLimit: Int = 0): Flux<LogResults> {
        val logFileRefGroupEvents = Flux.generate<LogKeysAfter, String>(
                { null }
        ) { lastS3Key, sink ->
            val logKeys = s3LogReader.readLogKeys(path, lastS3Key)
            if (logKeys.isEmpty()) {
                lastS3Key
            } else {
                val sortedLogKeys = logKeys.sortedBy { it.lastModified }
                val truncatedKeys  = if (historyLimit == 0) sortedLogKeys else sortedLogKeys.takeLast(historyLimit)
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
                val logKeys = if (lka.lastS3Key == null &&
                        prioritizeRecent != 0 && (historyLimit == 0 || prioritizeRecent < historyLimit)) {
                    val isLogClosed = lka.keys.last().isClosed
                    val recentLogs = lka.keys.takeLast(prioritizeRecent)
                    val reversedHistory = lka.keys.take(lka.keys.size - prioritizeRecent)
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

}

data class LogKey(
        val path: List<String>,
        val host: String,
        val time: Long,
        val lastModified: Long,
        val isClosed: Boolean,
        val prepend: Boolean = false,
        private  val s3Key: String? = null
) {
    companion object {
        fun constructGmtFormat(): SimpleDateFormat {
            val gmtFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS")
            gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
            return gmtFormat
        }
        fun fromS3ObjectSummary(s3ObjectSummary: S3ObjectSummary): LogKey {
            val key = s3ObjectSummary.key
            return LogKey(
                key.substringBefore("Z_").split("/").dropLast(1),
                key.substringAfter("Z_").substringBeforeLast("_CLOSED.log").substringBeforeLast(".log"),
                s3ObjectSummary.lastModified.time,
                    constructGmtFormat().parse(key.substringBefore("Z_").substringAfterLast("/")).time,
                key.endsWith("CLOSED.log"),
            false,
                key
            )
        }
    }

    fun toS3Key(): String {
        return if (s3Key != null) {
            s3Key
        } else {
            val closeSuffix = if (isClosed) {
                "_CLOSED"
            } else {
                ""
            }
            "${path.joinToString("/")}/${constructGmtFormat().format(Date(time))}Z_$host$closeSuffix.log"
        }
    }
}

data class LogResults(
    val lastKey: LogKey,
    val logs: List<LogLine>
)

data class IncomingLog(
        val host: String,
        val content: List<LogLine>
)

data class LogKeysAfter (
        val keys: List<LogKey>,
        val lastS3Key: String?)


data class LogLine(
        val level: String,
        val message: String,
        val timestamp: String)
