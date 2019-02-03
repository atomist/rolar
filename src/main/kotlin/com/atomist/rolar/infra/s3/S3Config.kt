package com.atomist.rolar.infra.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
class S3Config(val props: S3LoggingServiceProperties) {

    @Bean @Lazy
    fun s3Client(): AmazonS3 {
        val creds = BasicAWSCredentials(props.aws_access_key_id,
                props.aws_secret_access_key)
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(AWSStaticCredentialsProvider(creds))
                .withRegion(props.region)
                .build()
    }
}
