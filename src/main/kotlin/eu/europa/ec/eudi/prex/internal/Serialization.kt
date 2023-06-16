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

import eu.europa.ec.eudi.prex.*
import eu.europa.ec.eudi.prex.Constraints.LimitDisclosure
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Json ser-de for [Format]
 *
 */
internal object FormatSerializer : KSerializer<Format> {

    /**
     * Internal class presenting - in Json - a set of [JwtAlgorithm] or a set of [LdpProof]
     */
    @Serializable
    private data class AlgorithmsOrProofTypes
        @OptIn(ExperimentalSerializationApi::class)
        constructor(
            @EncodeDefault(EncodeDefault.Mode.NEVER)
            @SerialName("alg")
            val algorithms: Set<JwtAlgorithm>? = null,
            @EncodeDefault(EncodeDefault.Mode.NEVER)
            @SerialName("proof_type")
            val proofTypes: Set<LdpProof>? = null,
        )

    /**
     * Json representation of a [Format]. It will be mapped into a map
     * having as key entry a [ClaimFormat] and as value [AlgorithmsOrProofTypes]
     */
    private val delegateSerializer = serializer<Map<ClaimFormat, AlgorithmsOrProofTypes>>()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("Format", delegateSerializer.descriptor)

    override fun deserialize(decoder: Decoder): Format {
        return decoder.decodeSerializableValue(delegateSerializer).map {
            val name = it.key
            val (algorithms, proofTypes) = it.value
            SupportedClaimFormat.supportedClaimFormat(name, algorithms, proofTypes)
                ?: throw SerializationException("Invalid definition of $name. Misses alg or proof_type")
        }.let { Format(it) }
    }

    override fun serialize(encoder: Encoder, value: Format) {
        val data = value.supportedClaimFormats.associate {
            it.type to when (it) {
                is SupportedClaimFormat.JwtBased -> AlgorithmsOrProofTypes(algorithms = it.algorithms)
                is SupportedClaimFormat.LdpBased -> AlgorithmsOrProofTypes(proofTypes = it.proofTypes)
                is SupportedClaimFormat.MsoMdoc -> AlgorithmsOrProofTypes(algorithms = it.algorithms)
            }
        }
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
 * Json ser-de for [JwtAlgorithm]
 */
internal object JwtAlgorithmSerializer : KSerializer<JwtAlgorithm> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JwtAlgorithm", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JwtAlgorithm) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): JwtAlgorithm {
        val name: String = decoder.decodeString()
        return JwtAlgorithm.jwtAlgorithm(name) ?: throw SerializationException("Not a valid JwtAlgorithm $name")
    }
}

/**
 * Json ser-de for [ClaimFormat]
 */
internal object ClaimFormatSerializer : KSerializer<ClaimFormat> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ClaimFormatType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ClaimFormat {
        val value = decoder.decodeString()
        return claimFormaType(value) ?: throw SerializationException("Unsupported value $value")
    }

    override fun serialize(encoder: Encoder, value: ClaimFormat) {
        encoder.encodeString(value.asString())
    }

    private fun claimFormaType(name: String): ClaimFormat? = when (name) {
        "jwt" -> ClaimFormat.JwtType.JWT
        "jwt_vc" -> ClaimFormat.JwtType.JWT_VC
        "jwt_vp" -> ClaimFormat.JwtType.JWT_VP
        "sd_jwt" -> ClaimFormat.JwtType.SD_JWT
        "hb_jwt" -> ClaimFormat.JwtType.HB_JWT
        "ldp" -> ClaimFormat.LdpType.LDP
        "ldp_vc" -> ClaimFormat.LdpType.LDP_VC
        "ldp_vp" -> ClaimFormat.LdpType.LDP_VP
        "mso_mdoc" -> ClaimFormat.MsoMdoc
        else -> null
    }

    private fun ClaimFormat.asString() = when (this) {
        ClaimFormat.JwtType.JWT -> "jwt"
        ClaimFormat.JwtType.JWT_VC -> "jwt_vc"
        ClaimFormat.JwtType.JWT_VP -> "jwt_vp"
        ClaimFormat.JwtType.SD_JWT -> "sd_jwt"
        ClaimFormat.JwtType.HB_JWT -> "hb_jwt"
        ClaimFormat.LdpType.LDP -> "ldp"
        ClaimFormat.LdpType.LDP_VC -> "ldp_vc"
        ClaimFormat.LdpType.LDP_VP -> "ldp_vp"
        ClaimFormat.MsoMdoc -> "mso_mdoc"
    }
}

/**
 * Json ser-de for [JsonPath]
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
