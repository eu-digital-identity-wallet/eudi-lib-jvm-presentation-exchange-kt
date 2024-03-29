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

import eu.europa.ec.eudi.prex.JsonParser
import eu.europa.ec.eudi.prex.PresentationDefinition
import eu.europa.ec.eudi.prex.PresentationSubmission
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonObject
import java.io.InputStream

/**
 * The key under which a presentation definition is expected to be found
 * as defined in Presentation Exchange specification
 */
private const val presentationDefinitionKey = "presentation_definition"

private const val presentationSubmissionKey = "presentation_submission"

/**
 * https://identity.foundation/presentation-exchange/spec/v2.0.0/#embed-locations
 */
private enum class PresentationSubmissionEmbedLocation {
    JWT,
    OIDC,
    DIDComms,
    VP,
    CHAPI,
    ;

    fun extractFrom(json: JsonObject): JsonObject? {
        val root: JsonObject? = when (this) {
            OIDC, VP -> json
            JWT -> json["vp"]?.jsonObject
            DIDComms -> json["presentations~attach"]?.jsonObject?.get("data")?.jsonObject?.get("json")?.jsonObject
            CHAPI -> json["data"]?.jsonObject
        }
        return root?.get(presentationSubmissionKey)?.jsonObject
    }
}

/**
 * An implementation of the [JsonParser] that uses the
 * Kotlinx Serialization library
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DefaultJsonParser : JsonParser {

    override fun decodePresentationDefinition(inputStream: InputStream): Result<PresentationDefinition> =
        JsonSupport.decodeFromStream<JsonObject>(inputStream).mapToPd()

    override fun decodePresentationDefinition(jsonString: String): Result<PresentationDefinition> =
        JsonSupport.parseToJsonElement(jsonString).jsonObject.mapToPd()

    private fun JsonObject.mapToPd(): Result<PresentationDefinition> = runCatching {
        val pdObject = this[presentationDefinitionKey]?.jsonObject ?: this
        JsonSupport.decodeFromJsonElement(pdObject)
    }

    override fun PresentationDefinition.encode(): String = JsonSupport.encodeToString(this)

    override fun decodePresentationSubmission(inputStream: InputStream): Result<PresentationSubmission> =
        JsonSupport.decodeFromStream<JsonObject>(inputStream).mapToPS()

    override fun decodePresentationSubmission(jsonString: String): Result<PresentationSubmission> =
        JsonSupport.parseToJsonElement(jsonString).jsonObject.mapToPS()

    private fun JsonObject.mapToPS(): Result<PresentationSubmission> = runCatching {
        val pdObject = PresentationSubmissionEmbedLocation.entries
            .firstNotNullOfOrNull { location -> location.extractFrom(this@mapToPS) } ?: this
        JsonSupport.decodeFromJsonElement(pdObject)
    }

    override fun PresentationSubmission.encode(): String = JsonSupport.encodeToString(this)
}
