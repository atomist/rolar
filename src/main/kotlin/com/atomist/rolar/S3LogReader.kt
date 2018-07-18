package com.atomist.rolar

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class S3LogReader @Autowired
constructor(private val s3Client: AmazonS3Client,
            private val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    private val mapper = jacksonObjectMapper()
    private val bucketName = s3LoggingServiceProperties.s3_logging_bucket

    fun readLogKeys(path: List<String>): List<LogKey> {
        val objectListing = s3Client.listObjects(ListObjectsRequest()
                .withBucketName(bucketName).withPrefix("${path.joinToString("/")}/"))
        return objectListing.getObjectSummaries().map { s ->
            LogKey.fromS3Key(s.key)
        }
    }

    fun readLogContent(logKey: LogKey): List<LogLine> {
        val s3Object = s3Client.getObject(GetObjectRequest(bucketName, logKey.toS3Key()))
        val logContent = s3Object.objectContent.bufferedReader().use { it.readText() }
        return mapper.readValue(logContent)
    }

}
