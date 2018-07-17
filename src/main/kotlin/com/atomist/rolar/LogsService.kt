package com.atomist.rolar

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
        val key = LogKey(path, incomingLog.host, creationTime, isClosed)
        s3LogWriter.write(key, incomingLog.content)
        return creationTime
    }

    fun logResultEvents(path: List<String>, after: Long = 0L,
                        prioritizeLastCount: Int = 0): Flux<LogResults> {
        val logFileRefGroupEvents = Flux.generate<LogFileRefGroup, Long>(
                { after }
        ) { state, sink ->
            val logFileRefs = s3LogReader.readLogFileRefs(path)
            if (logFileRefs.isEmpty()) {
                after
            } else {
                val lastKey = LogKey(logFileRefs.last().key)
                sink.next(LogFileRefGroup(logFileRefs, state))
                if (lastKey.isClosed && lastKey.path == path) {
                    sink.complete()
                }
                lastKey.time
            }
        }
        return logFileRefGroupEvents.delayElements(delay)
            .flatMapSequential { lfrg ->
                val logRefs: List<LogRef> = if (lfrg.after == 0L && prioritizeLastCount != 0) {
                        lfrg.refs.mapIndexed { i, l ->
                            LogRef(l, i < (lfrg.refs.size - prioritizeLastCount))
                        }
                    } else {
                        lfrg.refs.filter {  LogKey(it.key).time > lfrg.after }.map { l -> LogRef(l) }
                    }
                Flux.fromIterable(logRefs)
            }.groupBy { lr -> lr.prepend }
            .flatMap { gf ->
                gf.map { logRef ->
                    val logLines = s3LogReader.readLogFileContent(logRef.file)
                    LogResults(LogKey(logRef.file.key), logLines, logRef.prepend)
                }
            }
    }

}

data class LogKey(
        val path: List<String>,
        val host: String,
        val time: Long,
        val isClosed: Boolean
) {
    companion object {
        val gmtFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS")
        init {
            gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    constructor(key: String): this(
            key.substringBefore("Z_").split("/").dropLast(1),
            key.substringAfter("Z_").substringBeforeLast("_CLOSED.log").substringBeforeLast(".log"),
            gmtFormat.parse(key.substringBefore("Z_").substringAfterLast("/")).time,
            key.endsWith("CLOSED.log")
    )

    fun toKeyName(): String {
        val closeSuffix = if (isClosed) { "_CLOSED" } else { "" }
        return "${path.joinToString("/")}/${gmtFormat.format(Date(time))}Z_${host}${closeSuffix}.log"
    }
}

data class LogResults(
    val lastKey: LogKey?,
    val logs: List<LogLine>,
    val prepend: Boolean = false
)

data class IncomingLog(
        val host: String,
        val content: List<LogLine>
)

data class LogRef(
        val file: LogFileRef,
        val prepend: Boolean = false
)

data class LogFileRef(
    val bucketName: String,
    val key: String,
    val size: Long,
    val lastModified: Date,
    val etag: String)

data class LogFileRefGroup (
        val refs: List<LogFileRef>,
        val after: Long)


data class LogLine(
        val level: String,
        val message: String,
        val timestamp: String)
