package com.atomist.rolar

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LogsController @Autowired
constructor(private var logsService: LogsService) {

    @RequestMapping("/logs/{env}/{host}")
    fun logs(@PathVariable env: String, @PathVariable host: String): String {
        return logsService.retriveAllLogs(env, host)
    }

}