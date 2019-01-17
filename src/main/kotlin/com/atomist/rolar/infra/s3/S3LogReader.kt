package com.atomist.rolar.infra.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogLine
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.*

@Service
class S3LogReader @Autowired
constructor(private val s3Client: AmazonS3,
            s3LoggingServiceProperties: S3LoggingServiceProperties) {

    private val mapper = jacksonObjectMapper()
    private val bucketName = s3LoggingServiceProperties.s3_logging_bucket

    fun readLogKeys(path: List<String>, lastS3Key: String? = null): List<LogKey> {
        val request = ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix("${path.joinToString("/")}/")
                .withMarker(lastS3Key)
        var objectListing = s3Client.listObjects(request)
        val allObjectSummaries = objectListing.getObjectSummaries()
        while (objectListing.nextMarker != null) {
            objectListing = s3Client.listObjects(request.withMarker(objectListing.nextMarker))
            allObjectSummaries.addAll(objectListing.getObjectSummaries())
        }
        return allObjectSummaries.map { s ->
            LogKey.fromS3ObjectSummary(s)
        }
    }

    fun readLogContent(logKey: LogKey): List<LogLine> {
        val s3Object = s3Client.getObject(GetObjectRequest(bucketName, logKey.toS3Key()))
        val logContent = s3Object.objectContent.bufferedReader().use { it.readText() }
        val logs: List<LogLine> = mapper.readValue(logContent)
        val preferMillisTimestampLogs = logs.map {
            val ts = if (it.timestampMillis != null) {
                val utcDateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS")
                utcDateFormat.timeZone = TimeZone.getTimeZone("GMT")
                utcDateFormat.format(it.timestampMillis)
            } else {
                it.timestamp
            }
            it.copy(timestamp = ts)
        }
        return preferMillisTimestampLogs
    }
}

fun LogKey.toS3Key(): String {
    return if (key != null) {
        key
    } else {
        val closeSuffix = if (isClosed) {
            "_CLOSED"
        } else {
            ""
        }
        "${path.joinToString("/")}/${LogKey.constructGmtFormat().format(Date(time))}Z_$host$closeSuffix.log"
    }
}

fun LogKey.Companion.fromS3ObjectSummary(s3ObjectSummary: S3ObjectSummary): LogKey {
    val key = s3ObjectSummary.key
    return LogKey(
            key.substringBefore("Z_").split("/").dropLast(1),
            key.substringAfter("Z_").substringBeforeLast("_CLOSED.log").substringBeforeLast(".log"),
            s3ObjectSummary.lastModified.time,
            LogKey.constructGmtFormat().parse(key.substringBefore("Z_").substringAfterLast("/")).time,
            key.endsWith("CLOSED.log"),
            false,
            key
    )
}

fun LogKey.Companion.constructGmtFormat(): SimpleDateFormat {
    val gmtFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS")
    gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
    return gmtFormat
}
