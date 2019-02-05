package com.atomist.rolar.infra.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.ServerResponse

import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

@Configuration
@EnableWebFlux
class WebfluxConfiguration: WebFluxConfigurer {
    @Bean
    fun logRouter(logFunctions: LogControllerFunctions) = router {
        GET("/api/logs/{*path}").invoke(logFunctions::getLogs)
        GET("/api/reactive/logs/{*path}").invoke(logFunctions::streamLogs)
        POST("/api/logs/{*path}").and(contentType(MediaType.APPLICATION_JSON)).invoke(logFunctions::writeLog)
        HEAD("/api/logs/{*path}").invoke(logFunctions::check)
    }

    @Bean
    fun actuatorRouter() = router {
        GET("/actuator/health") { req -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(Health("UP")), Health::class.java)
        }
    }

    data class Health(val status: String)
}
