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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC


@AutoConfigureWireMock(port = 0)
@RestClientTest(OAuth2AccessTokenClient::class)
class OAuth2AccessTokenClientTest {

    @Autowired
    lateinit var client: OAuth2AccessTokenClient

    private val now = Instant.parse("2023-02-19T12:00:00Z")

    @Test
    fun receiveToken(@Value("\${wiremock.server.port}") port: Int) {
        val clientId = "7936495"
        val secret = "oxDidjp6tSFjms74lv8iquw"
        client.clock = Clock.fixed(now, UTC)

        stubFor(
            post("/oauth2/token")
                .withHeader(HttpHeaders.CONTENT_TYPE, containing("application/x-www-form-urlencoded"))
                .withRequestBody(equalTo("client_id=$clientId&client_secret=$secret&grant_type=client_credentials"))
                .willReturn(
                    okJson(
                        """
                            {
                              "access_token": "u33afg6v1yszd78a4jippu80j8ixzh",
                              "expires_in": 4845032,
                              "token_type": "bearer"
                            }
                        """.trimIndent()
                    )
                )
        )

        val accessToken = runBlocking {
            client.receiveToken(
                ClientRegistration(
                    clientId,
                    secret,
                    "http://localhost:$port/oauth2/token"
                )
            )
        }

        assertThat(accessToken)
            .usingRecursiveComparison()
            .isEqualTo(OAuth2AccessToken("u33afg6v1yszd78a4jippu80j8ixzh", now.plus(Duration.ofSeconds(4845032))))
    }
}
