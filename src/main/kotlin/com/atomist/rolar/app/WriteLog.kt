package com.atomist.rolar.app

import com.atomist.rolar.domain.model.IncomingLog

interface WriteLog {
    fun writeLog(request: WriteLogRequest)
}

data class WriteLogRequest(
        val path: List<String>,
        val closed: Boolean,
        val incomingLog: IncomingLog
)
