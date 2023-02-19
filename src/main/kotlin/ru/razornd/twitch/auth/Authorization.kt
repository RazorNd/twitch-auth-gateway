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

import org.springframework.stereotype.Component
import java.time.Instant


data class ClientRegistration(val clientId: String, val clientSecret: String)

data class OAuth2AccessToken(val tokenValue: String, val expiredAt: Instant)

data class OAuth2AuthorizedClient(val clientRegistration: ClientRegistration, val accessToken: OAuth2AccessToken)

@Component
class OAuth2AuthorizedClientManager {

    suspend fun authorize(): OAuth2AuthorizedClient {
        TODO("Not implemented")
    }
}
