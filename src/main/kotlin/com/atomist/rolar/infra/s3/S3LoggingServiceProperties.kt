package com.atomist.rolar.infra.s3

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("vcap.services.s3-logging.credentials")
@Lazy
data class S3LoggingServiceProperties(var aws_access_key_id: String = "",
                                      var aws_secret_access_key: String = "",
                                      var s3_logging_bucket: String = "",
                                      var auth_server_base_url: String = "",
                                      var region: String = "")
