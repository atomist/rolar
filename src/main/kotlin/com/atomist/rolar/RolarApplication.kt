package com.atomist.rolar

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RolarApplication

fun main(args: Array<String>) {
    runApplication<RolarApplication>(*args)
}

