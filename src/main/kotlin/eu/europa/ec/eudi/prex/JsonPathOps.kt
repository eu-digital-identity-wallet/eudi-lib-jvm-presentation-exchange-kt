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
package eu.europa.ec.eudi.prex

import com.fasterxml.jackson.databind.JsonNode as JacksonJsonNode
import com.fasterxml.jackson.databind.ObjectMapper as JacksonObjectMapper
import com.nfeld.jsonpathkt.JsonPath as ExternalJsonPath

/**
 * JSON Path related operations
 */
internal object JsonPathOps {

    /**
     * Checks that the provided [path][String] is JSON Path
     */
    internal fun isValid(path: String): Boolean = path.toJsonPath().isSuccess

    /**
     * Extracts from given [JSON][jsonString] the content
     * at [path][jsonPath]. Returns the value found at the path, if found
     */
    internal fun getJsonAtPath(jsonPath: JsonPath, jsonString: String): String? =
        ExternalJsonPath(jsonPath.value)
            .readFromJson<JacksonJsonNode>(jsonString)
            ?.toJsonString()

    private fun String.toJsonPath(): Result<com.nfeld.jsonpathkt.JsonPath> = runCatching {
        ExternalJsonPath(this)
    }

    private fun JacksonJsonNode.toJsonString(): String = objectMapper.writeValueAsString(this)

    /**
     * Jackson JSON support
     */
    private val objectMapper: JacksonObjectMapper by lazy { JacksonObjectMapper() }
}
