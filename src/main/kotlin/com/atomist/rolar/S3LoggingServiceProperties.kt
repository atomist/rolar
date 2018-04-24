package com.atomist.rolar

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("vcap.services.s3-logging.credentials")
data class S3LoggingServiceProperties(var aws_access_key_id: String = "",
                                      var aws_secret_access_key: String = "",
                                      var s3_logging_bucket: String = "")
