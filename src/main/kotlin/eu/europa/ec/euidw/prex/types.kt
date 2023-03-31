package eu.europa.ec.euidw.prex

import eu.europa.ec.euidw.prex.Constraints.LimitDisclosure.PREFERRED
import eu.europa.ec.euidw.prex.Constraints.LimitDisclosure.REQUIRED
import eu.europa.ec.euidw.prex.internal.*
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@JvmInline
value class JsonString(val value: String)


@Serializable
@JvmInline
value class Id(val value: String)

@Serializable
@JvmInline
value class Name(val value: String)

@Serializable
@JvmInline
value class Purpose(val value: String)


typealias NonEmptySet<T> = List<T>
//class NonEmptySet<out T>(head: T, tail: Set<T>) : Set<T> by (setOf(head) + tail)

/**
 *  According to JSON Web Algorithms (JWA)
 */
@Serializable(with = JwtAlgorithmSerializer::class)
sealed interface JwtAlgorithm {

    val name: String

    enum class Hmac : JwtAlgorithm {
        HS256, // HMAC using SHA-256 (Required)
        HS384, // HMAC using SHA-384
        HS512 // HMAC using SHA-512
    }

    enum class DigSig : JwtAlgorithm {
        RS256, // RSASSA-PKCS1-v1_5 using SHA-256 (Recommended)
        RS384, // RSASSA-PKCS1-v1_5 using SHA-384
        RS512, // RSASSA-PKCS1-v1_5 using SHA-512
        ES256, // ECDSA using P-256 and SHA-256 (Recommended)
        ES256K,
        ES384, // ECDSA using P-384 and SHA-384
        ES512, // ECDSA using P-521 and SHA-512
        PS256, // RSASSA-PSS using SHA-256 and MGF1 with SHA-256
        PS384,
        PS512,
        EdDSA
    }


    companion object {

        @JvmStatic
        fun jwtAlgorithm(name: String): JwtAlgorithm? = hmacAlgorithm(name) ?: digSigAlgorithm(name)

        private fun hmacAlgorithm(name: String): Hmac? = Hmac.values().find { it.name == name }

        private fun digSigAlgorithm(name: String): DigSig? = DigSig.values().find { it.name == name }
    }
}


/**
 * https://w3c-ccg.github.io/ld-cryptosuite-registry/
 */
enum class LdpProof {
    Ed25519Signature2018,
    RsaSignature2018,
    RsaVerificationKey2018,
    EcdsaSecp256k1Signature2019,
    EcdsaSecp256k1VerificationKey2019,
    EcdsaSecp256k1RecoverySignature2020,
    EcdsaSecp256k1RecoveryMethod2020,
    JsonWebSignature2020,
    JwsVerificationKey2020,
    GpgSignature2020,
    GpgVerificationKey2020,
    JcsEd25519Signature2020,
    JcsEd25519Key2020,
    BbsBlsSignature2020,
    BbsBlsSignatureProof2020,
    Bls12381G1Key2020,
    Bls12381G2Key2020
}


@Serializable(with = ClaimFormatSerializer::class)
sealed interface ClaimFormat {

    object MsoMdoc : ClaimFormat

    enum class JwtType : ClaimFormat {
        JWT,
        JWT_VC,
        JWT_VP
    }

    enum class LdpType : ClaimFormat {
        LDP,
        LDP_VC,
        LDP_VP
    }


}


sealed interface SupportedClaimFormat<CF : ClaimFormat> {

    val type: CF

    data class JwtBased(override val type: ClaimFormat.JwtType, val algorithms: Set<JwtAlgorithm>) :
        SupportedClaimFormat<ClaimFormat.JwtType> {
        init {
            require(algorithms.isNotEmpty())
        }
    }

    data class MsoMdoc(val algorithms: Set<JwtAlgorithm>) : SupportedClaimFormat<ClaimFormat.MsoMdoc> {
        override val type: ClaimFormat.MsoMdoc
            get() = ClaimFormat.MsoMdoc
    }

