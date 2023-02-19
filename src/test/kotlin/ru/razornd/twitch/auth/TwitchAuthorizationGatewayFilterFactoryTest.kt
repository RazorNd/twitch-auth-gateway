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

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import java.time.Instant

class TwitchAuthorizationGatewayFilterFactoryTest {

    private val authorizedClientManager: OAuth2AuthorizedClientManager = mockk()

    private val gatewayFilterFactory = TwitchAuthorizationGatewayFilterFactory(authorizedClientManager)

    private val request = MockServerHttpRequest.get("/helix/users?login=testUser")

    private val exchange = MockServerWebExchange.from(request)

    private val chain: GatewayFilterChain = mockk {
        @Suppress("ReactiveStreamsUnusedPublisher")
        every { filter(any()) } returns Mono.empty()
    }

    @Test
    fun `will add authorization header to exchange`() {
        coEvery { authorizedClientManager.authorize() } returns OAuth2AuthorizedClient(
            ClientRegistration("test-client-id", "NOOP"),
            OAuth2AccessToken("test-access-token", Instant.parse("2000-01-28T05:24:44Z"))
        )

        gatewayFilterFactory.apply(Any()).filter(exchange, chain).block()

        assertThat(exchange.request.headers)
            .containsEntry("Authorization", listOf("Bearer test-access-token"))
            .containsEntry("Client-Id", listOf("test-client-id"))
    }
}
