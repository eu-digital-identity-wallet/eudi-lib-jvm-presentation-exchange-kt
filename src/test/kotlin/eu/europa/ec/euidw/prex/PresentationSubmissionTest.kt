package eu.europa.ec.euidw.prex

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.InputStream

class PresentationSubmissionTest {

    private val parser = PresentationExchange.jsonParser

    @Test
    fun simpleCases() {
        listOf(
            "v2.0.0/presentation-submission/example.json",
            "v2.0.0/presentation-submission/appendix_OIDC_example.json",
            "v2.0.0/presentation-submission/appendix_VP_example.json",
            "v2.0.0/presentation-submission/appendix_JWT_example.json",
        ).forEach { testParseDefinition(it) }
    }

    private fun testParseDefinition(f: String): PresentationSubmission =
        parser.decodePresentationSubmission(load(f)!!)
            .also { println(it) }
            .fold(onSuccess = { it }, onFailure = { fail(it) })

    private fun load(f: String): InputStream? =
        PresentationDefinitionTest::class.java.classLoader.getResourceAsStream(f)
}
