package com.atomist.rolar.infra.web.security

import com.atomist.rolar.infra.s3.S3LoggingServiceProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ServerWebExchange

import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

//@Component
class JwtGateFilter(val s3LoggingServiceProperties: S3LoggingServiceProperties) : WebFilter {

    @Override
    override fun filter(serverWebExchange: ServerWebExchange,
                        chain: WebFilterChain) : Mono<Void> {
        val authHeader = serverWebExchange.request.headers.getFirst("Authorization") ?: "Bearer ${serverWebExchange.request.queryParams.getFirst("auth")}"
        if (serverWebExchange.request.method == HttpMethod.GET) {
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
                    "${s3LoggingServiceProperties.auth_server_base_url}/graphql",
                    authRequest,
                    AuthResult::class.java)
            val persons = authResponse.body?.data?.personByIdentity ?: listOf()
            val validRoots = persons.map { p -> p.team?.id }.filterNotNull()

            val root = serverWebExchange.request.path.pathWithinApplication().value().substringAfter("/logs/").substringBefore("/")
            return if (validRoots.contains(root)) {
                chain.filter(serverWebExchange)
            } else {
                serverWebExchange.response.statusCode = HttpStatus.UNAUTHORIZED
                serverWebExchange.response.writeWith { Flux.just("Invalid token for root '$root'") }
            }
        } else {
            return chain.filter(serverWebExchange)
        }
    }

}

data class AuthResult (var data: Data?)
data class Data (var personByIdentity: List<Person>?)
data class Person (var id: String?, var roles: List<Role>?, var team: Team?)
data class Role (var name: String?, var permissions: List<Permission>?)
data class Permission(var spec: String?)
data class Team(var id: String?, var name: String?)
