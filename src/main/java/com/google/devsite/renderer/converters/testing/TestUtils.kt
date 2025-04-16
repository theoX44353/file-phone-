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

package com.google.devsite.testing

import com.google.devsite.DevsiteConfiguration
import com.google.devsite.defaultValidNullabilityAnnotations
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.toCompactJsonString

/** A [DevsiteConfiguration] based off of AndroidX to use in tests. */
val defaultPluginsConfiguration =
    mutableListOf(
        PluginConfigurationImpl(
            fqPluginName = "com.google.devsite.DevsitePlugin",
            serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
            values =
                DevsiteConfiguration(
                        docRootPath = "reference",
                        projectPath = "androidx",
                        excludedPackages = null,
                        excludedPackagesForJava = null,
                        excludedPackagesForKotlin = null,
                        libraryMetadataFilename = null,
                        versionMetadataFilenames = null,
                        javaDocsPath = "",
                        kotlinDocsPath = "kotlin",
                        packagePrefixToRemoveInToc = null,
                        baseSourceLink = null,
                        basePropertySourceLink = null,
                        baseFunctionSourceLink = null,
                        annotationsNotToDisplay = null,
                        annotationsNotToDisplayJava = null,
                        annotationsNotToDisplayKotlin = null,
                        hidingAnnotations = listOf("androidx.annotation.RestrictTo"),
                        validNullabilityAnnotations =
                            defaultValidNullabilityAnnotations +
                                listOf("androidx.example.NonNull", "androidx.example.Nullable"),
                    )
                    .toCompactJsonString(),
        ),
    )
