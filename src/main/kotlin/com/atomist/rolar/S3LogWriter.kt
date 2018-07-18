package com.atomist.rolar

import com.amazonaws.services.s3.AmazonS3Client
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.*

@Service
class S3LogWriter @Autowired
constructor(private val s3Client: AmazonS3Client,
            private val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    private val mapper = jacksonObjectMapper()
    private val bucketName = s3LoggingServiceProperties.s3_logging_bucket

    fun write(key: LogKey, content: List<LogLine>) {
        s3Client.putObject(bucketName, key.toS3Key(), mapper.writeValueAsString(content))
    }

}
