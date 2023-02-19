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
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC

class ClientCredentialsOAuth2AuthorizedClientProviderTest {

    private val client: OAuth2AccessTokenClient = mockk()

    private val clientRegistration = ClientRegistration("test-client-id", "NOOP")

    private val provider = ClientCredentialsOAuth2AuthorizedClientProvider(client).apply {
        clock = Clock.fixed(Instant.parse("2023-02-19T12:00:00Z"), UTC)
    }

    @Test
    fun `will return authorized client with token received from access token client`() {
        val accessToken = OAuth2AccessToken("EC70hFsiZqJ2M4lB40", Instant.parse("2023-02-19T15:00:00Z"))

        coEvery { client.receiveToken(any()) } returns accessToken

        val actual = runBlocking { provider.authorize(context()) }

        assertThat(actual).usingRecursiveComparison().isEqualTo(authorizedClient(accessToken))
    }

    @Test
    fun `must pass registration from context as argument to access token client`() {
        val accessToken = OAuth2AccessToken("8HsWg2IodF8qS", Instant.parse("2023-02-19T15:00:00Z"))
        val registrationSlot = slot<ClientRegistration>()

        coEvery { client.receiveToken(capture(registrationSlot)) } returns accessToken

        runBlocking { provider.authorize(context()) }

        assertThat(registrationSlot.captured).isSameAs(clientRegistration)
    }

    @Test
    fun `will return authorized client from context if it doesn't expired`() {
        val accessToken = OAuth2AccessToken("7ZLcUzLTALMW", Instant.parse("2023-02-19T15:00:00Z"))
        val expected = authorizedClient(accessToken)

        val actual = runBlocking { provider.authorize(context(expected)) }

        assertThat(actual).isSameAs(expected)
    }

    @Test
    fun `will receive new token if token from context expired`() {
        val expiredToken = OAuth2AccessToken("oq1nyTqrmEK8", Instant.parse("2023-02-19T11:50:00Z"))
        val newToken = OAuth2AccessToken("SsVT1n3aasJTyF", Instant.parse("2023-02-19T15:00:00Z"))

        coEvery { client.receiveToken(any()) } returns newToken

        val actual = runBlocking { provider.authorize(context(authorizedClient(expiredToken))) }

        assertThat(actual).usingRecursiveComparison().isEqualTo(authorizedClient(newToken))
    }

    @Test
    fun `will receive new token if token from context in skew duration`() {
        val expiredToken = OAuth2AccessToken("Z8b28O25UqSsh0rNTHg67tjv", Instant.parse("2023-02-19T12:05:00Z"))
        val newToken = OAuth2AccessToken("QmTpXXnAB1lLiBbDba2oFU", Instant.parse("2023-02-19T15:00:00Z"))
        provider.clockSkew = Duration.ofMinutes(10)

        coEvery { client.receiveToken(any()) } returns newToken

        val actual = runBlocking { provider.authorize(context(authorizedClient(expiredToken))) }

        assertThat(actual).usingRecursiveComparison().isEqualTo(authorizedClient(newToken))
    }

    private fun authorizedClient(token: OAuth2AccessToken) = OAuth2AuthorizedClient(clientRegistration, token)

    private fun context(client: OAuth2AuthorizedClient? = null) = OAuth2AuthorizationContext(clientRegistration, client)
}
