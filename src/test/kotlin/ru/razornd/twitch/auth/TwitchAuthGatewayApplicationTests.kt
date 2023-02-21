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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.RequestMethod.ANY
import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.web.reactive.server.WebTestClient

@AutoConfigureWireMock(port = 0)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "twitch.api.url=http://localhost:\${wiremock.server.port}",
        "twitch.registration.client-id=eWiinhlBzvWiCx",
        "twitch.registration.client-secret=HGHSfIZ6EaPSDA2GWlG6Qip",
        "twitch.registration.token-uri=http://localhost:\${wiremock.server.port}/oauth2/token",
        "wiremock.reset-mappings-after-each-test=true"
    ]
)
class TwitchAuthGatewayApplicationTests {

    @Autowired
    lateinit var testClient: WebTestClient

    private val token = "lyCQHINQVYqgo4SRhXkxs"

    @BeforeEach
    fun setUp() {
        stubFor(
            post("/oauth2/token")
                .willReturn(
                    okJson(
                        """
                            {
                              "access_token": "$token",
                              "expires_in": 4845032,
                              "token_type": "bearer"
                            }
                        """.trimIndent()
                    )
                )
        )
    }

    @Test
    fun `will return response from twitch API`(@Value("\${twitch.registration.client-id}") clientId: String) {
        val body = """{"data": [{}]}"""

        stubFor(
            get(urlPathEqualTo("/helix/users"))
                .withHeader("Authorization", equalTo("Bearer $token"))
                .withHeader("Client-Id", equalTo(clientId))
                .withQueryParam("login", equalTo("testUser"))
                .willReturn(okJson(body))
        )

        testClient.get()
            .uri("/helix/users?login=testUser")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(body)

        verify(
            newRequestPattern(GET, urlPathEqualTo("/helix/users"))
                .withQueryParam("login", equalTo("testUser"))
        )
    }

    @Test
    fun `will return response from twitch API from sub path`(@Value("\${twitch.registration.client-id}") clientId: String) {
        val body = """{"data": [{"name": "Half-Life 2"}]}"""

        stubFor(
            get(urlPathEqualTo("/helix/games/top/first"))
                .withHeader("Authorization", equalTo("Bearer $token"))
                .withHeader("Client-Id", equalTo(clientId))
                .willReturn(okJson(body))
        )

        testClient.get()
            .uri("/helix/games/top/first")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(body)

        verify(newRequestPattern(GET, urlPathEqualTo("/helix/games/top/first")))
    }

    @Test
    fun `will return 404 error if request not to twitch`() {
        testClient.get().uri("/not-twitch")
            .exchange()
            .expectStatus().isNotFound

        verify(0, newRequestPattern(ANY, anyUrl()))
    }
}
