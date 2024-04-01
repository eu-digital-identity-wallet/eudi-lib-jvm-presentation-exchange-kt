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

import eu.europa.ec.eudi.prex.Constraints.LimitDisclosure.PREFERRED
import eu.europa.ec.eudi.prex.Constraints.LimitDisclosure.REQUIRED
import eu.europa.ec.eudi.prex.internal.*
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
@JvmInline
value class Id(val value: String) : java.io.Serializable

@Serializable
@JvmInline
value class Name(val value: String) : java.io.Serializable

@Serializable
@JvmInline
value class Purpose(val value: String) : java.io.Serializable

typealias NonEmptySet<T> = List<T>

@Serializable(with = FormatSerializer::class)
@JvmInline
value class Format private constructor(val json: String) : java.io.Serializable {

    fun jsonObject(): JsonObject = JsonSupport.parseToJsonElement(json).jsonObject

    companion object {
        fun format(json: JsonObject): Format = Format(JsonSupport.encodeToString(json))
    }
}

/**
 *  JSONPath string expressions
 */
@Serializable(with = JsonPathSerializer::class)
@JvmInline
value class JsonPath private constructor(val value: String) : java.io.Serializable {

    companion object {
        fun jsonPath(s: String): JsonPath? = if (JsonPathOps.isValid(s)) JsonPath(s) else null
    }
}

/**
 * A filter is a [JsonObject] which is expected to contain a
 * JSON Schema definition
 */
@Serializable(with = FilterSerializer::class)
@JvmInline
value class Filter private constructor(val json: String) : java.io.Serializable {
    fun jsonObject(): JsonObject = JsonSupport.parseToJsonElement(json).jsonObject

    companion object {
        fun filter(json: JsonObject): Filter = Filter(JsonSupport.encodeToString(json))
    }
}

/**
 * [paths]: an array of one or more [JsonPath] expressions
 * (as defined in the JSONPath Syntax Definition section)
 * that select a target value from the input.
 *
 */
@Serializable
data class FieldConstraint(
    @SerialName("path") @Required val paths: NonEmptySet<JsonPath>,
    val id: Id? = null,
    val name: Name? = null,
    val purpose: Purpose? = null,
    val filter: Filter? = null,
    val optional: Boolean = false,
    @SerialName("intent_to_retain") val intentToRetain: Boolean? = null,
) : java.io.Serializable

@Serializable(with = ConstraintsSerializer::class)
sealed interface Constraints : java.io.Serializable {
    fun fields(): List<FieldConstraint> = when (this) {
        is Fields -> fieldConstraints
        is FieldsAndDisclosure -> fieldConstraints
        else -> emptyList()
    }

    fun limitDisclosure(): LimitDisclosure? = when (this) {
        is LimitDisclosure -> this
        is FieldsAndDisclosure -> limitDisclosure
        else -> null
    }

    /**
     * Conformant Consumer MAY submit a response that contains more than the data described in the fields array.
     */
    data class Fields(val fieldConstraints: NonEmptySet<FieldConstraint>) : Constraints {
        init {
            check(fieldConstraints.isNotEmpty())
        }
    }

    /**
     * [REQUIRED] This indicates that the Conformant Consumer MUST limit submitted fields to those listed in the fields array (if present). Conformant Consumers are not required to implement support for this value, but they MUST understand this value sufficiently to return nothing (or cease the interaction with the Verifier) if they do not implement it.
     * [PREFERRED]  This indicates that the Conformant Consumer SHOULD limit submitted fields to those listed in the fields array (if present).
     */
    enum class LimitDisclosure : Constraints { REQUIRED, PREFERRED }

    data class FieldsAndDisclosure(
        val fieldConstraints: NonEmptySet<FieldConstraint>,
        val limitDisclosure: LimitDisclosure,
    ) : Constraints {
        init {
            check(fieldConstraints.isNotEmpty())
        }
    }

    companion object {

        /**
         * Creates a [Constraints] given a [list of field constraints][FieldConstraint] and/or [limitDisclosure]
         */
        fun of(fs: List<FieldConstraint>?, limitDisclosure: LimitDisclosure?): Constraints? =
            when {
                !fs.isNullOrEmpty() && limitDisclosure != null -> FieldsAndDisclosure(fs, limitDisclosure)
                !fs.isNullOrEmpty() && limitDisclosure == null -> Fields(fs)
                fs.isNullOrEmpty() && limitDisclosure != null -> limitDisclosure
                else -> null
            }
    }
}

