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
package eu.europa.ec.eudi.prex.internal

import eu.europa.ec.eudi.prex.*
import eu.europa.ec.eudi.prex.FieldQueryResult.CandidateField
import eu.europa.ec.eudi.prex.FieldQueryResult.RequiredFieldNotFound
import eu.europa.ec.eudi.prex.InputDescriptorEvaluation.CandidateClaim
import eu.europa.ec.eudi.prex.InputDescriptorEvaluation.NotMatchedFieldConstraints

/**
 * Evaluates whether an [InputDescriptor] matches a [Claim].
 *
 *
 */
internal object InputDescriptorEvaluator {

    /**
     * Evaluates whether a given [claim] satisfies a set of [inputDescriptors]
     */
    internal fun matchInputDescriptors(
        presentationDefinitionFormat: Format?,
        inputDescriptors: Iterable<InputDescriptor>,
        claim: Claim,
    ): Map<InputDescriptorId, InputDescriptorEvaluation> {
        val claimJsonString = claim.asJsonString()
        return inputDescriptors.associate {
            it.id to evaluate(
                presentationDefinitionFormat,
                it,
                claim.format,
                claimJsonString,
            )
        }
    }

    /**
     * Evaluates whether a given [claimJsonString] satisfies a  [inputDescriptor]
     */
    private fun evaluate(
        presentationDefinitionFormat: Format?,
        inputDescriptor: InputDescriptor,
        claimFormat: Format,
        claimJsonString: String,
    ): InputDescriptorEvaluation {
        val supportedFormat = isFormatSupported(inputDescriptor, presentationDefinitionFormat, claimFormat)
        return if (!supportedFormat) {
            InputDescriptorEvaluation.UnsupportedFormat
        } else {
            checkFieldConstraints(inputDescriptor.constraints.fields(), claimJsonString)
        }
    }

    private fun isFormatSupported(
        inputDescriptor: InputDescriptor,
        presentationDefinitionFormat: Format?,
        claimFormat: Format,
    ): Boolean {
        val supportedClaimFormats = (inputDescriptor.format ?: presentationDefinitionFormat)?.jsonObject()?.keys ?: emptySet()
        val claimFormats = claimFormat.jsonObject().keys
        return supportedClaimFormats.isEmpty() || supportedClaimFormats.intersect(claimFormats).isNotEmpty()
    }

    /**
     *
     */
    private fun checkFieldConstraints(
        fieldConstraints: List<FieldConstraint>,
        claimJsonString: String,
    ): InputDescriptorEvaluation {
        fun FieldConstraint.query() = with(FieldConstraintMatcher) { match(this@query, claimJsonString) }

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
