package com.atomist.rolar.infra.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config(val props: S3LoggingServiceProperties) {

    @Bean
    fun s3Client(): AmazonS3 {
        val credentials = BasicAWSCredentials(props.aws_access_key_id,
                props.aws_secret_access_key)
        return AmazonS3ClientBuilder.standard()
                .withCredentials(AWSStaticCredentialsProvider(credentials))
                .withRegion(props.region)
                .build()
    }
}
