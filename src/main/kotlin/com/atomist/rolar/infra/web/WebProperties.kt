package com.atomist.rolar.infra.web

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("atomist.rolar.cors")
@Lazy
data class WebProperties(var allowedOrigin: String = "*")
