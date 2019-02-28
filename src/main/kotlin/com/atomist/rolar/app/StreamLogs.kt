package com.atomist.rolar.app

import com.atomist.rolar.domain.model.LogResults
import java.util.function.Consumer

interface StreamLogs {
    fun getLogs(request: StreamLogsRequest, logResultConsumer: Consumer<LogResults>)
}

data class StreamLogsRequest(
        val path: List<String>,
        val prioritize: Int,
        val historyLimit: Int
)
