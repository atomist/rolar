package com.atomist.rolar.infra.s3

import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogLine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.*
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Service
class S3LogWriter @Autowired
constructor(private val s3Client: S3Client,
            s3LoggingServiceProperties: S3LoggingServiceProperties) {

    private val mapper = jacksonObjectMapper()
    private val bucketName = s3LoggingServiceProperties.s3_logging_bucket

    fun write(key: LogKey, content: List<LogLine>) {
        val request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key.toS3Key())
        val body = RequestBody.fromString(mapper.writeValueAsString(content))
        s3Client.putObject(request.build(), body)
    }
}
