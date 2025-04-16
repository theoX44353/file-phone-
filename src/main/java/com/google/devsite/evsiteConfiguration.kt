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

package com.google.devsite

import com.google.devsite.renderer.Language
import org.jetbrains.dokka.plugability.ConfigurableBlock

/**
 * Configuration for the [DevsitePlugin]. This should be provided as a JSON string in the
 * `pluginsConfiguration` of the config file provided to Dackka.
 *
 * @param docRootPath The beginning of every file path generated. If not specified, it defaults to
 *   "reference".
 * @param javaDocsPath The Java version of the docs will be placed [docRootPath]/[javaDocsPath]. If
 *   [javaDocsPath] is null or unspecified, Java docs will not be generated. If [javaDocsPath] is an
 *   empty string, the Java docs go directly in [docRootPath].
 * @param kotlinDocsPath Equivalent to [javaDocsPath] for the Kotlin docs. [javaDocsPath] and
 *   [kotlinDocsPath] cannot both be null and cannot have the same value.
 * @param projectPath A subdirectory of the [javaDocsPath] and [kotlinDocsPath] where the
 *   package-list, table of contents, package index, and class index are placed. If [projectPath] is
 *   empty, these files are placed directly in the [javaDocsPath] and [kotlinDocsPath] directories.
 *   Specifying a [projectPath] is required.
 * @param excludedPackages Set of packages that Dackka will exclude for both Java and Kotlin refdoc
 *   generation. This is a list of regular expression strings. Defaults to an empty list.
 * @param excludedPackagesForJava Packages to exclude for Java refdoc generation, in addition to
 *   those in [excludedPackages]. Defaults to an empty list.
 * @param excludedPackagesForKotlin Packages to exclude for Kotlin refdoc generation, in addition to
 *   those in [excludedPackages]. Defaults to an empty list.
 * @param libraryMetadataFilename The location of the JSON file containing the library metadata.
 *   Optional, if not specified, library metadata will not be displayed.
 * @param versionMetadataFilenames The location of the JSON file(s) containing the library API
 *   version metadata. Optional, if not specified, library API version metadata will not be
 *   displayed.
 * @param includedHeadTagsPathJava A path to a file to include in the `<head>` section of each
 *   generated HTML page for the Java docs. See the
 *   [devsite docs](https://developers.google.com/devsite/reference/templatetags/include) for
 *   details on the requirements for the path. If not specified, defaults to
 *   "_shared/_reference-head-tags.html". To have no file included in the `<head>` section,
 *   explicitly pass `null` as the value for [includedHeadTagsPathJava].
 * @param includedHeadTagsPathKotlin Equivalent to [includedHeadTagsPathJava], but for Kotlin docs.
 * @param packagePrefixToRemoveInToc A string to trim from the beginning of each package name in the
 *   table of contents (for instance, if [packagePrefixToRemoveInToc] were set to `com.google`, the
 *   packages `com.google.a` and `com.google.b` would appear in the TOC as `a` and `b`). If the
 *   provided value does not end in `.`, the `.` is still included in the string to trim. This only
 *   applies to the table of contents, not other pages (e.g. the package index and the summary page
 *   for each package). Optional, defaults to `null` (no string trimmed from package names).
 * @param baseSourceLink Used to generate links to the source for each class page. This is a format
 *   string with placeholders for the filepath and (optionally) the qualified name of the class
 *   (`https://cs.android.com/search?q=file:%s+class:%s` is the [baseSourceLink] for AndroidX).
 *   Optional, if not specified, no source links are generated.
 * @param baseFunctionSourceLink Like [baseSourceLink], but used to generate links to the source for
 *   top-level and companion functions. This is a format string with placeholders for the filepath
 *   and function name (`https://cs.android.com/search?q=file:%s+function:%s` is the
 *   [baseFunctionSourceLink] for AndroidX). Property accessors will be linked based on their
 *   property with [basePropertySourceLink], as synthetic accessors don't exist in source. Optional,
 *   if not specified, no source links are generated for functions.
 * @param basePropertySourceLink Like [baseSourceLink], but used to generate links to the source for
 *   top-level and companion properties. This is a format string with placeholders for the filepath
 *   and property name `https://cs.android.com/search?q=file:%s+symbol:%s` is the
 *   [basePropertySourceLink] for AndroidX). Optional, if not specified, no source links are
 *   generated for properties.
 * @param annotationsNotToDisplay A list of annotation names (including the package name, e.g.
 *   `java.lang.Override`) which should not be displayed in the docs. Optional, if unspecified
 *   defaults to an empty list. Note that nullability annotations are handled separately.
 * @param annotationsNotToDisplayJava A list of annotation names which should not be displayed in
 *   the Java docs, in addition to those in [annotationsNotToDisplay]. Optional, if unspecified
 *   defaults to an empty list.
 * @param annotationsNotToDisplayKotlin Equivalent to [annotationsNotToDisplayJava] for Kotlin docs.
 * @param hidingAnnotations A list of annotation names (including the package name, e.g.
 *   `androidx.annotation.RestrictTo`) which mean that the elements they are applied to should be
 *   not be displayed in the docs.
 * @param validNullabilityAnnotations A list of annotation names (including the package name, e.g.
 *   `androidx.annotation.Nullable`) which can be used for nullability. Using nullability
 *   annotations not on this list is not allowed. Android projects should generally exclusively use
 *   the `androidx` nullability annotations, which are the default.
 * @param includeHiddenParentSymbols Whether to document methods/fields of hidden parent classes as
 *   though they were defined on the visible child class. Defaults to false (not documenting these
 *   symbols), which matches the behavior of the AndroidX RestrictTo lint check. Documenting the
 *   symbols is closer to the behavior of Metalava. (If a hidden parent symbol is overridden on a
 *   visible child class, then it will be included regardless.)
 * @param propagatingAnnotations A list of fully qualified annotation names that should propagate
 *   from elements to their children, e.g. from classes to nested classes, classes to functions, and
 *   properties to accessors (annotations are not propagated from functions to parameters). For KMP,
 *   annotations are propagated from the common source set to all source sets. By default, this is
 *   the deprecation annotations.
 */
