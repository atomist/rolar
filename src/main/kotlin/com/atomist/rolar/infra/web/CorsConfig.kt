package com.atomist.rolar.infra.web

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

import java.util.Arrays

@Configuration
class CorsConfig(private val webProperties: WebProperties) {
    private val ALLOWED_HEADERS = listOf("x-requested-with", "authorization", "Content-Type", "Authorization", "credential", "X-XSRF-TOKEN")
    private val ALLOWED_METHODS = listOf("GET", "PUT", "POST", "DELETE", "OPTIONS")
    private val MAX_AGE = 3600L
    private val ALLOW_CREDENTIALS = true

    @Bean
    fun corsFilter(): FilterRegistrationBean<CorsFilter> {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = ALLOW_CREDENTIALS
        config.addAllowedOrigin(webProperties.allowedOrigin)
        ALLOWED_HEADERS.forEach {
            config.addAllowedHeader(it);
            config.addExposedHeader(it)
        }
        ALLOWED_METHODS.forEach { config.addAllowedMethod(it) }
        config.maxAge = MAX_AGE
        source.registerCorsConfiguration("/**", config)
        val bean = FilterRegistrationBean(CorsFilter(source))
        bean.order = 0
        return bean
    }
}
