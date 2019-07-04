package com.atomist.rolar.infra.s3

import com.amazonaws.AmazonServiceException
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogLine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.*
import io.micrometer.core.annotation.Timed
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import java.util.concurrent.*
import org.springframework.retry.support.RetryTemplate
import java.lang.RuntimeException


@Service
class S3LogWriter @Autowired constructor(private val s3Client: AmazonS3, s3LoggingServiceProperties: S3LoggingServiceProperties) {
    private val mapper = jacksonObjectMapper()
    private val bucketName: String = s3LoggingServiceProperties.s3_logging_bucket
    private val executorService: ExecutorService = Executors.newCachedThreadPool()
    private val semaphore: Semaphore = Semaphore(10)
    private val retry = RetryTemplate()

    init {
        retry.setThrowLastExceptionOnExhausted(true)
        retry.setRetryPolicy(SimpleRetryPolicy(3, mapOf(
                AmazonServiceException::class.java to true,
                SdkClientException::class.java to true
        )))
        val backoffPolicy = ExponentialBackOffPolicy()
        backoffPolicy.initialInterval = 1500
        retry.setBackOffPolicy(backoffPolicy)
    }
    @Timed("s3-writer-timed")
    fun write(key: LogKey, content: List<LogLine>) {
        executorService.execute {
            try {
                semaphore.acquire()
                retry.execute<Unit, RuntimeException> {
                    s3Client.putObject(bucketName, key.toS3Key(), mapper.writeValueAsString(content))
                }
            } finally {
                semaphore.release()
            }
        }
    }
}
