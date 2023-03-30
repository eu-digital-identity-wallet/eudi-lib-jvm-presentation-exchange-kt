package eu.europa.ec.euidw.prex.internal

import eu.europa.ec.euidw.prex.*
import eu.europa.ec.euidw.prex.FieldQueryResult.Candidate
import eu.europa.ec.euidw.prex.FieldQueryResult.RequiredFieldNotFound
import eu.europa.ec.euidw.prex.InputDescriptorEvaluation.CandidateFound
import eu.europa.ec.euidw.prex.InputDescriptorEvaluation.NotMatchedFieldConstraints

/**
 * Evaluates whether an [InputDescriptor] matches a [Claim]
 */
internal class InputDescriptorEvaluator(private val fieldConstraintMatcher: FieldConstraintMatcher) {

    /**
     * Evaluates whether a given [claim] satisfies a set of [inputDescriptors]
     */
    internal fun matchInputDescriptors(
        inputDescriptors: Iterable<InputDescriptor>,
        claim: Claim
    ): Map<InputDescriptorId, InputDescriptorEvaluation> {
        val claimJsonString = claim.asJsonString()
        return inputDescriptors.associate { it.id to evaluate(it, claimJsonString) }
    }

    /**
     * Evaluates whether a given [claimJsonString] satisfies a  [inputDescriptor]
     */
    private fun evaluate(
        inputDescriptor: InputDescriptor,
        claimJsonString: JsonString
    ): InputDescriptorEvaluation =
        checkFieldConstraints(inputDescriptor.constraints.fields(), claimJsonString)


    private fun checkFieldConstraints(
        fieldConstraints: List<FieldConstraint>,
        claimJsonString: JsonString
    ): InputDescriptorEvaluation {

        fun FieldConstraint.query() = with(fieldConstraintMatcher) { match(this@query, claimJsonString) }

        val fieldQueryResults: Map<FieldConstraint, FieldQueryResult> =
            fieldConstraints.associateWith { it.query() }
        val notMatchedFieldConstraints =
            fieldQueryResults.filterValues { it is RequiredFieldNotFound }.keys

        return when {
            notMatchedFieldConstraints.isNotEmpty() -> NotMatchedFieldConstraints(notMatchedFieldConstraints)
            else -> CandidateFound(fieldQueryResults.mapValues { it.value as Candidate })
        }
    }
}