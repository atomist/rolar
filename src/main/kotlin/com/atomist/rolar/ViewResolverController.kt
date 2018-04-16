package com.atomist.rolar

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class ViewResolverController {

    @RequestMapping("/")
    fun index(): String {
        return "index"
    }

}