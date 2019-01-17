package com.atomist.rolar.infra.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
class S3Config(val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    @Bean @Lazy
    fun s3Client(): AmazonS3 {
        val creds = BasicAWSCredentials(s3LoggingServiceProperties.aws_access_key_id,
                s3LoggingServiceProperties.aws_secret_access_key)
        return AmazonS3ClientBuilder.standard().withCredentials(AWSStaticCredentialsProvider(creds))
                .withRegion(s3LoggingServiceProperties.region).build()
    }
}
