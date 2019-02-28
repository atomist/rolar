package com.atomist.rolar.app.impl

import com.atomist.rolar.app.StreamLogs
import com.atomist.rolar.app.StreamLogsRequest
import com.atomist.rolar.domain.LogService
import com.atomist.rolar.domain.model.LogResults
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.function.Consumer

@Service @Lazy
class StreamLogsImpl(private val logService: LogService): StreamLogs {
    override fun getLogs(request: StreamLogsRequest, logResultConsumer: Consumer<LogResults>) {
        return logService.streamResultEvents(request.path, request.prioritize, request.historyLimit, logResultConsumer)
    }
}
