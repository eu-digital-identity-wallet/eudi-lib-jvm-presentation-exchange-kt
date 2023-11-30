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
import eu.europa.ec.eudi.prex.FieldQueryResult.CandidateField.Found
import eu.europa.ec.eudi.prex.InputDescriptorEvaluation.CandidateClaim
import eu.europa.ec.eudi.prex.InputDescriptorEvaluation.NotMatchedFieldConstraints

/**
 * The outcome of applying a [FieldConstraint] to a claim.
 */
sealed interface FieldQueryResult {

    /**
     * Indicates that a [FieldConstraint] is not satisfied by a claim
     */
    data object RequiredFieldNotFound : FieldQueryResult {
        private fun readResolve(): Any = RequiredFieldNotFound
    }

    /**
     * Indicates that a [FieldConstraint] is satisfied by a claim.
     * There are two case:
     * Either [FieldConstraint] was [Found] by the claim or it is not matched but the constraint
     * is marked as [optional][FieldConstraint.optional]
     * There is also a third option when using the predicate feature of the specification
     * where the  Verifier asks from the wallet to evaluate a predicate
     */
    sealed interface CandidateField : FieldQueryResult {

        /**
         * Indicates that a [FieldConstraint] was satisfied by a claim because (the claim) it
         * contains at [path] an appropriate [content]
         */
        data class Found(val path: JsonPath, val content: String) : CandidateField
        data class PredicateEvaluated(val path: JsonPath, val predicateEvaluation: Boolean) : CandidateField

        /**
         * Indicates that the claim doesn't contain a field as described in the [FieldConstraint]
         * yet this is not a reason to reject the claim since the field is/was optional
         */
        data object OptionalFieldNotFound : CandidateField {
            private fun readResolve(): Any = OptionalFieldNotFound
        }
    }
}

/**
 * The outcome of evaluating an [InputDescriptor] against a [Claim]
 */
sealed interface InputDescriptorEvaluation {

    /**
     * Indicates that claim is candidate, that is matches, the given [InputDescriptor]
     */
    data class CandidateClaim(val matches: Map<FieldConstraint, CandidateField>) : InputDescriptorEvaluation {
        init {
            require(matches.isNotEmpty())
        }
    }

    /**
     * Indicates that claim doesn't satisfy the constraints of the [InputDescriptor]
     */
    sealed interface NotMatchingClaim : InputDescriptorEvaluation
    data object NotMatchedFieldConstraints : NotMatchingClaim {
        private fun readResolve(): Any = NotMatchedFieldConstraints
    }
    data object UnsupportedFormat : NotMatchingClaim {
        private fun readResolve(): Any = UnsupportedFormat
    }
}

typealias ClaimId = String

interface Claim {
    val uniqueId: ClaimId
    val format: ClaimFormat
    fun asJsonString(): String
}

typealias InputDescriptorEvalPerClaim<A> = Map<InputDescriptorId, Map<ClaimId, A>>

sealed interface Match {
    data class Matched(val matches: InputDescriptorEvalPerClaim<CandidateClaim>) : Match
    data class NotMatched(val details: InputDescriptorEvalPerClaim<NotMatchedFieldConstraints>) : Match
}

/**
 * Checks whether a [presentation definition][PresentationDefinition] can be satisfied by
 * the given list of [Claim]
 *
 * @see <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#input-evaluation">Input evaluation</a>
 */
fun interface PresentationMatcher {
    fun match(pd: PresentationDefinition, claims: List<Claim>): Match
}
