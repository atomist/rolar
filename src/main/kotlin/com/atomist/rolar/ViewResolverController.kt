package com.atomist.rolar

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class ViewResolverController {

    @RequestMapping("logs/{env}/{host}")
    fun index(@PathVariable env: String, @PathVariable host: String): String {
        return "index"
    }

}