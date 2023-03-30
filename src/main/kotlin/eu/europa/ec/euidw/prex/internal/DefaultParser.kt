package eu.europa.ec.euidw.prex.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import eu.europa.ec.euidw.prex.JsonString
import eu.europa.ec.euidw.prex.Parser
import eu.europa.ec.euidw.prex.PresentationDefinition
import eu.europa.ec.euidw.prex.PresentationSubmission
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
    CHAPI;

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
 * An implementation of the [Parser]
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DefaultParser(private val format: Json) : Parser {


    override fun decodePresentationDefinition(inputStream: InputStream): Result<PresentationDefinition> =
        format.decodeFromStream<JsonObject>(inputStream).mapToPd()

    override fun decodePresentationDefinition(json: JsonString): Result<PresentationDefinition> =
        format.parseToJsonElement(json.value).jsonObject.mapToPd()

    private fun JsonObject.mapToPd(): Result<PresentationDefinition> = runCatching {
        val pdObject = this[presentationDefinitionKey]?.jsonObject ?: this
        format.decodeFromJsonElement(pdObject)
    }

    override fun decodePresentationSubmission(inputStream: InputStream): Result<PresentationSubmission> =
        format.decodeFromStream<JsonObject>(inputStream).mapToPS()

    override fun decodePresentationSubmission(json: JsonString): Result<PresentationSubmission> =
        format.parseToJsonElement(json.value).jsonObject.mapToPS()

    private fun JsonObject.mapToPS(): Result<PresentationSubmission> = runCatching {
        val pdObject = PresentationSubmissionEmbedLocation.values()
            .firstNotNullOfOrNull { location-> location.extractFrom(this@mapToPS) } ?: this
        format.decodeFromJsonElement(pdObject)
    }

}