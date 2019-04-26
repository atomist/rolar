package com.atomist.rolar.infra.web

import com.atomist.rolar.app.*
import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogLine
import com.atomist.rolar.domain.model.LogResults
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import javax.servlet.http.HttpServletRequest

@CrossOrigin
@RestController
class LogController @Autowired constructor(
        private var getLogs: GetLogs,
        private var streamLogs: StreamLogs,
        private var writeLog: WriteLog,
        private val rateLimiterRegistry: RateLimiterRegistry) {

    @RequestMapping("api/logs/**")
    fun getLog(@RequestParam prioritize: Int? = 0,
               @RequestParam historyLimit: Int? = 0,
               request: HttpServletRequest): List<LogResults> {
        return rateLimiterRegistry.rateLimiter("rolar").executeSupplier {
            val path = constructPathFromUriWildcardSuffix(request)
            val consumer = SimpleStateConsumer<GetLogsResponse>()
            getLogs.getLogs(GetLogsRequest(path, prioritize ?: 0, historyLimit ?: 0), consumer)
            consumer.value
        }
    }

    @RequestMapping(path = ["api/logs/**"], method = [RequestMethod.HEAD])
    fun getLog(): Long {
        return rateLimiterRegistry.rateLimiter("rolar").executeSupplier {
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
            consumer.value
        }
    }

    @GetMapping(value = ["api/reactive/logs/**"])
    fun getLogs(@RequestParam prioritize: Int? = 0,
                @RequestParam historyLimit: Int? = 0,
                request: HttpServletRequest): SseEmitter {
        return rateLimiterRegistry.rateLimiter("rolar").executeSupplier {
            val path = constructPathFromUriWildcardSuffix(request)
            val sseEmitter = SseEmitter()
            streamLogs.getLogs(StreamLogsRequest(path, prioritize ?: 0, historyLimit ?: 0), SsePublisher(sseEmitter))
            sseEmitter
        }
    }

    @RequestMapping(value = ["api/logs/**"], method = arrayOf(RequestMethod.POST))
    fun postLog(@RequestParam closed: Boolean? = false,
                @RequestBody incomingLog: IncomingLog,
                request: HttpServletRequest): Long {
        return rateLimiterRegistry.rateLimiter("rolar").executeSupplier {
            val path = constructPathFromUriWildcardSuffix(request)
            val consumer = SimpleStateConsumer<WriteLogResponse>()
            writeLog.writeLog(WriteLogRequest(path, closed ?: false, incomingLog), consumer)
            consumer.value
        }
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

    class SimpleStateConsumer<T>: Consumer<T> {
        private var _value: T? = null
        val value: T
            get() = _value!!

        override fun accept(t: T) {
            _value = t
        }
    }

}
