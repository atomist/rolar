package com.atomist.rolar.app

import com.atomist.rolar.domain.model.IncomingLog
import reactor.core.publisher.Mono

interface WriteLog {
    fun writeLog(request: WriteLogRequest) : Mono<Long>
}

data class WriteLogRequest(
        val path: List<String>,
        val closed: Boolean,
        val incomingLog: Mono<IncomingLog>
)
