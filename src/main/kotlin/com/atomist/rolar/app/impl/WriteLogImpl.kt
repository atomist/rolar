package com.atomist.rolar.app.impl

import com.atomist.rolar.app.WriteLog
import com.atomist.rolar.app.WriteLogRequest
import com.atomist.rolar.app.WriteLogResponse
import com.atomist.rolar.domain.LogService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.function.Consumer

@Service @Lazy
class WriteLogImpl(private val logService: LogService): WriteLog {
    override fun writeLog(request: WriteLogRequest, responseConsumer: Consumer<WriteLogResponse>) {
        val response = logService.writeLogs(request.path, request.incomingLog, request.closed)
        responseConsumer.accept(response)
    }
}
