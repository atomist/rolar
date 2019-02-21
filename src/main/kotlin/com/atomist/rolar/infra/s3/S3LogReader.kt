package com.atomist.rolar.infra.s3

import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogLine
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.S3Object
import java.lang.RuntimeException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

@Service
class S3LogReader @Autowired
constructor(private val s3Client: S3Client,
            s3LoggingServiceProperties: S3LoggingServiceProperties) {

    private val mapper = jacksonObjectMapper()
    private val bucketName = s3LoggingServiceProperties.s3_logging_bucket
    private val logger = LoggerFactory.getLogger("s3-log-reader")

    fun readLogKeys(path: List<String>, lastS3Key: String? = null): List<LogKey> {
        try {
            val request = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .prefix("${path.joinToString("/")}/")
                    .marker(lastS3Key)

            var objectListing = s3Client.listObjects(request.build())
            val allObjectSummaries = objectListing.contents()
            while (objectListing.nextMarker() != null) {
                objectListing = s3Client.listObjects(request.marker(objectListing.nextMarker()).build())
                allObjectSummaries.addAll(objectListing.contents())
            }
            return allObjectSummaries.map { s ->
                LogKey.fromS3ObjectSummary(s)
            }
        } catch(ex: RuntimeException) {
            logger.error("Error reading logs", ex)
            throw RuntimeException("Error reading logs")
        }
    }

    fun readLogContent(logKey: LogKey): List<LogLine> {
        try {
            val request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(logKey.toS3Key())
            val s3Object = s3Client.getObject(request.build())
            val logContent = StreamUtils.copyToString(s3Object, Charset.defaultCharset())
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
        } catch(ex: RuntimeException) {
            logger.error("Error reading logs", ex)
            throw RuntimeException("Error reading log content")
        }
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

fun LogKey.Companion.fromS3ObjectSummary(s3ObjectSummary: S3Object): LogKey {
    val key = s3ObjectSummary.key()
    return LogKey(
            key.substringBefore("Z_").split("/").dropLast(1),
            key.substringAfter("Z_").substringBeforeLast("_CLOSED.log").substringBeforeLast(".log"),
            s3ObjectSummary.lastModified().toEpochMilli(),
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
