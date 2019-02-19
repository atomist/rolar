package com.atomist.rolar.main

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = arrayOf("com.atomist.rolar"))
@EnableConfigurationProperties
class RolarApplication

fun main(args: Array<String>) {
    runApplication<RolarApplication>(*args)
}

