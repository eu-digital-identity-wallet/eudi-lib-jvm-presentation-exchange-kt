package eu.europa.ec.euidw.prex.internal

import eu.europa.ec.euidw.prex.*
import eu.europa.ec.euidw.prex.FieldQueryResult.CandidateField
import eu.europa.ec.euidw.prex.FieldQueryResult.RequiredFieldNotFound
import eu.europa.ec.euidw.prex.InputDescriptorEvaluation.CandidateClaim
import eu.europa.ec.euidw.prex.InputDescriptorEvaluation.NotMatchedFieldConstraints

/**
 * Evaluates whether an [InputDescriptor] matches a [Claim]
 */
internal class InputDescriptorEvaluator(private val fieldConstraintMatcher: FieldConstraintMatcher) {

    /**
     * Evaluates whether a given [claim] satisfies a set of [inputDescriptors]
     */
    internal fun matchInputDescriptors(
        presentationDefinitionFormat: Format?,
        inputDescriptors: Iterable<InputDescriptor>,
        claim: Claim
    ): Map<InputDescriptorId, InputDescriptorEvaluation> {
        val claimJsonString = claim.asJsonString()
        return inputDescriptors.associate { it.id to evaluate(presentationDefinitionFormat, it, claim.format, claimJsonString) }
    }

    /**
     * Evaluates whether a given [claimJsonString] satisfies a  [inputDescriptor]
     */
    private fun evaluate(
        presentationDefinitionFormat: Format?,
        inputDescriptor: InputDescriptor,
        claimFormat: ClaimFormat,
        claimJsonString: JsonString
    ): InputDescriptorEvaluation {
        val supportedFormat = isFormatSupported(inputDescriptor, presentationDefinitionFormat, claimFormat)
        return if (!supportedFormat) InputDescriptorEvaluation.UnsupportedFormat
        else checkFieldConstraints(inputDescriptor.constraints.fields(), claimJsonString)
    }

    private fun isFormatSupported(
        inputDescriptor: InputDescriptor,
        presentationDefinitionFormat: Format?,
        claimFormat: ClaimFormat
    ): Boolean =
        (inputDescriptor.format ?: presentationDefinitionFormat)
            ?.supportedClaimFormats
            ?.map { it.type }
            ?.contains(claimFormat) ?: true


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
            notMatchedFieldConstraints.isNotEmpty() -> NotMatchedFieldConstraints
            else -> CandidateClaim(fieldQueryResults.mapValues { it.value as CandidateField })
        }
    }
}