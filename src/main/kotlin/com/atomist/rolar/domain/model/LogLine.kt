package com.atomist.rolar.domain.model

data class LogLine(
        val level: String,
        val message: String,
        val timestamp: String?,
        val timestampMillis: Long?)
