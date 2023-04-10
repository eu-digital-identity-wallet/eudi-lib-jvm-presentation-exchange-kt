package eu.europa.ec.euidw.prex.internal

import eu.europa.ec.euidw.prex.*
import eu.europa.ec.euidw.prex.FieldQueryResult.CandidateField.Found
import eu.europa.ec.euidw.prex.FieldQueryResult.CandidateField.OptionalFieldNotFound
import eu.europa.ec.euidw.prex.FieldQueryResult.RequiredFieldNotFound


/**
 * Evaluates whether a claim satisfies a field constraint
 */
internal class FieldConstraintMatcher(private val filterOps: FilterOps) {

    /**
     * Evaluates whether a claim satisfies a field constraint
     */
    internal fun match(fieldConstraint: FieldConstraint, claim: String): FieldQueryResult {
        // Check whether the provided json satisfies the constraints of the filter
        // if the field constraint doesn't contain a filter, true is being returned
        fun matchFilter(j: String): Boolean =
            with(filterOps) { fieldConstraint.filter?.isMatchedBy(j) } ?: true

        // tries to locate within the JSON the specified path.
        // If there is a value checks it against the field constraint filter
        fun matchingField(path: JsonPath): String? =
            JsonPathOps.getJsonAtPath(path, claim)?.let { json->if(matchFilter(json)) json else null }

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