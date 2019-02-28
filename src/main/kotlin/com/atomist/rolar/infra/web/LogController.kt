package com.atomist.rolar.infra.web

import com.atomist.rolar.app.*
import com.atomist.rolar.domain.model.IncomingLog
import com.atomist.rolar.domain.model.LogLine
import com.atomist.rolar.domain.model.LogResults
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
class LogController @Autowired
constructor(private var getLogs: GetLogs, private var streamLogs: StreamLogs, private var writeLog: WriteLog) {

    @RequestMapping("api/logs/**")
    fun getLog(@RequestParam prioritize: Int? = 0,
               @RequestParam historyLimit: Int? = 0,
               request: HttpServletRequest): List<LogResults> {
        val path = constructPathFromUriWildcardSuffix(request)
        return getLogs.getLogs(GetLogsRequest(path, prioritize ?: 0, historyLimit ?: 0))
    }

    @RequestMapping(path = ["api/logs/**"], method = [RequestMethod.HEAD])
    fun getLog(): Long {
        return writeLog.writeLog(WriteLogRequest(listOf("service_testing"), true, IncomingLog(
                "unknown",
                listOf(LogLine(
                        "info",
                        "testing that service is up",
                        Date().time.toString(),
                        Date().time
                ))
        )))
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

    @RequestMapping(value = ["api/logs/**"], method = arrayOf(RequestMethod.POST))
    fun postLog(@RequestParam closed: Boolean? = false,
                @RequestBody incomingLog: IncomingLog,
                request: HttpServletRequest): Long {
        val path = constructPathFromUriWildcardSuffix(request)
        return writeLog.writeLog(WriteLogRequest(path, closed ?: false, incomingLog))
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

}
