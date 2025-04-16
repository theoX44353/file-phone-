/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devsite.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Data class to store and represent API version metadata parsed from JSON
 *
 * Each field has a defined [JsonProperty] to prevent bugs in case a field is renamed (which could
 * result in the Json parser not finding the new field name).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonVersionMetadata(
    @JsonProperty("class") val clazz: String,
    @JsonProperty("addedIn") val addedIn: String,
    @JsonProperty("deprecatedIn") val deprecatedIn: String? = null,
    @JsonProperty("methods") val methods: List<JsonVersionMetadataMethod> = emptyList(),
    @JsonProperty("fields") val fields: List<JsonVersionMetadataField> = emptyList(),
) {

    /** Nested data class to store API method metadata */
    data class JsonVersionMetadataMethod(
        @JsonProperty("method") var method: String,
        @JsonProperty("addedIn") val addedIn: String,
        @JsonProperty("deprecatedIn") val deprecatedIn: String? = null,
    )

    /** Nested data class to store API field metadata */
    data class JsonVersionMetadataField(
        @JsonProperty("field") var field: String,
        @JsonProperty("addedIn") val addedIn: String,
        @JsonProperty("deprecatedIn") val deprecatedIn: String? = null,
    )

    companion object {
        fun getMetadataFromFile(filename: String): List<JsonVersionMetadata> {
            if (filename.isEmpty()) {
                return emptyList()
            }

            return try {
                val jsonStr = File(filename).readText()
                jacksonObjectMapper().readValue(jsonStr)
            } catch (e: FileNotFoundException) {
                throw FileNotFoundException("Could not find API metadata file: $filename")
            } catch (e: JacksonException) {
                throw IOException("Error parsing JSON in API metadata file $filename", e)
            } catch (e: Exception) {
                throw Exception("Undefined error processing API metadata file $filename", e)
            }
        }
    }
}
