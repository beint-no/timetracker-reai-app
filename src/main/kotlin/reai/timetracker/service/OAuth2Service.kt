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
        return try {
            logger.info("Exchanging authorization code for tokens...")

            val credentials = "$clientId:$clientSecret"
            val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

            val body: MultiValueMap<String, String> = LinkedMultiValueMap()
            body.add("grant_type", "authorization_code")
            body.add("code", code)
            body.add("redirect_uri", redirectUri)

            logger.debug("Calling token endpoint: $tokenEndpoint")

            val response = restClientBuilder.build()
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Basic $encodedCredentials")
                .body(body)
                .retrieve()
                .body(OAuth2TokenResponse::class.java)

            logger.info("Successfully exchanged code for tokens")
            response
        } catch (e: Exception) {
            logger.error("Error exchanging authorization code: ${e.message}", e)
            null
        }
    }

    fun refreshAccessToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): OAuth2TokenResponse? {
        return try {
            logger.info("Refreshing access token for client: $clientId")
            logger.debug("Refresh token (first 10 chars): ${refreshToken.take(10)}...")

            val credentials = "$clientId:$clientSecret"
            val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

            val body: MultiValueMap<String, String> = LinkedMultiValueMap()
            body.add("grant_type", "refresh_token")
            body.add("refresh_token", refreshToken)

            logger.debug("Calling token endpoint: $tokenEndpoint")
            logger.debug("Request body: grant_type=refresh_token, refresh_token=${refreshToken.take(10)}...")

            val response = restClientBuilder.build()
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Basic $encodedCredentials")
                .body(body)
                .retrieve()
                .body(OAuth2TokenResponse::class.java)

            logger.info("Successfully refreshed access token")
            response
        } catch (e: Exception) {
            logger.error("Error refreshing access token for client $clientId: ${e.message}", e)
            null
        }
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