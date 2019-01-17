package com.atomist.rolar.app.impl

import com.atomist.rolar.app.GetLogs
import com.atomist.rolar.app.GetLogsRequest
import com.atomist.rolar.app.GetLogsResponse
import com.atomist.rolar.domain.LogService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service @Lazy
class GetLogsImpl(private val logService: LogService): GetLogs {
    override fun getLogs(request: GetLogsRequest): GetLogsResponse {
        return logService.logResultEvents(request.path, request.prioritize, request.historyLimit)
    }
}
