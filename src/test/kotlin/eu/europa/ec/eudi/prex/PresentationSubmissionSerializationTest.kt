/*
 * Copyright (c) 2023 European Commission
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

import eu.europa.ec.eudi.prex.TestUtils.deserializeObject
import eu.europa.ec.eudi.prex.TestUtils.loadResource
import eu.europa.ec.eudi.prex.TestUtils.serializeObject
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PresentationSubmissionSerializationTest {

    private val parser = PresentationExchange.jsonParser

    @Test
    fun `should serialize simple example`() {
        testSubmissionSerialization("v2.0.0/presentation-submission/example.json")
    }

    @Test
    fun `should serialize OIDC example`() {
        testSubmissionSerialization("v2.0.0/presentation-submission/appendix_OIDC_example.json")
    }

    @Test
    fun `should serialize VP example`() {
        testSubmissionSerialization("v2.0.0/presentation-submission/appendix_VP_example.json")
    }

    @Test
    fun `should serialize JWT example`() {
        testSubmissionSerialization("v2.0.0/presentation-submission/appendix_JWT_example.json")
    }

    private fun testSubmissionSerialization(path: String) {
        loadResource(path).use {
            val definition = parser.decodePresentationSubmission(it).getOrThrow()
            val serialized = serializeObject(definition)
            val deserialized = deserializeObject<PresentationSubmission>(serialized)
            assertEquals(definition, deserialized)
        }
    }
}
