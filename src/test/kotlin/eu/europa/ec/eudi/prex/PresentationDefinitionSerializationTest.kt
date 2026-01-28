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

import eu.europa.ec.eudi.prex.TestUtils.deserializeObject
import eu.europa.ec.eudi.prex.TestUtils.loadResource
import eu.europa.ec.eudi.prex.TestUtils.serializeObject
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PresentationDefinitionSerializationTest {

    @Test
    fun `should serialize format test`() {
        testPresentationSerialization("v2.0.0/presentation-definition/format_example.json")
    }

    @Test
    fun `should serialize basic example`() {
        testPresentationSerialization("v2.0.0/presentation-definition/basic_example.json")
    }

    @Test
    fun `should serialize SD-JWT VC`() {
        testPresentationSerialization("v2.0.0/presentation-definition/pd_sd_jwt_vc.json")
    }

    @Test
    fun `should serialize single group example`() {
        testPresentationSerialization("v2.0.0/presentation-definition/single_group_example.json")
    }

    @Test
    fun `should serialize multi group example`() {
        testPresentationSerialization("v2.0.0/presentation-definition/multi_group_example.json")
    }

    @Test
    fun `should serialize mDL example`() {
        testPresentationSerialization("v2.0.0/presentation-definition/mDL-example.json")
    }

    private fun testPresentationSerialization(path: String) {
        loadResource(path).use {
            val definition = PresentationExchange.jsonParser.decodePresentationDefinition(it).getOrThrow()
            val serialized = serializeObject(definition)
            val deserialized = deserializeObject<PresentationDefinition>(serialized)
            assertEquals(definition, deserialized)
        }
    }
}
