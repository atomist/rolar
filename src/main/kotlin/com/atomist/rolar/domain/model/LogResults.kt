package com.atomist.rolar.domain.model
data class LogResults(
        val lastKey: LogKey,
        val logs: List<LogLine>
)
