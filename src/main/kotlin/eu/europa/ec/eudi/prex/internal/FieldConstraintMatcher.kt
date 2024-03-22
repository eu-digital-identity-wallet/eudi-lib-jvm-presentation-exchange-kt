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

import eu.europa.ec.eudi.prex.FieldConstraint
import eu.europa.ec.eudi.prex.FieldQueryResult
import eu.europa.ec.eudi.prex.FieldQueryResult.CandidateField.Found
import eu.europa.ec.eudi.prex.FieldQueryResult.CandidateField.OptionalFieldNotFound
import eu.europa.ec.eudi.prex.FieldQueryResult.RequiredFieldNotFound
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.JsonPathOps

/**
 * Evaluates whether a claim satisfies a field constraint
 */
internal object FieldConstraintMatcher {

    /**
     * Evaluates whether a claim satisfies a field constraint
     */
    internal fun match(fieldConstraint: FieldConstraint, claim: String): FieldQueryResult {
        // Check whether the provided json satisfies the constraints of the filter
        // if the field constraint doesn't contain a filter, true is being returned
        fun matchFilter(j: String): Boolean =
            with(FilterOps) { fieldConstraint.filter?.isMatchedBy(j) } ?: true

        // Tries to locate within the JSON the specified path.
        // If there is a value checks it against the field constraint filter
        fun matchingField(path: JsonPath): String? =
            JsonPathOps.getJsonAtPath(path, claim)?.let { json -> if (matchFilter(json)) json else null }

        fun notFound() = if (fieldConstraint.optional) OptionalFieldNotFound else RequiredFieldNotFound

        var maybeFound: Found? = null
        for (path in fieldConstraint.paths) {
            val json = matchingField(path)
            if (null != json) {
                maybeFound = Found(path, json)
                break
            }
        }
        return maybeFound ?: notFound()
    }
}
