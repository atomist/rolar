package com.atomist.rolar.infra.web

import com.atomist.rolar.app.*
import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogLine
import com.atomist.rolar.domain.model.LogResults
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import org.springframework.util.AntPathMatcher
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.pattern.PathPattern
import reactor.core.publisher.Mono
import java.util.*


@CrossOrigin
@RestController @Lazy
class LogController(private var getLogs: GetLogs, private var streamLogs: StreamLogs, private var writeLog: WriteLog) {
    @GetMapping("api/logs/**")
    fun getLogs(@RequestParam(required = false, defaultValue = "0") prioritize: Int,
               @RequestParam(required = false, defaultValue = "0") historyLimit: Int,
               request: ServerWebExchange): Flux<LogResults> {
        return getLogs.getLogs(GetLogsRequest(getWildcardPath(request), prioritize, historyLimit))
    }

    @GetMapping(value = ["api/reactive/logs/**"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamLogs(@RequestParam(required = false, defaultValue = "0") prioritize: Int,
                @RequestParam(required = false, defaultValue = "0") historyLimit: Int,
                request: ServerWebExchange): Flux<LogResults> {
        return streamLogs.getLogs(StreamLogsRequest(getWildcardPath(request), prioritize, historyLimit))
    }

    @PostMapping(value = ["api/logs/**"])
    fun writeLog(@RequestParam(required = false, defaultValue = "false") closed: Boolean,
                 @RequestBody incomingLog: IncomingLog,
                 request: ServerWebExchange) : Mono<Long> {
        return writeLog.writeLog(WriteLogRequest(getWildcardPath(request), closed, incomingLog))
    }

    @RequestMapping(path = ["api/logs/**"], method = [RequestMethod.HEAD])
    fun getLog(): Mono<Long> {
        writeLog.writeLog(WriteLogRequest(path = listOf("service_testing"), incomingLog = IncomingLog(
                "unknown",
                listOf(LogLine(
                        "info",
                        "testing that service is up",
                        Date().time.toString(),
                        Date().time
                ))
        ), closed = false))
        return Mono.just(1)
    }

    fun getWildcardPath(exchange: ServerWebExchange): List<String> {
        val path = exchange.request.path.pathWithinApplication().value()
        val bestMatchPattern = exchange.getAttribute<PathPattern>(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)!!.patternString
        val finalPath = AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path)
        return finalPath.split("/")
    }
}
