package com.atomist.rolar.infra.web.security

import com.atomist.rolar.infra.s3.S3LoggingServiceProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig(val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    @Bean
    fun jwtFilter(): FilterRegistrationBean<JwtGateFilter> {
        val baseUrl = s3LoggingServiceProperties.auth_server_base_url
        val registrationBean = FilterRegistrationBean<JwtGateFilter>()
        registrationBean.filter = JwtGateFilter(baseUrl)
        registrationBean.addUrlPatterns("/api/*")
        registrationBean.addUrlPatterns("/logs/*")
        if (baseUrl.isEmpty()) {
            registrationBean.isEnabled = false
        }
        return registrationBean
    }
}
