package com.atomist.rolar

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.HandlerMapping
import reactor.core.publisher.Flux
import javax.servlet.http.HttpServletRequest

@CrossOrigin
@RestController
class LogsController @Autowired
constructor(private var logsService: LogsService) {

    @RequestMapping("api/logs/**")
    fun getLog(@RequestParam prioritize: Int? = 0,
               @RequestParam historyLimit: Int? = 0,
               request: HttpServletRequest): List<LogResults> {
        val path = constructPathFromUriWildcardSuffix(request)
        return logsService.logResultEvents(path, prioritize ?: 0, historyLimit ?: 0)
                .collectList().block()!!.toList()
    }

    @GetMapping(value = "api/reactive/logs/**", produces = arrayOf(MediaType.TEXT_EVENT_STREAM_VALUE))
    fun getLogs(@RequestParam prioritize: Int? = 0,
                @RequestParam historyLimit: Int? = 0,
                request: HttpServletRequest): Flux<LogResults> {
        val path = constructPathFromUriWildcardSuffix(request)
        return logsService.logResultEvents(path, prioritize ?: 0, historyLimit ?: 0)
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
