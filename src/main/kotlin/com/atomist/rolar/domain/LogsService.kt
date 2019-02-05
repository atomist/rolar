package com.atomist.rolar.domain

import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogResults
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface LogService {
    fun writeLogs(path: List<String>, incomingLog: Mono<IncomingLog>, isClosed: Boolean = false): Mono<Long>
    fun logResultEvents(path: List<String>,
                        prioritizeRecent: Int = 0,
                        historyLimit: Int = 0): Flux<LogResults>
}

data class LogKeysAfter (
        val keys: List<LogKey>,
        val lastKey: String?)

