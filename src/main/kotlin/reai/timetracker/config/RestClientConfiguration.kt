package reai.timetracker.config

import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration

class RestClientConfiguration {

    @Bean
    fun restClientBuilder(): RestClient.Builder {
        return RestClient.builder()
            .requestInterceptor(loggingInterceptor())
    }

    @Bean
    fun restClientCustomizer(): RestClientCustomizer {
        return RestClientCustomizer { builder ->
            builder
                .defaultHeader("User-Agent", "ReAI-TimeTracker/1.0")
                .defaultHeader("Accept", "application/json")
                .requestFactory(SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofSeconds(10))
                    setReadTimeout(Duration.ofSeconds(30))
                })
        }
    }

    private fun loggingInterceptor(): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request, body, execution ->
            val startTime = System.currentTimeMillis()

            println("RestClient Request: ${request.method} ${request.uri}")
            request.headers.forEach { (name, values) ->
                if (name.lowercase() == "authorization") {
                    println("Header: $name = $values")
                }
            }

            val response = execution.execute(request, body)

            val duration = System.currentTimeMillis() - startTime
            println("RestClient Response: ${response.statusCode} in ${duration}ms")

            response
        }
    }
}