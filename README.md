# EUDIW - Library for Presentation exchange

## In a nutshell

Presentation exchange is a specification that defines:

* A way for the `Verifier` to describe proof requirements in terms of `PresentationDefintion` object
* A way for the `Holder` to describe submissions of proofs that allign with those requirements in terms of a `PresentationSubmission`

The use of this specification is mandatory by OpenID4VP

## Library functionality

* As a `Verifier` be able to
    * produce a valid `PresentationDefinition` in order to be communicated to a `Holder` using a protocol like `OpenID4VP`
    * decide whether  a given `PresentationSubmission` satisfies a specific `PresentationDefinition`

* As a `Holder/Wallet` be able to
    * parse/validate a `PresentationDefition`
    * to check if a claim stored in the wallet satisfies a `PresentationDefinition`
    * to produce a `PresentationSubmission` given a valid `PresentationDefintion` and a matching `Claim`

### Presentation Exchange optional features supported

| Feature                      | Status |
|------------------------------|--------|
| Submission requirement       | ❌      |
| Predicate                    | ❌      |
| Relational constraint        | ❌      |
| Credential status constraint | ❌      |
| JSON-LD framing              | ❌      |
| Retention                    | ❌      |


## Usage

```kotlin
import niscy.eudiw.prex.*

// Decoding a presentation definition json (string)
val pdJsonStr : String = TODO("provide a presentation definition json")
val pd = PresentationExchange.parser.decodePresentationDefinition(pdJsonStr).getOrThrow()

// Check whether a list of candidate claims can satisfy the 
// presentation definition requirements
val candidateClaims : List<Claim> = TODO("provide a list of candidate claims")
val match = PresentationExchange.matcher.match(pd, candidateClaims)


```
### Verifier: Produce a valid `PresentationDefinition`

Precondition:

* Verifier should know the data model of the claim(s)  that wants to be presented by the holder
* Verifier should be able to describe which formats (jwt, jwt_vc, ldp_vc etc.) and which algorithms is able to process

Library should offer a factory/builder to produce the `PresentationDefinition`.
The resulting `PresentationDefinition` should

* Adhere to the data model defined in the spec (JSON Schema validation)
* Contain valid JSONPath expressions

### Holder: Parse/Validate a `PresentationDefintion`

The holder should be able to verify that a JSON object is a syntactically valid `PresentationDefintion`:

* Adheres to the data model defined in the spec (JSON Schema validation)
* Contain valid JSONPath expressions



### Data Model

```mermaid
classDiagram
    class PresentationDefinition
    class InputDescriptor
    class FieldConstraint
    class LimitDisclosure
    PresentationDefinition *-- "1..*" InputDescriptor
    InputDescriptor o-- "*" FieldConstraint
    InputDescriptor --> "0..1" LimitDisclosure
    FieldConstraint -- "1..*" Path
    FieldConstraint : +Boolean optional
    note for InputDescriptor "Constraints for a single claim"
```

### Dependencies (to other libs)

* Json : [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
* JsonSchema: [Json Kotlin Schema](https://github.com/pwall567/json-kotlin-schema)
* JsonPath: [JsonPathKt](https://github.com/codeniko/JsonPathKt)



#### JSON Schema (optional)

Presentation Exchange spec provides a set of [JSON Schemas](https://github.com/decentralized-identity/presentation-exchange/tree/main/schemas/v2.0.0) that define the structure of the data model (`PresentationDefintion`, `PresentationSubmission` etc.). It is expected that comformant libraries adhere to those schemas.

### References

* [Presentation Exchange v2](https://identity.foundation/presentation-exchange/spec/v2.0.0/)
* [JSON Schema of data model](https://github.com/decentralized-identity/presentation-exchange/tree/main/schemas/v2.0.0)  