import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.io.IOException

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

import org.springframework.web.filter.GenericFilterBean

import javax.servlet.http.HttpServletResponse

class JwtGateFilter(val authServerBaseUrl: String) : GenericFilterBean() {

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(req: ServletRequest,
                          res: ServletResponse,
                          chain: FilterChain) {
        val request = req as HttpServletRequest

        val authHeader = request.getHeader("Authorization") ?: "Bearer ${request.getParameter("auth")}"
        if (request.method == HttpMethod.GET.name) {
            val headers = HttpHeaders()
            headers.set("Authorization", authHeader)
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
            val authRequest = HttpEntity<String>("{\"query\": \"$query\"}", headers)
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
