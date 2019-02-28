package com.atomist.rolar.app.impl

import com.atomist.rolar.app.WriteLog
import com.atomist.rolar.app.WriteLogRequest
import com.atomist.rolar.domain.LogService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service @Lazy
class WriteLogImpl(private val logService: LogService): WriteLog {
    override fun writeLog(request: WriteLogRequest) : Long {
        return logService.writeLogs(request.path, request.incomingLog, request.closed)
    }
}
