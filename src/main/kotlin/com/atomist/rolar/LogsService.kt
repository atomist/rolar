package com.atomist.rolar

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import com.fasterxml.jackson.module.kotlin.*

@Service
class LogsService @Autowired
constructor(private var s3Client: AmazonS3Client,
            @param:Value("\${aws.default-bucket}") private var bucketName: String) {

    val mapper = jacksonObjectMapper()

    fun retriveLogs(env: String, host: String): List<LogLine> {
        val logRefs = retrieveLogFileRefs(env, host)
        val logContents = logRefs.map { r -> retrieveLogFileContent(r) }
        val logLines = logContents.map { c -> mapper.readValue<List<LogLine>>(c) }
        return logLines.flatten()
    }

    private fun retrieveLogFileRefs(env: String, host: String): List<LogFileRef> {
        val objectListing = s3Client.listObjects(ListObjectsRequest()
                .withBucketName(bucketName).withPrefix("$env/$host/"))
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
        return s3Object.objectContent.bufferedReader().useLines { lines -> "[" + lines.joinToString(",") + "]" }
    }
}

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