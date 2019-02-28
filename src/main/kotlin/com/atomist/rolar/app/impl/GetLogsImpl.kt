package com.atomist.rolar.app.impl

import com.atomist.rolar.app.GetLogs
import com.atomist.rolar.app.GetLogsRequest
import com.atomist.rolar.app.GetLogsResponse
import com.atomist.rolar.domain.LogService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.function.Consumer

@Service @Lazy
class GetLogsImpl(private val logService: LogService): GetLogs {
    override fun getLogs(request: GetLogsRequest, responseConsumer: Consumer<GetLogsResponse>) {
        val events = logService.logResultEvents(request.path, request.prioritize, request.historyLimit)
        responseConsumer.accept(events)
    }
}