    data class LdpBased(override val type: ClaimFormat.LdpType, val proofTypes: Set<LdpProof>) :
        SupportedClaimFormat<ClaimFormat.LdpType> {
        init {
            require(proofTypes.isNotEmpty())
        }
    }

    companion object {
        internal fun supportedClaimFormat(
            type: ClaimFormat,
            algorithms: Set<JwtAlgorithm>? = null,
            proofTypes: Set<LdpProof>? = null
        ): SupportedClaimFormat<*>? = when (type) {
            is ClaimFormat.JwtType -> jwt(type, algorithms)
            is ClaimFormat.LdpType -> ldp(type, proofTypes)
            is ClaimFormat.MsoMdoc -> msoMdoc(algorithms)
        }

        @JvmStatic
        fun ldp(
            type: ClaimFormat.LdpType,
            proofTypes: Set<LdpProof>?
        ) = if (!proofTypes.isNullOrEmpty()) LdpBased(type, proofTypes) else null

        @JvmStatic
        fun jwt(
            type: ClaimFormat.JwtType,
            algorithms: Set<JwtAlgorithm>?
        ): JwtBased? = if (!algorithms.isNullOrEmpty()) JwtBased(type, algorithms) else null

        @JvmStatic
        fun msoMdoc(
            algorithms: Set<JwtAlgorithm>?
        ): MsoMdoc? = if (!algorithms.isNullOrEmpty()) MsoMdoc(algorithms) else null
    }

}

@Serializable(with = FormatSerializer::class)
data class Format(val supportedClaimFormats: List<SupportedClaimFormat<*>> = emptyList()) {
    companion object {
        fun invoke(supportedClaimFormat: SupportedClaimFormat<*>, vararg other: SupportedClaimFormat<*>): Format =
            Format(listOf(supportedClaimFormat).plus(other.toList()))
    }
}

/**
 *  JSONPath string expressions
 */
@Serializable(with = JsonPathSerializer::class)
@JvmInline
value class JsonPath private constructor(val value: String) {

    companion object {
        fun jsonPath(s: String): JsonPath? = if (JsonPathOps.isValid(s)) JsonPath(s) else null
    }
}

// TODO Currently we don't check that Filter is a valid JSON Schema
/**
 * A filter is a [JsonObject] which is expected to contain a
 * JSON Schema definition
 */
typealias Filter = JsonObject

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
    @SerialName("intent_to_retain") val intentToRetain: Boolean? = null
)

@Serializable(with = ConstraintsSerializer::class)
sealed interface Constraints {
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
        val limitDisclosure: LimitDisclosure
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
value class Group(val value: String)


sealed interface From {
    data class FromGroup(val group: Group) : From
    data class FromNested(val nested: List<SubmissionRequirement>) : From
}


sealed interface Rule {
    object All : Rule {
        override fun toString(): String = "All"
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
)

fun SubmissionRequirement.allGroups(): Set<Group> =
    when (from) {
        is From.FromGroup -> setOf(from.group)
        is From.FromNested -> from.nested.flatMap { it.allGroups() }.toSet()
    }


@Serializable
@JvmInline
value class InputDescriptorId(val value: String)

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
    @SerialName("group") val groups: List<Group>? = null
)

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
    @Required @SerialName("input_descriptors") val inputDescriptors: List<InputDescriptor>,
    @SerialName("submission_requirements") val submissionRequirements: List<SubmissionRequirement>? = null
) {

    init {

        /**
         * Make sure that InputDescriptor Ids are unique
         */
        fun checkInputDescriptorIds() {
            require(inputDescriptors.distinctBy { it.id }.count() == inputDescriptors.size) {
                "InputDescriptor(s) should have unique ids"
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
                    require(grp in allGroups) { "Input descriptor ${inputDescriptor.id} contains groups ${grp.value} which is not present in submission requirements" }
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
    @Required val format: ClaimFormat,
    @Required val path: JsonPath
)

// TODO: PresentationSubmission add path_nested
@Serializable
data class PresentationSubmission(
    @Required val id: Id,
    @Required @SerialName("definition_id") val definitionId: Id,
    @Required @SerialName("descriptor_map") val descriptorMaps: List<DescriptorMap>
)