@Serializable
@JvmInline
value class Group(val value: String) : java.io.Serializable

sealed interface From : java.io.Serializable {
    data class FromGroup(val group: Group) : From
    data class FromNested(val nested: List<SubmissionRequirement>) : From
}

sealed interface Rule : java.io.Serializable {
    data object All : Rule {
        private fun readResolve(): Any = All
    }

    data class Pick(val count: Int?, val min: Int?, val max: Int?) : Rule {
        init {
            count?.let { require(it > 0) { "Count must be greater than zero" } }
            min?.let { require(it >= 0) { "Min must be greater than or equal to zero" } }
            max?.let { require(it >= 0) { "Max must be greater than or equal to zero" } }
            min?.let { a -> max?.let { b -> require(a <= b) { "Max must be greater than or equal Min " } } }
        }
    }
}

@Serializable(with = SubmissionRequirementSerializer::class)
data class SubmissionRequirement(
    val rule: Rule,
    val from: From,
    val name: Name? = null,
    val purpose: Purpose? = null,
) : java.io.Serializable

fun SubmissionRequirement.allGroups(): Set<Group> =
    when (from) {
        is From.FromGroup -> setOf(from.group)
        is From.FromNested -> from.nested.flatMap { it.allGroups() }.toSet()
    }

@Serializable
@JvmInline
value class InputDescriptorId(val value: String) : java.io.Serializable

/**
 * Input Descriptors are objects used to describe the information a Verifier requires of a Holder.
 * It contains an [identifier][Id] and may contain [constraints][Constraints] on data values,
 * and an [explanation][Purpose] why a certain item or set of data is being requested
 */
@Serializable
data class InputDescriptor(
    val id: InputDescriptorId,
    val name: Name? = null,
    val purpose: Purpose? = null,
    val format: Format? = null,
    val constraints: Constraints,
    @SerialName("group") val groups: List<Group>? = null,
) : java.io.Serializable

/**
 * @param id The Presentation Definition MUST contain an id property.
 * The value of this property MUST be a string.
 * The string SHOULD provide a unique ID for the desired context.
 * For example, a UUID such as 32f54163-7166-48f1-93d8-f f217bdb0653
 * could provide an ID that is unique in a global context,
 * while a simple string such as my_presentation_definition_1 could be suitably unique in a local context
 * @param inputDescriptors The Presentation Definition MUST contain an input_descriptors property.
 * @param name The Presentation Definition MAY contain a name property.
 * If present, its value SHOULD be a human-friendly string intended
 * to constitute a distinctive designation of the Presentation Definition.
 * @param purpose purpose - The Presentation Definition MAY contain a purpose property.
 * If present, its value MUST be a string that describes the purpose for which the Presentation Definition's inputs are being used for.
 *
 */
@Serializable
data class PresentationDefinition(
    @Required val id: Id,
    val name: Name? = null,
    val purpose: Purpose? = null,
    val format: Format? = null,
    @Required
    @SerialName("input_descriptors")
    val inputDescriptors: List<InputDescriptor>,
    @SerialName("submission_requirements") val submissionRequirements: List<SubmissionRequirement>? = null,
) : java.io.Serializable {

    init {

        /**
         * Make sure that InputDescriptor Ids are unique
         */
        fun checkInputDescriptorIds() {
            require(inputDescriptors.distinctBy { it.id }.count() == inputDescriptors.size) {
                "InputDescriptor(s) should have PresentationDefinitionunique ids"
            }
        }

        /**
         * Input descriptor groups, if present, should be
         * referenced from submission groups
         */
        fun checkInputDescriptorGroups() {
            val allGroups = submissionRequirements?.flatMap { it.allGroups() }?.toSet() ?: emptySet()
            inputDescriptors.forEach { inputDescriptor ->
                inputDescriptor.groups?.forEach { grp ->
                    require(grp in allGroups) {
                        "Input descriptor ${inputDescriptor.id} " +
                            "contains groups ${grp.value} which is not present in submission requirements"
                    }
                }
            }
        }

        checkInputDescriptorIds()
        checkInputDescriptorGroups()
    }
}

@Serializable
data class DescriptorMap(
    @Required val id: InputDescriptorId,
    @Required val format: String,
    @Required val path: JsonPath,
) : java.io.Serializable

@Serializable
data class PresentationSubmission(
    @Required val id: Id,
    @Required
    @SerialName("definition_id")
    val definitionId: Id,
    @Required
    @SerialName("descriptor_map")
    val descriptorMaps: List<DescriptorMap>,
) : java.io.Serializable
