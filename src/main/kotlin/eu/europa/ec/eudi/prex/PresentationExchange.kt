package eu.europa.ec.eudi.prex

import eu.europa.ec.eudi.prex.internal.DefaultJsonParser
import eu.europa.ec.eudi.prex.internal.DefaultPresentationMatcher
import kotlinx.serialization.json.Json

/**
 * Entry point to the Presentation Exchange library
 */
object PresentationExchange {

    /**
     * Kotlinx JSON serialization
     */
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
