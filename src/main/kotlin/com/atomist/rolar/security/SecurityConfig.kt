package com.atomist.rolar.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import JwtGateFilter
import com.atomist.rolar.S3LoggingServiceProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
class SecurityConfig(val s3LoggingServiceProperties: S3LoggingServiceProperties) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowCredentials = true
        configuration.allowedHeaders = listOf("Access-Control-Allow-Headers", "Access-Control-Allow-Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "Origin", "Cache-Control", "Content-Type", "Authorization")
        configuration.allowedMethods = listOf("DELETE", "GET", "POST", "PATCH", "PUT")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
    
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
