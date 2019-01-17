package com.atomist.rolar.app.impl

import com.atomist.rolar.app.StreamLogs
import com.atomist.rolar.app.StreamLogsRequest
import com.atomist.rolar.app.StreamLogsResponse
import com.atomist.rolar.domain.LogService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service @Lazy
class StreamLogsImpl(private val logService: LogService): StreamLogs {
    override fun getLogs(request: StreamLogsRequest): StreamLogsResponse {
        return logService.logResultEvents(request.path, request.prioritize, request.historyLimit)
    }
}
