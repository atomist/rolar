package com.atomist.rolar.infra.web

import com.atomist.rolar.app.*
import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogLine
import com.atomist.rolar.domain.model.LogResults
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import javax.servlet.http.HttpServletRequest

@CrossOrigin
@RestController
class LogController @Autowired
constructor(private var getLogs: GetLogs, private var streamLogs: StreamLogs, private var writeLog: WriteLog) {

    @RequestMapping("api/logs/**")
    fun getLog(@RequestParam prioritize: Int? = 0,
               @RequestParam historyLimit: Int? = 0,
               request: HttpServletRequest): List<LogResults> {
        val path = constructPathFromUriWildcardSuffix(request)
        val consumer = SimpleStateConsumer<GetLogsResponse>()
        getLogs.getLogs(GetLogsRequest(path, prioritize ?: 0, historyLimit ?: 0), consumer)
        return consumer.value
    }

    @RequestMapping(path = ["api/logs/**"], method = [RequestMethod.HEAD])
    fun getLog(): Long {
        val consumer = SimpleStateConsumer<WriteLogResponse>()
        writeLog.writeLog(WriteLogRequest(listOf("service_testing"), true, IncomingLog(
                "unknown",
                listOf(LogLine(
                        "info",
                        "testing that service is up",
                        Date().time.toString(),
                        Date().time
                ))
        )), consumer)
        return consumer.value
    }

    @GetMapping(value = ["api/reactive/logs/**"])
    fun getLogs(@RequestParam prioritize: Int? = 0,
                @RequestParam historyLimit: Int? = 0,
                request: HttpServletRequest): SseEmitter {
        val path = constructPathFromUriWildcardSuffix(request)
        val sseEmitter = SseEmitter()
        streamLogs.getLogs(StreamLogsRequest(path, prioritize ?: 0, historyLimit ?: 0), SsePublisher(sseEmitter))
        return sseEmitter
    }

    @GetMapping(value = ["api/reactive/plain/logs/**"])
    fun getPlainLogs(@RequestParam prioritize: Int? = 0,
                @RequestParam historyLimit: Int? = 0,
                request: HttpServletRequest): ResponseBodyEmitter {
        val path = constructPathFromUriWildcardSuffix(request)
        val emitter = ResponseBodyEmitter()
        streamLogs.getLogs(StreamLogsRequest(path, prioritize ?: 0, historyLimit ?: 0), PlainPublisher(emitter))
        return emitter
    }

    @RequestMapping(value = ["api/logs/**"], method = arrayOf(RequestMethod.POST))
    fun postLog(@RequestParam closed: Boolean? = false,
                @RequestBody incomingLog: IncomingLog,
                request: HttpServletRequest): Long {
        val path = constructPathFromUriWildcardSuffix(request)
        val consumer = SimpleStateConsumer<WriteLogResponse>()
        writeLog.writeLog(WriteLogRequest(path, closed ?: false, incomingLog), consumer)
        return consumer.value
    }

    private fun constructPathFromUriWildcardSuffix(request: HttpServletRequest): List<String> {
        val uriPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
        val fullUri = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val pathString = fullUri.removePrefix(uriPattern.removeSuffix("**"))
        return pathString.split("/")
    }

    class SsePublisher(val sseEmitter: SseEmitter): Consumer<LogResults> {
        override fun accept(t: LogResults) {
            try {
                sseEmitter.send(t)
                if (t.lastKey.isClosed) {
                    sseEmitter.complete()
                }
            } catch(ioEx: IOException) {
                // ignore, somebody probably closed the connection
                sseEmitter.complete()
            }
        }

    }

    class PlainPublisher(val emitter: ResponseBodyEmitter): Consumer<LogResults> {
        override fun accept(t: LogResults) {
            try {
                val message = t.logs.map { it.message.trimEnd() }.joinToString("\n")
                emitter.send(message + "\n")
                if (t.lastKey.isClosed) {
                    emitter.complete()
                }
            } catch(ioEx: IOException) {
                // ignore, somebody probably closed the connection
                emitter.complete()
            }
        }

    }

    class SimpleStateConsumer<T>: Consumer<T> {
        private var _value: T? = null
        val value: T
            get() = _value!!

        override fun accept(t: T) {
            _value = t
        }
    }

}
