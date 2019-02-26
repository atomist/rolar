package com.atomist.rolar.infra.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogLine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.*
import org.slf4j.LoggerFactory
import java.lang.RuntimeException

@Service
class S3LogWriter @Autowired
constructor(private val s3Client: AmazonS3,
            s3LoggingServiceProperties: S3LoggingServiceProperties) {

    private val mapper = jacksonObjectMapper()
    private val bucketName = s3LoggingServiceProperties.s3_logging_bucket

    fun write(key: LogKey, content: List<LogLine>) {
        s3Client.putObject(bucketName, key.toS3Key(), mapper.writeValueAsString(content))
    }
}
