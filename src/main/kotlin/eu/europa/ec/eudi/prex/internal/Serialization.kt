/*
 * Copyright (c) 2023-2026 European Commission
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

import eu.europa.ec.eudi.prex.*
import eu.europa.ec.eudi.prex.Constraints.LimitDisclosure
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Kotlinx JSON serialization
 */
internal val JsonSupport: Json by lazy { Json { ignoreUnknownKeys = true } }

/**
 * Json ser-de for [Format]
 *
 */
@OptIn(ExperimentalSerializationApi::class)
internal object FormatSerializer : KSerializer<Format> {
    private val delegateSerializer = serializer<JsonObject>()
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("Format", delegateSerializer.descriptor)

    override fun deserialize(decoder: Decoder): Format {
        val data = decoder.decodeSerializableValue(delegateSerializer)
        return Format.format(data)
    }

    override fun serialize(encoder: Encoder, value: Format) {
        val data = value.jsonObject()
        encoder.encodeSerializableValue(delegateSerializer, data)
    }
}

/**
 * Json ser-de for [Constraints]
 */
@OptIn(ExperimentalSerializationApi::class)
internal object ConstraintsSerializer : KSerializer<Constraints> {

    private fun limitDisclosure(s: String): LimitDisclosure = when (s) {
        "preferred" -> LimitDisclosure.PREFERRED
        "required" -> LimitDisclosure.REQUIRED
        else -> throw SerializationException("$s not a valid limit disclosure value")
    }

    private val LimitDisclosure.jsonName
        get(): String = when (this) {
            LimitDisclosure.REQUIRED -> "required"
            LimitDisclosure.PREFERRED -> "preferred"
        }

    /**
     * Helper class to represents [Constraints] in Json
     */
    @Serializable
    private data class ConstraintsJson(
        val fields: List<FieldConstraint>? = null,
        @SerialName("limit_disclosure") val limitDisclosure: String? = null,
    )

    private val delegateSerializer = serializer<ConstraintsJson>()
    override val descriptor: SerialDescriptor = SerialDescriptor("Constraints", delegateSerializer.descriptor)

    override fun deserialize(decoder: Decoder): Constraints {
        val (fields, ldStr) = delegateSerializer.deserialize(decoder)
        val limitDisclosure = ldStr?.let { limitDisclosure(it) }

        return Constraints.of(fields, limitDisclosure)
            ?: throw SerializationException("At least on of fields or limitDisclosure is required")
    }

    override fun serialize(encoder: Encoder, value: Constraints) {
        val ldStr = value.limitDisclosure()?.jsonName
        val constraintJson = ConstraintsJson(value.fields(), ldStr)
        delegateSerializer.serialize(encoder, constraintJson)
    }
}

/**
 * Json ser-de for  [JsonPath]
 */
internal object JsonPathSerializer : KSerializer<JsonPath> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("JsonPath", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JsonPath {
        val s = decoder.decodeString()
        return JsonPath.jsonPath(s)
            ?: throw SerializationException("Not a valid JsonPath expression")
    }

    override fun serialize(encoder: Encoder, value: JsonPath) {
        encoder.encodeString(value.value)
    }
}

/**
 * Json ser-de for  [Filter]
 */
@OptIn(ExperimentalSerializationApi::class)
internal object FilterSerializer : KSerializer<Filter> {
    private val delegateSerializer = serializer<JsonObject>()
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("Filter", delegateSerializer.descriptor)

    override fun deserialize(decoder: Decoder): Filter {
        val data = decoder.decodeSerializableValue(delegateSerializer)
        return Filter.filter(data)
    }

    override fun serialize(encoder: Encoder, value: Filter) {
        val data = value.jsonObject()
        encoder.encodeSerializableValue(delegateSerializer, data)
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal object SubmissionRequirementSerializer : KSerializer<SubmissionRequirement> {

    @Serializable
    private enum class RuleType {
        @SerialName("all")
        ALL,

        @SerialName("pick")
        PICK,
    }

    @Serializable
    private data class JsonSubmissionRequirement(
        @Required val rule: RuleType,
        val count: Int? = null,
        val min: Int? = null,
        val max: Int? = null,
        val from: Group? = null,
        @SerialName("from_nested") val fromNested: List<JsonSubmissionRequirement>? = null,
        val name: Name? = null,
        val purpose: Purpose? = null,
    )

    private val delegateSerializer = serializer<JsonSubmissionRequirement>()
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("SubmissionRequirement", delegateSerializer.descriptor)

    override fun deserialize(decoder: Decoder): SubmissionRequirement {
        val data = decoder.decodeSerializableValue(delegateSerializer)
        return fromJson(data)
    }

    override fun serialize(encoder: Encoder, value: SubmissionRequirement) {
        val data = toJson(value)
        encoder.encodeSerializableValue(delegateSerializer, data)
    }

    private fun fromJson(data: JsonSubmissionRequirement): SubmissionRequirement {
        val from = when {
            (data.from != null && data.fromNested != null) || (data.from == null && data.fromNested == null) ->
                throw SerializationException("one of  from or from_nested must be provided")

            data.from != null -> From.FromGroup(data.from)
            data.fromNested != null -> From.FromNested(data.fromNested.map { fromJson(it) })
            else -> error("Something wrong")
        }

        val rule = when (data.rule) {
            RuleType.ALL -> Rule.All
            RuleType.PICK -> Rule.Pick(count = data.count, min = data.min, max = data.max)
        }
        return SubmissionRequirement(rule = rule, from = from, name = data.name, purpose = data.purpose)
    }

    private fun toJson(value: SubmissionRequirement): JsonSubmissionRequirement {
        val (count, min, max) = when (value.rule) {
            is Rule.Pick -> Triple(value.rule.count, value.rule.min, value.rule.max)
            is Rule.All -> Triple(null, null, null)
        }

        val (from, fromNested) = when (value.from) {
            is From.FromGroup -> value.from.group to null
            is From.FromNested -> null to value.from.nested.map { toJson(it) }
        }
        return JsonSubmissionRequirement(
            rule = when (value.rule) {
                is Rule.All -> RuleType.ALL
                is Rule.Pick -> RuleType.PICK
            },
            count = count,
            min = min,
            max = max,
            from = from,
            fromNested = fromNested,
            name = value.name,
            purpose = value.purpose,
        )
    }
}
