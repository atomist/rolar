package com.atomist.rolar

import org.springframework.stereotype.Component

import javax.servlet.*
import javax.servlet.http.HttpServletResponse

@Component
class CorsFilter : Filter {

    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        val response = res as HttpServletResponse;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, content-type");
        chain.doFilter(req, res);
    }

    override fun init(filterConfig: FilterConfig) {}

    override fun destroy() {}

}