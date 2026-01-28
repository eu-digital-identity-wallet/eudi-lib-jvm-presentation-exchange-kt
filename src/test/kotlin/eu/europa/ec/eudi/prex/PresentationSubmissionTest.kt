/*
 * Copyright (c) 2023-2026 European Commission
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
 */
package eu.europa.ec.eudi.prex

import java.io.InputStream
import kotlin.test.Test
import kotlin.test.fail

class PresentationSubmissionTest {

    private val parser = PresentationExchange.jsonParser

    @Test
    fun simpleCases() {
        listOf(
            "v2.0.0/presentation-submission/example.json",
            "v2.0.0/presentation-submission/appendix_OIDC_example.json",
            "v2.0.0/presentation-submission/appendix_VP_example.json",
            "v2.0.0/presentation-submission/appendix_JWT_example.json",
        ).forEach { testParseDefinition(it) }
    }

    private fun testParseDefinition(f: String): PresentationSubmission =
        parser.decodePresentationSubmission(load(f)!!)
            .also { println(it) }
            .fold(onSuccess = { it }, onFailure = { fail(it.message, it) })

    private fun load(f: String): InputStream? =
        PresentationDefinitionTest::class.java.classLoader.getResourceAsStream(f)
}
