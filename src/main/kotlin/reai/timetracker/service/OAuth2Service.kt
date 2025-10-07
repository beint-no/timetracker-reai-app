package reai.timetracker.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import java.util.*

@Service
class OAuth2Service(
    private val restClientBuilder: RestClient.Builder
) {
    private val logger = LoggerFactory.getLogger(OAuth2Service::class.java)

    @Value("\${reai.oauth2.token-endpoint}")
    private lateinit var tokenEndpoint: String

    @Value("\${reai.oauth2.redirect-uri}")
    private lateinit var redirectUri: String

    fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        clientSecret: String
    ): OAuth2TokenResponse? {
        logger.info("Exchanging authorization code for tokens")

        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
        }

        return requestTokens(clientId, clientSecret, body, "exchange code")
    }

    fun refreshAccessToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): OAuth2TokenResponse? {
        logger.info("Refreshing access token for client: $clientId")
        logger.debug("Refresh token (first 10 chars): ${refreshToken.take(10)}...")

        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
        }

        return requestTokens(clientId, clientSecret, body, "refresh token")
    }

    private fun requestTokens(
        clientId: String,
        clientSecret: String,
        body: MultiValueMap<String, String>,
        operation: String
    ): OAuth2TokenResponse? {
        return try {
            val encodedCredentials = encodeCredentials(clientId, clientSecret)

            logger.debug("Calling token endpoint: $tokenEndpoint")

            restClientBuilder.build()
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Basic $encodedCredentials")
                .body(body)
                .retrieve()
                .body(OAuth2TokenResponse::class.java)
                .also { logger.info("Successfully completed: $operation") }
        } catch (e: Exception) {
            logger.error("Error during $operation: ${e.message}", e)
            null
        }
    }

    private fun encodeCredentials(clientId: String, clientSecret: String): String {
        val credentials = "$clientId:$clientSecret"
        return Base64.getEncoder().encodeToString(credentials.toByteArray())
    }
}

/**
 * OAuth2 Token Response
 */
data class OAuth2TokenResponse(
    val access_token: String,
    val refresh_token: String?,
    val token_type: String,
    val expires_in: Int,
    val scope: String?
)