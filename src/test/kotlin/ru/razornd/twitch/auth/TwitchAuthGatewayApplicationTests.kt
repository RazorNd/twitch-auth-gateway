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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.web.reactive.server.WebTestClient

@AutoConfigureWireMock(port = 0)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["twitch.api.url=http://localhost:\${wiremock.server.port}"])
class TwitchAuthGatewayApplicationTests {

    @Autowired
    lateinit var testClient: WebTestClient

    @Test
    fun `will return response from twitch API`() {
        val body = """{"data": [{}]}"""
        stubFor(
            get(urlPathEqualTo("/helix/users"))
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
    fun `will return 404 error if request not to twitch`() {
        testClient.get().uri("/not-twitch")
            .exchange()
            .expectStatus().isNotFound

        verify(0, newRequestPattern(ANY, anyUrl()))
    }
}
