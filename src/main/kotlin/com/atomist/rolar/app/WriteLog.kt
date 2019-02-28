package com.atomist.rolar.app

import com.atomist.rolar.domain.model.IncomingLog
import java.util.function.Consumer

interface WriteLog {
    fun writeLog(request: WriteLogRequest, responseConsumer: Consumer<WriteLogResponse>)
}

data class WriteLogRequest(
        val path: List<String>,
        val closed: Boolean,
        val incomingLog: IncomingLog
)

typealias WriteLogResponse = Long
