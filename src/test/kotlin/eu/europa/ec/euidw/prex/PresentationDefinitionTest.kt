package eu.europa.ec.euidw.prex

import org.junit.jupiter.api.fail
import java.io.InputStream
import kotlin.test.Test

class PresentationDefinitionTest {



    @Test
    fun `format test`() {
        testParseDefinition("v2.0.0/presentation-definition/format_example.json")
    }


    @Test
    fun `basic example`() {
        testParseDefinition("v2.0.0/presentation-definition/basic_example.json")
    }


    @Test
    fun `single group example`() {
        testParseDefinition("v2.0.0/presentation-definition/single_group_example.json").also {
            it.submissionRequirements?.forEach { x -> println(x) }
        }
    }

    @Test
    fun `multi group example`() {
        testParseDefinition("v2.0.0/presentation-definition/multi_group_example.json").also {
            it.submissionRequirements?.forEach { x -> println(x) }
        }
    }

    @Test
    fun `mDL example`() {
        testParseDefinition("v2.0.0/presentation-definition/mDL-example.json").also {
            it.submissionRequirements?.forEach { x -> println(x) }
        }
    }


    private fun testParseDefinition(f: String): PresentationDefinition =
        PresentationExchange.jsonParser.decodePresentationDefinition(load(f)!!)
            .also { println(it) }
            .fold(onSuccess = { it }, onFailure = { fail(it) })

    private fun load(f: String): InputStream? =
        PresentationDefinitionTest::class.java.classLoader.getResourceAsStream(f)

}