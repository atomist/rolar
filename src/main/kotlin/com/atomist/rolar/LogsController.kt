package com.atomist.rolar

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.HandlerMapping
import reactor.core.publisher.Flux
import java.time.Duration
import javax.servlet.http.HttpServletRequest

@RestController
class LogsController @Autowired
constructor(private var logsService: LogsService) {

    @RequestMapping("api/logs/**")
    fun getLog(@RequestParam after: Long?, request: HttpServletRequest): LogResults {
        val path = constructPathFromUriWildcardSuffix(request)
        return logsService.retriveLogs(path, after ?: 0)
    }

    @GetMapping(value = "api/reactive/logs/**", produces = arrayOf(MediaType.TEXT_EVENT_STREAM_VALUE))
    fun getLogs(@RequestParam after: Long? = 0, request: HttpServletRequest): Flux<LogResults> {
        val path = constructPathFromUriWildcardSuffix(request)
        val interval = Flux.interval(Duration.ZERO, Duration.ofSeconds(3))
        val flux = Flux.generate<LogResults, Long>(
                { after ?: 0 }
        ) { state, sink ->
            val logResults = logsService.retriveLogs(path, state)
            sink.next(logResults)
            if (logResults.lastKey != null) {
                if (logResults.lastKey.isClosed) {
                    sink.complete()
                }
                logResults.lastKey.time
            } else {
                state
            }
        }
        return interval.zipWith(flux, 1).map { it.t2 }
    }

    @RequestMapping(value = "api/logs/**", method = arrayOf(RequestMethod.POST))
    fun postLog(@RequestParam closed: String?,
                @RequestBody incomingLog: IncomingLog,
                request: HttpServletRequest): Long {
        val path = constructPathFromUriWildcardSuffix(request)
        return logsService.writeLogs(path, incomingLog, closed != null)
    }

    private fun constructPathFromUriWildcardSuffix(request: HttpServletRequest): List<String> {
        val uriPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
        val fullUri = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val pathString = fullUri.removePrefix(uriPattern.removeSuffix("**"))
        return pathString.split("/")
    }

}
