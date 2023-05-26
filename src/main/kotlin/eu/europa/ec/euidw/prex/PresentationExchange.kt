package eu.europa.ec.euidw.prex

import eu.europa.ec.euidw.prex.internal.DefaultJsonParser
import eu.europa.ec.euidw.prex.internal.DefaultPresentationMatcher
import kotlinx.serialization.json.Json

/**
 * Entry point to the Presentation Exchange library
 */
object PresentationExchange {

    /**
     * Kotlinx JSON serialization
     */
    // TODO Perhaps the ignoreUnknownKeys needs to be removed
    private val json: Json by lazy { Json { ignoreUnknownKeys = true } }

    /**
     * JSON serialization/deserialization
     */
    val jsonParser: JsonParser by lazy { DefaultJsonParser(json) }

    /**
     * Matching of credentials to a presentation definition
     */
    val matcher: PresentationMatcher by lazy { DefaultPresentationMatcher.build(json) }
}
