package eu.europa.ec.euidw.prex.internal

import eu.europa.ec.euidw.prex.JsonParser
import eu.europa.ec.euidw.prex.PresentationDefinition
import eu.europa.ec.euidw.prex.PresentationSubmission
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
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
internal class DefaultJsonParser(private val json: Json) : JsonParser {

    override fun decodePresentationDefinition(inputStream: InputStream): Result<PresentationDefinition> =
        json.decodeFromStream<JsonObject>(inputStream).mapToPd()

    override fun decodePresentationDefinition(jsonString: String): Result<PresentationDefinition> =
        json.parseToJsonElement(jsonString).jsonObject.mapToPd()

    private fun JsonObject.mapToPd(): Result<PresentationDefinition> = runCatching {
        val pdObject = this[presentationDefinitionKey]?.jsonObject ?: this
        json.decodeFromJsonElement(pdObject)
    }

    override fun PresentationDefinition.encode(): String = json.encodeToString(this)

    override fun decodePresentationSubmission(inputStream: InputStream): Result<PresentationSubmission> =
        json.decodeFromStream<JsonObject>(inputStream).mapToPS()

    override fun decodePresentationSubmission(jsonString: String): Result<PresentationSubmission> =
        this.json.parseToJsonElement(jsonString).jsonObject.mapToPS()

    private fun JsonObject.mapToPS(): Result<PresentationSubmission> = runCatching {
        val pdObject = PresentationSubmissionEmbedLocation.values()
            .firstNotNullOfOrNull { location -> location.extractFrom(this@mapToPS) } ?: this
        json.decodeFromJsonElement(pdObject)
    }

    override fun PresentationSubmission.encode(): String = json.encodeToString(this)
}
