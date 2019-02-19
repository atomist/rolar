package com.atomist.rolar.infra.s3

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Config(val props: S3LoggingServiceProperties) {

    @Bean @Lazy
    fun s3Client(): S3Client {

        val creds = AwsBasicCredentials.create(props.aws_access_key_id,
                props.aws_secret_access_key)

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.of(props.region))
                .build()

    }
}
