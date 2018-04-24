package com.atomist.rolar

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config(val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    @Bean
    fun s3Client(): AmazonS3Client {
        val creds = BasicAWSCredentials(s3LoggingServiceProperties.aws_access_key_id,
                s3LoggingServiceProperties.aws_secret_access_key)
        return AmazonS3Client(creds)
    }
}