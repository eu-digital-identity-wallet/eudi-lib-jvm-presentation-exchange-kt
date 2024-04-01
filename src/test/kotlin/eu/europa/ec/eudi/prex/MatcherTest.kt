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

import eu.europa.ec.eudi.prex.FieldQueryResult.CandidateField
import eu.europa.ec.eudi.prex.FieldQueryResult.CandidateField.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertIs

internal class MatcherTest {

    @Test
    fun `basic example`() {
        val presentationDefinition = loadPresentationDefinition("v2.0.0/presentation-definition/basic_example.json")
        val bankAccount = SimpleClaim(
            uniqueId = "bankAccountClaim",
            format = "ldp",
            value = buildJsonObject {
                putJsonObject("vc") {
                    put("issuer", "did:example:123")
                    putJsonObject("credentialSchema") {
                        put("id", "https://bank-standards.example.com/fullaccountroute.json")
                    }
                }
            },
        )
        val passport = SimpleClaim(
            uniqueId = "samplePassport",
            format = "ldp",
            value = buildJsonObject {
                putJsonObject("credentialSchema") {
                    put("id", "hub://did:foo:123/Collections/schema.us.gov/passport.json")
                }
                putJsonObject("credentialSubject") {
                    put("birth_date", "1974-02-11")
                }
            },
        )
        val claims = listOf(bankAccount, passport)
        assertIs<Match.Matched>(PresentationExchange.matcher.match(presentationDefinition, claims))
//            .also { match ->
//                printResult(match) { text -> println(text) }
//            }
    }
}

private fun loadPresentationDefinition(f: String): PresentationDefinition =
    PresentationExchange.jsonParser.decodePresentationDefinition(load(f)!!).getOrThrow()

private fun load(f: String): InputStream? =
    PresentationDefinitionTest::class.java.classLoader.getResourceAsStream(f)

private fun printResult(match: Match, log: (String) -> Unit = {}) {
    fun CandidateField.str(): String = when (this) {
        is Found -> "in path ${path.value} with content $content"
        is OptionalFieldNotFound -> "not present but was optional"
        is PredicateEvaluated -> "in path ${path.value} predicated evaluated to $predicateEvaluation"
    }

    fun InputDescriptorEvaluation.str() = when (this) {
        is InputDescriptorEvaluation.NotMatchingClaim -> "Not matched"
        is InputDescriptorEvaluation.CandidateClaim -> "Matched\n\t\t\t" + matches.entries.mapIndexed { index, entry ->
            val (_, queryResult) = entry

            "FieldConstraint no:$index  was matched  ${queryResult.str()}"
        }.joinToString(separator = "\n\t\t\t")
    }

    fun InputDescriptorId.str() = "Input descriptor: ${this.value}"

    when (match) {
        is Match.NotMatched -> {
            log("Failed to match presentation definition.")
            match.details.forEach {
                val (inputDescriptorId, notMatchedPerClaim) = it
                log("\t${inputDescriptorId.str()}")
                notMatchedPerClaim.forEach { entry ->
                    val (claimId, evaluation) = entry
                    log("\t\tClaim $claimId ${evaluation.str()}")
                }
            }
        }

        is Match.Matched -> {
            log("Matched presentation definition.")
            match.matches.forEach {
                val (inputDescriptorId, candidatesPerClaim) = it
                log("\t${inputDescriptorId.str()}")
                candidatesPerClaim.forEach { entry ->
                    val (claimId, evaluation) = entry
                    log("\t\tClaim $claimId ${evaluation.str()}")
                }
            }
        }
    }
}

private data class SimpleClaim(
    override val uniqueId: String,
    override val format: String,
    private val value: JsonObject,
) : Claim {
    override fun asJsonString(): String = value.toString()
}
