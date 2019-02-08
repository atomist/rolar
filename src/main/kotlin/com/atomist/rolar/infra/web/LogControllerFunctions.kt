package com.atomist.rolar.infra.web

import com.atomist.rolar.app.*
import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogKey
import com.atomist.rolar.domain.model.LogResults
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.*


@Component
@Lazy
class LogControllerFunctions(private var getLogs: GetLogs, private var streamLogs: StreamLogs, private var writeLog: WriteLog) {
    fun check(request: ServerRequest): Mono<ServerResponse> {
        val logs =  Flux.just(LogResults(LogKey(listOf(""), "unknown", Date().time, Date().time, false), listOf()))
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(logs, LogResults::class.java)
    }

    fun getLogs(request: ServerRequest): Mono<ServerResponse> {
        val path = getWildcardPath(request)
        val prioritize = request.queryParam("prioritize").orElse("0")
        val historyLimit = request.queryParam("prioritize").orElse("0")
        return if(path.isEmpty() || path.joinToString("") == "") {
            val logs =  Flux.just(LogResults(LogKey(path, "unknown", Date().time, Date().time, false), listOf()))
            ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(logs, LogResults::class.java)
        } else {
            val logs = getLogs.getLogs(GetLogsRequest(getWildcardPath(request), prioritize.toInt(), historyLimit.toInt()))
            ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(logs, LogResults::class.java)
        }
    }

    fun streamLogs(request: ServerRequest): Mono<ServerResponse> {
        val prioritize = request.queryParam("prioritize").orElse("0")
        val historyLimit = request.queryParam("prioritize").orElse("0")
        val logs = streamLogs.getLogs(StreamLogsRequest(getWildcardPath(request), prioritize.toInt(), historyLimit.toInt()))
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(logs, LogResults::class.java)
    }

    fun writeLog(request: ServerRequest) : Mono<ServerResponse> {
        val closed = request.queryParam("closed").orElse("false")
        val incomingLog = request.bodyToMono(IncomingLog::class.java)
        val writeResult = writeLog.writeLog(WriteLogRequest(getWildcardPath(request), closed!!.toBoolean(), incomingLog))
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(writeResult, Long::class.java)
    }

    fun getWildcardPath(request: ServerRequest): List<String> {
        val path = request.pathVariable("path").substring(1)
        return path.split("/")
    }
}
