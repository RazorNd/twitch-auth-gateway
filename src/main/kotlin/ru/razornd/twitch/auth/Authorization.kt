/*
 * Copyright 2023 Daniil Razorenov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ru.razornd.twitch.auth

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.razornd.twitch.auth.OAuth2AuthorizedClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import ru.razornd.twitch.auth.OAuth2AuthorizationContext as Context
import ru.razornd.twitch.auth.OAuth2AuthorizedClient as AuthorizedClient


@ConfigurationProperties("twitch.registration")
data class ClientRegistration(
    val clientId: String,
    val clientSecret: String,
    @DefaultValue("https://id.twitch.tv/oauth2/token")
    val tokenUri: String = "https://id.twitch.tv/oauth2/token"
)

data class OAuth2AccessToken(val tokenValue: String, val expiredAt: Instant)

data class OAuth2AuthorizedClient(val clientRegistration: ClientRegistration, val accessToken: OAuth2AccessToken)

data class OAuth2AuthorizationContext(
    val clientRegistration: ClientRegistration,
    val authorizedClient: AuthorizedClient?
)

private val log = logger<OAuth2AuthorizedClientManager>()

interface OAuth2AuthorizedClientRepository {
    suspend fun loadAuthorizedClient(): AuthorizedClient?

    suspend fun saveAuthorizedClient(authorizedClient: AuthorizedClient)

}

@Component
class InMemoryOAuth2AuthorizedClientRepository : OAuth2AuthorizedClientRepository {

    @Volatile
    private var authorizedClient: AuthorizedClient? = null

    override suspend fun loadAuthorizedClient() = authorizedClient

    override suspend fun saveAuthorizedClient(authorizedClient: AuthorizedClient) {
        this.authorizedClient = authorizedClient
    }

}

private const val CLIENT_CREDENTIALS = "client_credentials"

@Component
class OAuth2AccessTokenClient(webClientBuilder: WebClient.Builder) {

    private val webClient = webClientBuilder.build()

    var clock: Clock = Clock.systemUTC()

    @RegisterReflectionForBinding(AccessTokenResponse::class)
    suspend fun receiveToken(clientRegistration: ClientRegistration): OAuth2AccessToken {
        return webClient.post()
            .uri(clientRegistration.tokenUri)
            .bodyValue(clientRegistration.grantRequest())
            .retrieve()
            .awaitBody<AccessTokenResponse>()
            .convert()
    }

    private fun ClientRegistration.grantRequest() = LinkedMultiValueMap(
        mapOf(
            "client_id" to listOf(clientId),
            "client_secret" to listOf(clientSecret),
            "grant_type" to listOf(CLIENT_CREDENTIALS)
        )
    )

    private data class AccessTokenResponse(
        @JsonProperty("access_token")
        val tokenValue: String,
        @JsonProperty("expires_in")
        val expireIn: Duration
    )

    private fun AccessTokenResponse.convert() = OAuth2AccessToken(tokenValue, Instant.now(clock).plus(expireIn))
}

@Component
class ClientCredentialsOAuth2AuthorizedClientProvider(private val client: OAuth2AccessTokenClient) {

    var clockSkew: Duration = Duration.ofSeconds(60)

    var clock: Clock = Clock.systemUTC()

    suspend fun authorize(context: Context): AuthorizedClient = context.doAuthorize()

    private suspend fun Context.doAuthorize(): OAuth2AuthorizedClient {
        if (authorizedClient?.isExpired == true) {
            log.info("Access token on the Authorized client has expired. A new access token will be received.")
            return receiveNewToken(clientRegistration)
        }

        return authorizedClient ?: receiveNewToken(clientRegistration)
    }

    private val AuthorizedClient.isExpired get() = Instant.now(clock).isAfter(accessToken.expiredAt.minus(clockSkew))
    private suspend fun receiveNewToken(clientRegistration: ClientRegistration) =
        AuthorizedClient(clientRegistration, client.receiveToken(clientRegistration))
            .also { log.info("Received new access token: {}", it.accessToken) }
}

@Component
@EnableConfigurationProperties(ClientRegistration::class)
class OAuth2AuthorizedClientManager(
    private val clientRegistration: ClientRegistration,
    private val repository: OAuth2AuthorizedClientRepository,
    private val provider: ClientCredentialsOAuth2AuthorizedClientProvider
) {

    suspend fun authorize(): AuthorizedClient {
        val client = repository.loadAuthorizedClient()

        return provider.authorize(context(client)).also { it.takeIf { it !== client }?.updateAuthorizedClient() }
    }

    private fun context(client: AuthorizedClient?) = Context(clientRegistration, client)

    private suspend fun AuthorizedClient.updateAuthorizedClient() = repository.saveAuthorizedClient(this)
        .also { log.info("Save Authorized client: {}", this) }
}
