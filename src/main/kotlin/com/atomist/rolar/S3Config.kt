package com.atomist.rolar

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config(@param:Value("\${aws.access-key-id}") private val accessKeyId: String,
               @param:Value("\${aws.access-key-secret}") private val accessKeySecret: String) {

    @Bean
    fun s3Client(): AmazonS3Client {
        val creds = BasicAWSCredentials(accessKeyId, accessKeySecret)
        return AmazonS3Client(creds)
    }
}