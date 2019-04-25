package com.atomist.rolar.infra.web.security

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtGateFilter(val authServerBaseUrl: String) : GenericFilterBean() {

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(req: ServletRequest,
                          res: ServletResponse,
                          chain: FilterChain) {
        val httpRequest = req as HttpServletRequest
        val authCookie = httpRequest.cookies?.firstOrNull { it.name == "access_token"}
        val authHeader = httpRequest.getHeader("Authorization")
        val authParam = httpRequest.getParameter("auth")
        val auth = authCookie?.let {
            "Bearer ${it.value}"
        } ?: (authHeader ?: "Bearer $authParam")
        if (httpRequest.method == HttpMethod.GET.name) {
            val headers = HttpHeaders()
            headers.set("Authorization", auth)
            headers.set("Content-Type", "application/json")
            val query = """query User {
                personByIdentity {
                  id
                  roles {
                    name
                    permissions {
                      spec
                    }
                  }
                  team {
                    id
                    name
                  }
                }
            }"""
            val authRequest = HttpEntity("{\"query\": \"$query\"}", headers)
            try {
                val authResponse = RestTemplate().postForEntity(
                        "$authServerBaseUrl/graphql",
                        authRequest,
                        AuthResult::class.java)
                val persons = authResponse.body?.data?.personByIdentity ?: listOf()
                val validRoots = persons.map { p -> p.team?.id }.filterNotNull()

                val root = req.servletPath.substringAfter("/logs/").substringBefore("/")
                if (validRoots.contains(root)) {
                    chain.doFilter(req, res)
                } else {
                    (res as HttpServletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid token for root '$root'")
                }
            } catch(ex: HttpClientErrorException) {
                (res as HttpServletResponse).sendError(ex.rawStatusCode,
                        "Retrieving authentication details failed")
            }
        } else {
            chain.doFilter(req, res)
        }
    }

}

data class AuthResult (var data: Data?)
data class Data (var personByIdentity: List<Person>?)
data class Person (var id: String?, var roles: List<Role>?, var team: Team?)
data class Role (var name: String?, var permissions: List<Permission>?)
data class Permission(var spec: String?)
data class Team(var id: String?, var name: String?)
