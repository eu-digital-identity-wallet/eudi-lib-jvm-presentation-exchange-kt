package eu.europa.ec.euidw.prex

import com.fasterxml.jackson.databind.JsonNode as JacksonJsonNode
import com.fasterxml.jackson.databind.ObjectMapper as JacksonObjectMapper
import com.nfeld.jsonpathkt.JsonPath as ExternalJsonPath

internal object JsonPathOps {

    /**
     * Checks that the provided [path][String] is JSON Path
     */
    fun isValid(path: String): Boolean = path.toJsonPath().isSuccess

    /**
     * Extracts from the given [JSON][jsonString] the content
     * at [path][jsonPath]. Returns the [JsonString] found at the path, if found
     */
    fun getJsonAtPath(jsonPath: JsonPath, jsonString: JsonString): JsonString? =
        ExternalJsonPath(jsonPath.value).readFromJson<JacksonJsonNode>(jsonString.value)?.toJsonString()

    private fun String.toJsonPath(): Result<com.nfeld.jsonpathkt.JsonPath> = runCatching {
        ExternalJsonPath(this)
    }

    private fun JacksonJsonNode.toJsonString() : JsonString =
        JsonString(objectMapper.writeValueAsString(this))

    /**
     * Jackson JSON support
     */
    private val objectMapper: JacksonObjectMapper by lazy { JacksonObjectMapper() }
}