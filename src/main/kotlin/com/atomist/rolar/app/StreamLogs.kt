package com.atomist.rolar.app

import com.atomist.rolar.domain.model.LogResults
import reactor.core.publisher.Flux

interface StreamLogs {
    fun getLogs(request: StreamLogsRequest): StreamLogsResponse
}

data class StreamLogsRequest(
        val path: List<String>,
        val prioritize: Int,
        val historyLimit: Int
)

typealias StreamLogsResponse = Flux<LogResults>
