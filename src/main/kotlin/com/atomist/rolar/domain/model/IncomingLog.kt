package com.atomist.rolar.domain.model

data class IncomingLog(
        val host: String,
        val content: List<LogLine>
)
