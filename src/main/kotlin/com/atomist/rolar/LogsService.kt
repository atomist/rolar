package com.atomist.rolar

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import com.fasterxml.jackson.module.kotlin.*
import java.text.SimpleDateFormat

@Service
class LogsService @Autowired
constructor(private var s3Client: AmazonS3Client,
            @param:Value("\${aws.default-bucket}") private var bucketName: String) {

    val mapper = jacksonObjectMapper()

    fun writeLogs(path: List<String>, incomingLog: IncomingLog, isClosed: Boolean = false) {
        val key = LogKey(path, incomingLog.host, Date().time, isClosed)
        s3Client.putObject(bucketName, key.toKeyName(), mapper.writeValueAsString(incomingLog.content))
    }

    fun retriveLogs(path: List<String>, after: Long = 0): LogResults {
        val logRefs = retrieveLogFileRefs(path)
        val newLogRefs = logRefs.filter { LogKey(it.key).time > after }
        if (newLogRefs.isEmpty()) return LogResults(null, listOf())
        val logContents = newLogRefs.map { r -> retrieveLogFileContent(r) }
        val logLines = logContents.map { c -> mapper.readValue<List<LogLine>>(c) }
        return LogResults(LogKey(newLogRefs.last().key), logLines.flatten())
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
    val logs: List<LogLine>
)

data class IncomingLog(
        val host: String,
        val content: List<LogLine>
)

data class LogFileRef(
    val bucketName: String,
    val key: String,
    val size: Long,
    val lastModified: Date,
    val etag: String)


data class LogLine(
        val level: String,
        val message: String,
        val timestamp: String)
