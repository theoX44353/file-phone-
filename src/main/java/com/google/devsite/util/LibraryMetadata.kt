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

import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultLink

/** Data class to store the metadata associated with a particular artifact ID */
data class LibraryMetadata(
    var groupId: String,
    var artifactId: String,
    var releaseNotesUrl: String,
) {
    internal val link: Link
        get() = DefaultLink(Link.Params(name = "$groupId:$artifactId", url = releaseNotesUrl))

    companion object {

        // Generate mapping of each file to its respective artifact ID and other metadata
        fun convertJsonMetadataToFileMap(
            metadataList: List<JsonLibraryMetadata>,
        ): Map<String, LibraryMetadata> {
            val fileMetadataMap = hashMapOf<String, LibraryMetadata>()
            metadataList.forEach { jsonLibraryMetadata ->
                val fileMetadata =
                    LibraryMetadata(
                        groupId = jsonLibraryMetadata.groupId,
                        artifactId = jsonLibraryMetadata.artifactId,
                        releaseNotesUrl = jsonLibraryMetadata.releaseNotesUrl,
                    )

                jsonLibraryMetadata.jarContents.forEach { file ->

                    // Only process Kotlin and Java files
                    if (file.endsWith(".kt") || file.endsWith("java")) {
                        fileMetadataMap[file] = fileMetadata
                    }
                }
            }

            return fileMetadataMap
        }
    }
}
