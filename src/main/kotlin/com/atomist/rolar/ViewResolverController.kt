package com.atomist.rolar

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest
import org.springframework.web.servlet.ModelAndView

@Controller
class ViewResolverController {

    @RequestMapping("logs/**")
    fun index(@RequestParam after: Long?, request: HttpServletRequest): ModelAndView {
        val path = constructPathFromUriWildcardSuffix(request)
        val auth = request.getHeader("Authorization")
        val mav = ModelAndView("index")
        mav.addObject("path", path.joinToString("/"))
        mav.addObject("after", after ?: 0)
        mav.addObject("auth", auth)
        return mav
    }

    private fun constructPathFromUriWildcardSuffix(request: HttpServletRequest): List<String> {
        val uriPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
        val fullUri = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val pathString = fullUri.removePrefix(uriPattern.removeSuffix("**"))
        return pathString.split("/")
    }

}
