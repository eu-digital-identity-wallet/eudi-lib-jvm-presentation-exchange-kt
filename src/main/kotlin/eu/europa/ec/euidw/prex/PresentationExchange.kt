package eu.europa.ec.euidw.prex

import eu.europa.ec.euidw.prex.internal.*
import kotlinx.serialization.json.Json

/**
 * Entry point to the Presentation Exchange library
 */
object PresentationExchange {


    /**
     * Kotlinx JSON serialization
     */
    private val jsonFormat: Json by lazy { Json { ignoreUnknownKeys = true } }

    /**
     * JSON serialization/deserialization
     */
    val parser: Parser by lazy { DefaultParser(jsonFormat) }

    /**
     * Matching of credentials to a presentation definition
     */
    val matcher: PresentationMatcher by lazy { DefaultPresentationMatcher.build(jsonFormat) }
}