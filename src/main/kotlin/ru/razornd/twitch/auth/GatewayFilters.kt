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

import kotlinx.coroutines.reactor.mono
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.http.server.reactive.ServerHttpRequest.Builder as RequestBuilder
import org.springframework.web.server.ServerWebExchange.Builder as ExchangeBuilder

private const val CLIENT_ID = "Client-Id"

@Component
class TwitchAuthorizationGatewayFilterFactory(
    private val authorizedClientManager: OAuth2AuthorizedClientManager
) : AbstractGatewayFilterFactory<Any>() {

    override fun apply(config: Any) = GatewayFilter { exchange, chain ->
        mono { addAuthorization(exchange) }.flatMap(chain::filter)
    }

    private suspend fun addAuthorization(exchange: ServerWebExchange): ServerWebExchange {
        val authorizedClient = authorizedClientManager.authorize()
        return exchange.mutate().withTwitchAuth(authorizedClient).build()
    }

    private fun ExchangeBuilder.withTwitchAuth(authorizedClient: OAuth2AuthorizedClient) = request {
        it.addTwitchAuth(authorizedClient)
    }

    private fun RequestBuilder.addTwitchAuth(authorizedClient: OAuth2AuthorizedClient) = headers {
        it.setBearerAuth(authorizedClient.accessToken.tokenValue)
        it[CLIENT_ID] = authorizedClient.clientRegistration.clientId
    }

}
