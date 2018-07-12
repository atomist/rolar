package com.atomist.rolar

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import com.fasterxml.jackson.module.kotlin.*
import reactor.core.publisher.Flux
import java.text.SimpleDateFormat
import java.time.Duration

@Service
class LogsService @Autowired
constructor(private var s3Client: AmazonS3Client,
            val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    val mapper = jacksonObjectMapper()
    val bucketName = s3LoggingServiceProperties.s3_logging_bucket

    fun writeLogs(path: List<String>, incomingLog: IncomingLog, isClosed: Boolean = false): Long {
        val creationTime = Date().time
        val key = LogKey(path, incomingLog.host, creationTime, isClosed)
        s3Client.putObject(bucketName, key.toKeyName(), mapper.writeValueAsString(incomingLog.content))
        return creationTime
    }

    fun logResultEvents(path: List<String>, after: Long = 0L, prioritizeLastCount: Int = 0): Flux<LogResults> {
        val logFileRefGroupEvents = Flux.generate<LogFileRefGroup, Long>(
                { after }
        ) { state, sink ->
            val logFileRefs = retrieveLogFileRefs(path)
            if (logFileRefs.isEmpty()) {
                after
            } else {
                val lastKey = LogKey(logFileRefs.last().key)
                sink.next(LogFileRefGroup(logFileRefs, state))
                if (lastKey.isClosed) {
                    sink.complete()
                }
                lastKey.time
            }
        }
        return logFileRefGroupEvents.delayElements(Duration.ofSeconds(2))
            .flatMapSequential { lfrg ->
                val logRefs: List<LogRef> = if (lfrg.after == 0L) {
                    if (prioritizeLastCount > 0) {
                        val recent = lfrg.refs.takeLast(prioritizeLastCount).map { l -> LogRef(l) }
                        val history = lfrg.refs.dropLast(prioritizeLastCount).reversed().map { l ->
                            LogRef(l, true)
                        }
                        recent + history
                    } else {
                        lfrg.refs.map { l -> LogRef(l) }
                    }
                } else {
                    lfrg.refs.filter {  LogKey(it.key).time > lfrg.after }.map { l -> LogRef(l) }
                }
                Flux.fromIterable(logRefs)
            }.map { logRef ->
                val logContent = retrieveLogFileContent(logRef.file)
                val logLines = mapper.readValue<List<LogLine>>(logContent)
                LogResults(LogKey(logRef.file.key), logLines, logRef.prepend)
            }
    }

    private fun retrieveLogFileRefs(path: List<String>): List<LogFileRef> {
        val objectListing = s3Client.listObjects(ListObjectsRequest()
                .withBucketName(bucketName).withPrefix("${path.joinToString("/")}/"))
        return objectListing.getObjectSummaries().map { s ->
            LogFileRef(
                s.bucketName,
                s.key,
                s.size,
                s.lastModified,
                s.eTag
            )
        }
    }

    private fun retrieveLogFileContent(logFileRef: LogFileRef): String {
        val s3Object = s3Client.getObject(GetObjectRequest(bucketName, logFileRef.key))
        return s3Object.objectContent.bufferedReader().use { it.readText() }
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
