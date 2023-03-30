package eu.europa.ec.euidw.prex.internal

import eu.europa.ec.euidw.prex.Filter
import eu.europa.ec.euidw.prex.JsonString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.pwall.json.schema.JSONSchema


internal class FilterOps(private val format: Json) {

    /**
     * Checks whether a given [json][JsonString] satisfies the
     * constraints described in the [Filter]
     */
    internal fun Filter.isMatchedBy(json: JsonString): Boolean =
        toSchema().validate(json.value, net.pwall.json.pointer.JSONPointer.root)

    /**
     * Converts the [Filter] to [JSONSchema]
     */
    private fun Filter.toSchema(): JSONSchema {
        val jsonStr = format.encodeToString(this)
        return JSONSchema.parse(jsonStr)
    }
}