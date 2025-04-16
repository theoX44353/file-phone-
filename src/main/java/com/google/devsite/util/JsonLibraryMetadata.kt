/*
 * Copyright 2022 The Android Open Source Project
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
 * Data class to store the metadata passed in from LIBRARY_METADATA_FILE.
 *
 * Each field has a defined [JsonProperty] to prevent bugs in case a field is renamed (which could
 * result in the Json parser not finding the new field name).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonLibraryMetadata(
    @JsonProperty("groupId") var groupId: String,
    @JsonProperty("artifactId") var artifactId: String,
    @JsonProperty("releaseNotesUrl") var releaseNotesUrl: String,
    @JsonProperty("jarContents") var jarContents: List<String>,
) {

    /**
     * Read and parse contents of the given JSON filename to a list of [JsonLibraryMetadata].
     *
     * Only includes metadata that has both a groupId and an artifactId.
     *
     * Returns an empty list if the filename is an empty string.
     *
     * Throws an exception if the file can't be read or if the JSON can't be parsed.
     */
    companion object {
        fun getMetadataFromFile(filename: String): List<JsonLibraryMetadata> {
            if (filename.isEmpty()) {
                return emptyList()
            }

            return try {
                val jsonStr = File(filename).readText()
                val list = jacksonObjectMapper().readValue<List<JsonLibraryMetadata>>(jsonStr)
                list.filter { it.groupId.isNotEmpty() && it.artifactId.isNotEmpty() }
            } catch (e: FileNotFoundException) {
                throw FileNotFoundException("Could not find library metadata file: $filename")
            } catch (e: JacksonException) {
                throw IOException("Error parsing JSON in library metadata file $filename", e)
            } catch (e: Exception) {
                throw Exception("Undefined error processing library metadata file $filename", e)
            }
        }
    }
}
