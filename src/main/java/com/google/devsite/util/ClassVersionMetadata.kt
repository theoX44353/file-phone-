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

/** Data class to store the version metadata associated with a Class */
data class ClassVersionMetadata(
    val className: String,
    val addedIn: String,
    val deprecatedIn: String? = null,
    val methodVersions: Map<String, MethodVersionMetadata> = emptyMap(),
    val fieldVersions: Map<String, FieldVersionMetadata> = emptyMap(),
) {

    /** Data class to store the version metadata associated with a method */
    data class MethodVersionMetadata(
        val methodName: String,
        val addedIn: String,
        val deprecatedIn: String? = null,
    )

    /** Data class to store the version metadata associated with a field */
    data class FieldVersionMetadata(
        val fieldName: String,
        val addedIn: String,
        val deprecatedIn: String? = null,
    )
}

/** Converts JSON version metadata for fields into a map */
fun createFieldVersionMetadata(
    jsonVersionMetadataFields: List<JsonVersionMetadata.JsonVersionMetadataField>,
): Map<String, ClassVersionMetadata.FieldVersionMetadata> {
    val versionMetadataMap = hashMapOf<String, ClassVersionMetadata.FieldVersionMetadata>()

    jsonVersionMetadataFields.forEach { jsonVersionMetadataField ->
        versionMetadataMap[jsonVersionMetadataField.field] =
            ClassVersionMetadata.FieldVersionMetadata(
                fieldName = jsonVersionMetadataField.field,
                addedIn = jsonVersionMetadataField.addedIn,
                deprecatedIn = jsonVersionMetadataField.deprecatedIn,
            )
    }

    return versionMetadataMap
}

/** Converts JSON version metadata for methods into a map */
fun createMethodVersionMetadata(
    jsonVersionMetadataMethods: List<JsonVersionMetadata.JsonVersionMetadataMethod>,
): Map<String, ClassVersionMetadata.MethodVersionMetadata> {
    val versionMetadataMap = hashMapOf<String, ClassVersionMetadata.MethodVersionMetadata>()

    jsonVersionMetadataMethods.forEach { jsonVersionMetadataMethod ->
        versionMetadataMap[jsonVersionMetadataMethod.method] =
            ClassVersionMetadata.MethodVersionMetadata(
                methodName = jsonVersionMetadataMethod.method,
                addedIn = jsonVersionMetadataMethod.addedIn,
                deprecatedIn = jsonVersionMetadataMethod.deprecatedIn,
            )
    }

    return versionMetadataMap
}
