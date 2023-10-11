# Module Presentation Exchange

The `eudi-lib-jvm-presentation-exchange-kt` is a kotlin library that implements the functionality as described in
[Presentation Exchange v2](https://identity.foundation/presentation-exchange/spec/v2.0.0/).

This is a specification that defines:

* A way for the `Verifier` to describe proof requirements in terms of `PresentationDefintion` object
* A way for the `Holder` to describe submissions of proofs that align with those requirements in terms of a `PresentationSubmission`

The use of this specification is mandatory by OpenID4VP

## eu.europa.ec.eudi.prex

The library offers the following functionality:

* As a `Verifier` be able to
    * produce a valid `PresentationDefinition` in order to be communicated to a `Holder` using a protocol like `OpenID4VP`
    * decide whether a given `PresentationSubmission` satisfies a specific `PresentationDefinition`

* As a `Holder/Wallet` be able to
    * parse/validate a `PresentationDefition`
    * to check if a claim stored in the wallet satisfies a `PresentationDefinition`
    * to produce a `PresentationSubmission` given a valid `PresentationDefintion` and a matching `Claim`

### Presentation Exchange optional features supported

The table bellow summarizes the set of optional features defined by [Presentation Exchange v2](https://identity.foundation/presentation-exchange/spec/v2.0.0/)
which are supported by the library.
Currently, no optional features are being supported, except [retention](https://identity.foundation/presentation-exchange/spec/v2.0.0/#retention-feature)

| Feature                      | Status |
|------------------------------|--------|
| Submission requirement       | ❌      |
| Predicate                    | ❌      |
| Relational constraint        | ❌      |
| Credential status constraint | ❌      |
| JSON-LD framing              | ❌      |
| Retention                    | ✅      |