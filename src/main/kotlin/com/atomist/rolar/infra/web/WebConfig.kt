package com.atomist.rolar.infra.web

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val webProperties: WebProperties): WebMvcConfigurer
{
    private val ALLOWED_HEADERS = "x-requested-with, authorization, Content-Type, Authorization, credential, X-XSRF-TOKEN"
    private val ALLOWED_METHODS = "GET, PUT, POST, DELETE, OPTIONS"
    private val MAX_AGE = 3600L
    private val EXPOSE_HEADERS = ALLOWED_HEADERS
    private val ALLOW_CREDENTIALS = true

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
                .allowedOrigins(webProperties.allowedOrigin)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALLOWED_HEADERS)
                .exposedHeaders(EXPOSE_HEADERS)
                .allowCredentials(ALLOW_CREDENTIALS)
                .maxAge(MAX_AGE)
    }

    override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
        val executor = ConcurrentTaskExecutor()
        configurer.setTaskExecutor(executor)
    }
}
