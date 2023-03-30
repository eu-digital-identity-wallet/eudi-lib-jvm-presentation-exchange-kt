package eu.europa.ec.euidw.prex

import eu.europa.ec.euidw.prex.FieldQueryResult.Candidate
import eu.europa.ec.euidw.prex.FieldQueryResult.Candidate.Found
import eu.europa.ec.euidw.prex.InputDescriptorEvaluation.CandidateFound
import eu.europa.ec.euidw.prex.InputDescriptorEvaluation.NotMatchedFieldConstraints

/**
 * The outcome of applying a [FieldConstraint] to a claim
 */
sealed interface FieldQueryResult {

    /**
     * Indicates that a [FieldConstraint] is not satisfied by a claim
     */
    object RequiredFieldNotFound : FieldQueryResult

    /**
     * Indicates that a [FieldConstraint] is satisfied by a claim.
     * There are two case:
     * Either [FieldConstraint] was [Found] by the claim or it is not matched but the constraint
     * is marked as [optional][FieldConstraint.optional]
     */
    sealed interface Candidate : FieldQueryResult {

        /**
         * Indicates that a [FieldConstraint] was satisfied by a claim because (the claim) it
         * contains at [path] an appropriate [content]
         */
        data class Found(val path: JsonPath, val content: JsonString) : Candidate
        data class PredicateEvaluated(val path: JsonPath, val predicateEvaluation: Boolean) : Candidate
        object OptionalFieldNotFound : Candidate
    }
}

sealed interface InputDescriptorEvaluation {
    data class CandidateFound(val matches: Map<FieldConstraint, Candidate>) : InputDescriptorEvaluation {
        init {
            require(matches.isNotEmpty())
        }
    }

    data class NotMatchedFieldConstraints(val fieldConstraints: Set<FieldConstraint>) : InputDescriptorEvaluation {
        init {
            require(fieldConstraints.isNotEmpty())
        }
    }
}


typealias ClaimId = String

interface Claim {
    val uniqueId: ClaimId
    fun asJsonString(): JsonString
}


typealias InputDescriptorEvalPerClaim<A> = Map<InputDescriptorId, Map<ClaimId, A>>

sealed interface Match {
    data class Matched(val matches: InputDescriptorEvalPerClaim<CandidateFound>) : Match
    data class NotMatched(val details: InputDescriptorEvalPerClaim<NotMatchedFieldConstraints>) : Match
}

/**
 * Checks whether a [presentation definition][PresentationDefinition] can be satisfied by
 * the given list of [Claim]
 */
interface PresentationMatcher {
    fun match(pd: PresentationDefinition, claims: List<Claim>): Match
}