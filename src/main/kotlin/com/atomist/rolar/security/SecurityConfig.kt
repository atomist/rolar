package com.atomist.rolar.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import JwtGateFilter
import com.atomist.rolar.S3LoggingServiceProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean

@Configuration
class SecurityConfig(val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    @Bean
    fun jwtFilter(): FilterRegistrationBean<JwtGateFilter> {
        val baseUrl = s3LoggingServiceProperties.auth_server_base_url
        val registrationBean = FilterRegistrationBean<JwtGateFilter>()
        registrationBean.filter = JwtGateFilter(baseUrl ?: "no base url")
        registrationBean.addUrlPatterns("/api/*")
        registrationBean.addUrlPatterns("/logs/*")
        if (baseUrl == null) {
            registrationBean.isEnabled = false
        }
        return registrationBean
    }
}