data class DevsiteConfiguration(
    val docRootPath: String = "reference",
    val javaDocsPath: String?,
    val kotlinDocsPath: String?,
    val projectPath: String,
    val excludedPackages: List<String>?,
    val excludedPackagesForJava: List<String>?,
    val excludedPackagesForKotlin: List<String>?,
    val libraryMetadataFilename: String?,
    val versionMetadataFilenames: List<String>?,
    val includedHeadTagsPathJava: String? = "_shared/_reference-head-tags.html",
    val includedHeadTagsPathKotlin: String? = "_shared/_reference-head-tags.html",
    val packagePrefixToRemoveInToc: String?,
    val baseSourceLink: String?,
    val baseFunctionSourceLink: String?,
    val basePropertySourceLink: String?,
    val annotationsNotToDisplay: List<String>?,
    val annotationsNotToDisplayJava: List<String>?,
    val annotationsNotToDisplayKotlin: List<String>?,
    val hidingAnnotations: List<String> = emptyList(),
    // We set a default to the nullability annotations all android projects should use
    val validNullabilityAnnotations: List<String> = defaultValidNullabilityAnnotations,
    val includeHiddenParentSymbols: Boolean = false,
    val propagatingAnnotations: List<String> = listOf("kotlin.Deprecated", "java.lang.Deprecated"),
) : ConfigurableBlock {
    init {
        if (javaDocsPath == null && kotlinDocsPath == null) {
            throw IllegalStateException(
                "Invalid Dackka configuration: at least one of `javaDocsPath` and " +
                    "`kotlinDocsPath` must be specified as non-null",
            )
        } else if (javaDocsPath == kotlinDocsPath) {
            throw IllegalStateException(
                "Invalid Dackka configuration: `javaDocsPath` and `kotlinDocsPath` cannot have " +
                    "the same value.",
            )
        }
    }

    // Parse provided excluded package lists to regex sets
    private val computedExcludedPackagesForBoth: Set<Regex> by lazy {
        excludedPackages?.map { it.toRegex() }?.toSet() ?: emptySet()
    }

    val computedExcludedPackagesForJava: Set<Regex> by lazy {
        computedExcludedPackagesForBoth +
            (excludedPackagesForJava?.map { it.toRegex() }?.toSet() ?: emptySet())
    }

    val computedExcludedPackagesForKotlin: Set<Regex> by lazy {
        computedExcludedPackagesForBoth +
            (excludedPackagesForKotlin?.map { it.toRegex() }?.toSet() ?: emptySet())
    }

    val computedExcludedPackagesFor =
        mapOf(
            Language.JAVA to computedExcludedPackagesForJava,
            Language.KOTLIN to computedExcludedPackagesForKotlin,
        )

    // Computed sets for all annotations not to display for each language.
    private val annotationsNotToDisplayAsSet: Set<String> by lazy {
        annotationsNotToDisplay?.toSet() ?: emptySet()
    }

    val allAnnotationsNotToDisplayJava: Set<String> by lazy {
        annotationsNotToDisplayAsSet + (annotationsNotToDisplayJava?.toSet() ?: emptyList())
    }

    val allAnnotationsNotToDisplayKotlin: Set<String> by lazy {
        annotationsNotToDisplayAsSet + (annotationsNotToDisplayKotlin?.toSet() ?: emptyList())
    }
}

/** The `android.` annotations should be used in the android platform itself only--nowhere else. */
val defaultValidNullabilityAnnotations =
    listOf(
        "androidx.annotation.Nullable",
        "android.annotation.Nullable",
        "androidx.annotation.NonNull",
        "android.annotation.NonNull",
        "org.jspecify.annotations.NonNull",
        "org.jspecify.annotations.Nullable",
    )
