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
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class OAuth2AuthorizedClientManagerTest {

    private val clientRegistration = ClientRegistration("test-client-id", "NOOP")

    private val token = OAuth2AccessToken("token-value", Instant.parse("2023-02-19T14:33:00Z"))

    private val repository: OAuth2AuthorizedClientRepository = mockk(relaxUnitFun = true) {
        coEvery { loadAuthorizedClient() } returns null
    }

    private val provider: ClientCredentialsOAuth2AuthorizedClientProvider = mockk {
        coEvery { authorize(any()) } answers {
            firstArg<OAuth2AuthorizationContext>().authorizedClient ?: throw IllegalStateException()
        }
    }

    private val clientManager = OAuth2AuthorizedClientManager(
        clientRegistration,
        repository,
        provider
    )

    @Test
    fun `will load authorization from repository`() {
        val expected = OAuth2AuthorizedClient(clientRegistration, token)

        coEvery { repository.loadAuthorizedClient() } returns expected

        val actual = runBlocking { clientManager.authorize() }

        assertThat(actual).isSameAs(expected)
    }

    @Test
    fun `will return authorized client from provider`() {
        val expected = OAuth2AuthorizedClient(clientRegistration, token)
        val contextSlot = slot<OAuth2AuthorizationContext>()

        coEvery { provider.authorize(capture(contextSlot)) } returns expected

        val actual = runBlocking { clientManager.authorize() }

        assertThat(actual).describedAs("Authorized Client").isSameAs(expected)
        assertThat(contextSlot.captured).describedAs("Authorization Context")
            .isEqualTo(OAuth2AuthorizationContext(clientRegistration, null))
    }

    @Test
    fun `will save new authorized client`() {
        val expected = OAuth2AuthorizedClient(clientRegistration, token)
        val clientSlot = slot<OAuth2AuthorizedClient>()

        coEvery { provider.authorize(any()) } returns expected

        runBlocking { clientManager.authorize() }
        coVerify { repository.saveAuthorizedClient(capture(clientSlot)) }

        assertThat(clientSlot.captured).describedAs("Saved Client").isSameAs(expected)
    }

    @Test
    fun `will not save if client not changed`() {
        val expected = OAuth2AuthorizedClient(clientRegistration, token)

        coEvery { repository.loadAuthorizedClient() } returns expected

        runBlocking { clientManager.authorize() }

        coVerify(exactly = 0) { repository.saveAuthorizedClient(any()) }
    }
}
