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

import org.junit.jupiter.api.fail
import java.io.InputStream
import kotlin.test.Test

class PresentationDefinitionTest {

    @Test
    fun `format test`() {
        testParseDefinition("v2.0.0/presentation-definition/format_example.json")
    }

    @Test
    fun `basic example`() {
        testParseDefinition("v2.0.0/presentation-definition/basic_example.json")
    }

    @Test
    fun `single group example`() {
        testParseDefinition("v2.0.0/presentation-definition/single_group_example.json").also {
            it.submissionRequirements?.forEach { x -> println(x) }
        }
    }

    @Test
    fun `multi group example`() {
        testParseDefinition("v2.0.0/presentation-definition/multi_group_example.json").also {
            it.submissionRequirements?.forEach { x -> println(x) }
        }
    }

    @Test
    fun `mDL example`() {
        testParseDefinition("v2.0.0/presentation-definition/mDL-example.json").also {
            it.submissionRequirements?.forEach { x -> println(x) }
        }
    }

    @Test
    fun `fi example`() {
        testParseDefinition("v2.0.0/presentation-definition/fi.json").also {
            it.submissionRequirements?.forEach { x -> println(x) }
        }
    }

    private fun testParseDefinition(f: String): PresentationDefinition =
        PresentationExchange.jsonParser.decodePresentationDefinition(load(f)!!)
            .also { println(it) }
            .fold(onSuccess = { it }, onFailure = { fail(it) })

    private fun load(f: String): InputStream? =
        PresentationDefinitionTest::class.java.classLoader.getResourceAsStream(f)
}
