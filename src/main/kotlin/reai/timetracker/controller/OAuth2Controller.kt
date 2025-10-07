package reai.timetracker.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reai.timetracker.service.OAuth2Service
import reai.timetracker.service.OAuth2TokenResponse

@Controller
class OAuth2Controller(
    private val oauth2Service: OAuth2Service
) {
    private val logger = LoggerFactory.getLogger(OAuth2Controller::class.java)

    @Value("\${reai.oauth2.authorization-endpoint}")
    private lateinit var authorizationEndpoint: String

    @Value("\${reai.oauth2.redirect-uri}")
    private lateinit var redirectUri: String

    /**
     * OAuth2 callback endpoint - receives authorization code
     */
    @GetMapping("/authorized")
    fun authorized(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        model: Model
    ): String {
        if (error != null) {
            logger.error("OAuth2 authorization error: $error")
            model.addAttribute("error", error)
            return "oauth-error"
        }

        if (code == null) {
            logger.error("No authorization code received")
            model.addAttribute("error", "No authorization code received")
            return "oauth-error"
        }

        logger.info("Received authorization code")
        model.addAttribute("code", code)
        model.addAttribute("authEndpoint", authorizationEndpoint)
        model.addAttribute("redirectUri", redirectUri)

        return "oauth-callback"
    }

    /**
     * REST API endpoint to exchange authorization code for tokens
     */
    @PostMapping("/api/oauth2/token")
    @ResponseBody
    fun exchangeToken(
        @RequestBody request: TokenExchangeRequest
    ): ResponseEntity<OAuth2TokenResponse> {
        logger.info("Exchanging authorization code for tokens")

        val response = oauth2Service.exchangeCodeForTokens(
            code = request.code,
            clientId = request.clientId,
            clientSecret = request.clientSecret
        )

        return if (response != null) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    /**
     * REST API endpoint to refresh access token
     */
    @PostMapping("/api/oauth2/refresh")
    @ResponseBody
    fun refreshToken(
        @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<OAuth2TokenResponse> {
        logger.info("Refreshing access token")

        val response = oauth2Service.refreshAccessToken(
            refreshToken = request.refreshToken,
            clientId = request.clientId,
            clientSecret = request.clientSecret
        )

        return if (response != null) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}

data class TokenExchangeRequest(
    val code: String,
    val clientId: String,
    val clientSecret: String
)

data class RefreshTokenRequest(
    val refreshToken: String,
    val clientId: String,
    val clientSecret: String
)