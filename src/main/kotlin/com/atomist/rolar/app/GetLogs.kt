package com.atomist.rolar.app

import com.atomist.rolar.domain.model.LogResults
import java.util.function.Consumer


interface GetLogs {
    fun getLogs(request: GetLogsRequest, responseConsumer: Consumer<GetLogsResponse>)
}

data class GetLogsRequest(
        val path: List<String>,
        val prioritize: Int,
        val historyLimit: Int
)

typealias GetLogsResponse = List<LogResults>







