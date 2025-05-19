package com.anaphygon.streaming.config

import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.http.*
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import jakarta.inject.Singleton

/**
 * Simple CORS Filter using Reactor Mono for development environment
 */
@Filter("/**")
@Singleton
@Requires(env = [Environment.DEVELOPMENT])
class SimpleCorsFilter : HttpServerFilter {

    private val allowedOrigins = listOf(
        "http://127.0.0.1:5500",
        "http://localhost:5500",
        "http://127.0.0.1:3000",
        "http://localhost:3000",
        "http://127.0.0.1:5173",
        "http://localhost:5173"
    )
    private val allowedMethods = "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH"
    private val allowedHeaders = "Authorization, Content-Type, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers"
    private val exposedHeaders = "Authorization, Content-Type"

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val origin = request.headers.get("Origin")
        val allowedOrigin = if (origin != null && allowedOrigins.contains(origin)) origin else allowedOrigins[0]

        return if (request.method == HttpMethod.OPTIONS) {
            // Handle preflight requests
            Mono.just(
                HttpResponse.ok<Any>()
                    .header("Access-Control-Allow-Origin", allowedOrigin)
                    .header("Access-Control-Allow-Methods", allowedMethods)
                    .header("Access-Control-Allow-Headers", allowedHeaders)
                    .header("Access-Control-Allow-Credentials", "true")
                    .header("Access-Control-Max-Age", "3600")
                    .header("Access-Control-Expose-Headers", exposedHeaders)
            )
        } else {
            // Handle actual requests
            Mono.from(chain.proceed(request))
                .map { response ->
                    response.header("Access-Control-Allow-Origin", allowedOrigin)
                        .header("Access-Control-Allow-Methods", allowedMethods)
                        .header("Access-Control-Allow-Headers", allowedHeaders)
                        .header("Access-Control-Allow-Credentials", "true")
                        .header("Access-Control-Expose-Headers", exposedHeaders)
                }
        }
    }
}