package eu.europa.ec.euidw.prex.internal

import eu.europa.ec.euidw.prex.*
import eu.europa.ec.euidw.prex.internal.DefaultPresentationMatcher.Evaluator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Holds the [InputDescriptorEvaluation] per [InputDescriptorId] per [Claim].
 * That is, for each available [claim][Claim], there is an evaluation against all
 * [input descriptors][PresentationDefinition.inputDescriptors] of a [PresentationDefinition]
 */
private typealias ClaimsEvaluation = Map<ClaimId, Map<InputDescriptorId, InputDescriptorEvaluation>>

internal class DefaultPresentationMatcher(
    private val inputDescriptorEvaluator: InputDescriptorEvaluator
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
            candidateClaims: InputDescriptorEvalPerClaim<InputDescriptorEvaluation.CandidateClaim>,
            notMatchingClaims: InputDescriptorEvalPerClaim<InputDescriptorEvaluation.NotMatchedFieldConstraints>
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
        claimsEvaluation: ClaimsEvaluation
    ): Pair<InputDescriptorEvalPerClaim<InputDescriptorEvaluation.CandidateClaim>, InputDescriptorEvalPerClaim<InputDescriptorEvaluation.NotMatchedFieldConstraints>> {
        val candidateClaimsPerDescriptor =
            mutableMapOf<InputDescriptorId, Map<ClaimId, InputDescriptorEvaluation.CandidateClaim>>()
        val notMatchingClaimsPerDescriptor =
            mutableMapOf<InputDescriptorId, Map<ClaimId, InputDescriptorEvaluation.NotMatchedFieldConstraints>>()

        fun updateCandidateClaims(i: InputDescriptor) {
            val candidateClaims = claimsEvaluation.entriesFor<InputDescriptorEvaluation.CandidateClaim>(i.id)
            if (candidateClaims.isNotEmpty()) {
                candidateClaimsPerDescriptor[i.id] = candidateClaims
            }
        }

        fun updateNotMatchingClaims(i: InputDescriptor) {
            val notMatchingClaims =
                claimsEvaluation.entriesFor<InputDescriptorEvaluation.NotMatchedFieldConstraints>(i.id)
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
    private val allInputDescriptorsRequired = Evaluator { pd, candidateClaims, notMatchingClaims ->
        if (candidateClaims.size == pd.inputDescriptors.size) Match.Matched(candidateClaims)
        else Match.NotMatched(notMatchingClaims)
    }


    /**
     * An alternative [Evaluator] that takes into account [PresentationDefinition.submissionRequirements].
     * It can  be used only if presentation definition contains such requirements
     */
    private val matchSubmissionRequirements = Evaluator { pd, candidateClaims, notMatchingClaims ->
        check(null != pd.submissionRequirements)


        fun inputDescriptorsOf(sr: SubmissionRequirement): List<InputDescriptor> {
            val allGroups = sr.allGroups()
            return pd.inputDescriptors
                .filter { inputDescriptor -> inputDescriptor.groups?.all { it in allGroups } ?: false }
        }

        fun match(sr: SubmissionRequirement): Boolean {
            val inputDescriptors = inputDescriptorsOf(sr)
            return when (val from = sr.from) {
                is From.FromGroup -> when (sr.rule) {
                    is Rule.All -> TODO()
                    is Rule.Pick -> TODO()
                }

                is From.FromNested -> from.nested.all { match(it) }
            }
        }

        TODO()
    }

    private inline fun <reified E> ClaimsEvaluation.entriesFor(inputDescriptorId: InputDescriptorId): Map<ClaimId, E> =
        mapValues { it.value[inputDescriptorId]!! }
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