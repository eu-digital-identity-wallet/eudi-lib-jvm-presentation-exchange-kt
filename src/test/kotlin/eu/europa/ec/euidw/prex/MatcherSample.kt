package eu.europa.ec.euidw.prex

import eu.europa.ec.euidw.prex.FieldQueryResult.CandidateField
import eu.europa.ec.euidw.prex.FieldQueryResult.CandidateField.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.InputStream

private fun loadPresentationDefinition(f: String): PresentationDefinition =
    PresentationExchange.jsonParser.decodePresentationDefinition(load(f)!!).getOrThrow()

private fun load(f: String): InputStream? =
    PresentationDefinitionTest::class.java.classLoader.getResourceAsStream(f)

private fun printResult(match: Match) {
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
            println("Failed to match presentation definition.")
            match.details.forEach {
                val (inputDescriptorId, notMatchedPerClaim) = it
                println("\t${inputDescriptorId.str()}")
                notMatchedPerClaim.forEach { entry ->
                    val (claimId, evaluation) = entry
                    println("\t\tClaim $claimId ${evaluation.str()}")
                }
            }
        }

        is Match.Matched -> {
            println("Matched presentation definition.")
            match.matches.forEach {
                val (inputDescriptorId, candidatesPerClaim) = it
                println("\t${inputDescriptorId.str()}")
                candidatesPerClaim.forEach { entry ->
                    val (claimId, evaluation) = entry
                    println("\t\tClaim $claimId ${evaluation.str()}")
                }
            }
        }
    }
}

val bankAccount = SimpleClaim(
    uniqueId = "bankAccountClaim",
    format = ClaimFormat.LdpType.LDP,
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
    format = ClaimFormat.LdpType.LDP,
    value = buildJsonObject {
        putJsonObject("credentialSchema") {
            put("id", "hub://did:foo:123/Collections/schema.us.gov/passport.json")
        }
        putJsonObject("credentialSubject") {
            put("birth_date", "1974-02-11")
        }
    },
)

data class SimpleClaim(override val uniqueId: String, override val format: ClaimFormat, private val value: JsonObject) :
    Claim {
        override fun asJsonString(): String = value.toString()
    }

fun main() {
    val presentationDefinition = loadPresentationDefinition("v2.0.0/presentation-definition/basic_example.json")
    val claims = listOf(bankAccount, passport)
    PresentationExchange.matcher.match(presentationDefinition, claims).also { printResult(it) }
}
