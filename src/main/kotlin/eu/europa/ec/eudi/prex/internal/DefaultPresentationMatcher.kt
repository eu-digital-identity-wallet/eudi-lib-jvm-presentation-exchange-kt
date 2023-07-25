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
package eu.europa.ec.eudi.prex.internal

import eu.europa.ec.eudi.prex.*
import eu.europa.ec.eudi.prex.InputDescriptorEvaluation.CandidateClaim
import eu.europa.ec.eudi.prex.InputDescriptorEvaluation.NotMatchedFieldConstraints
import eu.europa.ec.eudi.prex.internal.DefaultPresentationMatcher.Evaluator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Holds the [InputDescriptorEvaluation] per [InputDescriptorId] per [Claim].
 * That is, for each available [claim][Claim], there is an evaluation against all
 * [input descriptors][PresentationDefinition.inputDescriptors] of a [PresentationDefinition]
 */
private typealias ClaimsEvaluation = Map<ClaimId, Map<InputDescriptorId, InputDescriptorEvaluation>>

internal class DefaultPresentationMatcher(
    private val inputDescriptorEvaluator: InputDescriptorEvaluator,
) : PresentationMatcher {

    override fun match(pd: PresentationDefinition, claims: List<Claim>): Match {
        // Evaluate the input descriptor match for all descriptors and claims
        val claimsEvaluation = claims.associate { claim ->
            claim.uniqueId to inputDescriptorEvaluator.matchInputDescriptors(pd.format, pd.inputDescriptors, claim)
        }
        // split evaluations to candidate and not matching
        val (candidateClaims, notMatchingClaims) = splitPerDescriptor(pd, claimsEvaluation)
        // choose evaluator based on the presentation definition content
        val evaluator = evaluatorOf(pd)
        // evaluate the outcome
        return evaluator.evaluate(pd, candidateClaims, notMatchingClaims)
    }

    /**
     * An interface that defines a way to decide on the overall presentation definition match
     * given a some candidate and not matching input descriptor evaluations
     */
    private fun interface Evaluator {
        fun evaluate(
            pd: PresentationDefinition,
            candidateClaims: InputDescriptorEvalPerClaim<CandidateClaim>,
            notMatchingClaims: InputDescriptorEvalPerClaim<NotMatchedFieldConstraints>,
        ): Match
    }

    /**
     * Based on the presence of [submission requirements][PresentationDefinition.submissionRequirements]
     * selects an appropriate [Evaluator].
     * That is , in case there aren't submission requirements [allInputDescriptorsRequired]
     * otherwise [matchSubmissionRequirements]
     */
    private fun evaluatorOf(pd: PresentationDefinition): Evaluator =
        pd.submissionRequirements?.let { matchSubmissionRequirements } ?: allInputDescriptorsRequired

    private fun splitPerDescriptor(
        pd: PresentationDefinition,
        claimsEvaluation: ClaimsEvaluation,
    ): Pair<InputDescriptorEvalPerClaim<CandidateClaim>, InputDescriptorEvalPerClaim<NotMatchedFieldConstraints>> {
        val candidateClaimsPerDescriptor =
            mutableMapOf<InputDescriptorId, Map<ClaimId, CandidateClaim>>()
        val notMatchingClaimsPerDescriptor =
            mutableMapOf<InputDescriptorId, Map<ClaimId, NotMatchedFieldConstraints>>()

        fun updateCandidateClaims(i: InputDescriptor) {
            val candidateClaims = claimsEvaluation.entriesFor<CandidateClaim>(i.id)
            if (candidateClaims.isNotEmpty()) {
                candidateClaimsPerDescriptor[i.id] = candidateClaims
            }
        }

        fun updateNotMatchingClaims(i: InputDescriptor) {
            val notMatchingClaims =
                claimsEvaluation.entriesFor<NotMatchedFieldConstraints>(i.id)
            if (notMatchingClaims.isNotEmpty()) {
                notMatchingClaimsPerDescriptor[i.id] = notMatchingClaims
            }
        }

        for (inputDescriptor in pd.inputDescriptors) {
            updateCandidateClaims(inputDescriptor)
            updateNotMatchingClaims(inputDescriptor)
        }
        return candidateClaimsPerDescriptor.toMap() to notMatchingClaimsPerDescriptor.toMap()
    }

    /**
     * Makes sure that for every [input descriptor][PresentationDefinition.inputDescriptors]
     * there is at least a [claim][Claim] that satisfies it.
     * In this case, return a [Match.Matched], otherwise a [Match.NotMatched]
     */
    @Suppress("ktlint")
    private val allInputDescriptorsRequired = Evaluator { pd, candidateClaims, notMatchingClaims ->
        if (candidateClaims.size == pd.inputDescriptors.size) Match.Matched(candidateClaims)
        else Match.NotMatched(notMatchingClaims)
    }

    /**
     * An alternative [Evaluator] that takes into account [PresentationDefinition.submissionRequirements].
     * It can  be used only if presentation definition contains such requirements
     */
    private val matchSubmissionRequirements = Evaluator { pd, _, _ ->
        checkNotNull(pd.submissionRequirements)
        error("Not yet implemented")
    }

    private inline fun <reified E> ClaimsEvaluation.entriesFor(inputDescriptorId: InputDescriptorId): Map<ClaimId, E> =
        mapValues { it.value[inputDescriptorId] }
            .filterValues { it is E }
            .mapValues { it.value as E }

    companion object {

        fun build(json: Json): PresentationMatcher {
            val filterOps = FilterOps { json.encodeToString(it) }
            val fieldMatcher = FieldConstraintMatcher(filterOps)
            val inputDescriptorEvaluator = InputDescriptorEvaluator(fieldMatcher)
            return DefaultPresentationMatcher(inputDescriptorEvaluator)
        }
    }
}
