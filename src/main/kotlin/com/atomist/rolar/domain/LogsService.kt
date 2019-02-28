package com.atomist.rolar.domain

import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogResults
import java.util.function.Consumer

interface LogService {
    fun writeLogs(path: List<String>, incomingLog: IncomingLog, isClosed: Boolean = false): Long
    fun logResultEvents(path: List<String>,
                        prioritizeRecent: Int = 0,
                        historyLimit: Int = 0): List<LogResults>
    fun streamResultEvents(path: List<String>,
                           prioritizeRecent: Int = 0,
                           historyLimit: Int = 0,
                           logResultConsumer: Consumer<LogResults>)
}

data class LogKeysAfter (
        val keys: List<LogKey>,
        val lastKey: String?)

