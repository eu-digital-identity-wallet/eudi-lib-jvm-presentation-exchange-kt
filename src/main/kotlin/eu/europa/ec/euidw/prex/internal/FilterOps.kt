package eu.europa.ec.euidw.prex.internal

import eu.europa.ec.euidw.prex.Filter
import net.pwall.json.schema.JSONSchema


internal class FilterOps(private val filterSerializer: (Filter) -> String) {

    /**
     * Checks whether a given [json] satisfies the
     * constraints described in the [Filter]
     */
    internal fun Filter.isMatchedBy(json: String): Boolean = isValid(this, json)

    private fun isValid(f: Filter, j: String): Boolean {
        val jsonStr = filterSerializer(f)
        val jsonSchema = JSONSchema.parse(jsonStr)
        return jsonSchema.validate(j, net.pwall.json.pointer.JSONPointer.root)
    }

}