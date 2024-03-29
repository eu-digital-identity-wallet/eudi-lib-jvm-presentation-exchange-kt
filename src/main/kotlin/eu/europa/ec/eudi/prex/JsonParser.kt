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

import java.io.InputStream

interface JsonParser {

    /**
     * Tries to parse the given [inputStream] into a [PresentationDefinition].
     * It is assumed that the [inputStream] corresponds to a Json object that either contains
     * a Json object under "presentation_definition" key or is the [PresentationDefinition] itself
     */
    fun decodePresentationDefinition(inputStream: InputStream): Result<PresentationDefinition>

    /**
     * Tries to parse the given [jsonString] into a [PresentationDefinition].
     * It is assumed that the [jsonString] corresponds to an object that either contains
     * a Json object under "presentation_definition" key or is the [PresentationDefinition] itself
     */
    fun decodePresentationDefinition(jsonString: String): Result<PresentationDefinition>

    fun PresentationDefinition.encode(): String

    /**
     * Tries to parse the given [inputStream] into a [PresentationSubmission].
     * It is assumed that the [inputStream] corresponds to a json object that either contains
     * a Json object under some well known location (embedded locations) or is the [PresentationSubmission]
     *
     * @see <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#embed-locations>embed-locations</a>
     */
    fun decodePresentationSubmission(inputStream: InputStream): Result<PresentationSubmission>

    /**
     * Tries to parse the given [jsonString] into a [PresentationSubmission].
     * It is assumed that the [jsonString] corresponds to a json object that either contains
     * a Json object under some well known location (embedded locations) or is the [PresentationSubmission]
     *
     * @see <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#embed-locations>embed-locations</a>
     */
    fun decodePresentationSubmission(jsonString: String): Result<PresentationSubmission>

    fun PresentationSubmission.encode(): String
}
