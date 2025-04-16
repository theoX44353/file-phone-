/*
 * Copyright 2020 The Android Open Source Project
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

package com.google.devsite.renderer.impl.paths

import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.anchor
import com.google.devsite.renderer.converters.companion
import com.google.devsite.renderer.converters.isHoistedFromCompanion
import com.google.devsite.renderer.impl.DocumentablesGraph
import java.nio.file.Paths
import kotlin.io.path.pathString
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.Documentable

private val NON_DOCUMENTABLE_PREFIXES =
    listOf(
        "kotlin.jvm.functions",
        "kotlin.coroutines.SuspendFunction",
    )

/** Converts various inputs to output file paths. */
internal interface FilePathProvider {

    /** The language the docs are displayed in, which impacts some paths */
    val language: Language

    /** The DokkaLocationProvider that is used to provide locations of external documentation */
    val locationProvider: ExternalDokkaLocationProvider?

    /** The raw list of packages in plain text format. */
    val packageList: String

    /** The HTML list of packages for human consumption. */
    val packages: String

    /** The HTML list of classes for human consumption. */
    val classes: String

    /** The global index file that encompasses all packages. */
    val rootIndex: String

    /**
     * The _toc.yaml file, responsible for pointing to the paths of each index.html file in the doc
     * tree.
     */
    val toc: String

    /** The _book.yaml file, responsible for the sidebar nav. */
    val book: String

    /** The path to a file to be included in the head section of each generated HTML page. */
    val includedHeadTagsPath: String?

    /** The prefix added to all paths. Sometimes must be removed from paths. */
    val rootPath: String

    val documentablesGraph: DocumentablesGraph

    /** @return the path of a class-like type */
    fun forType(packageName: String, name: String): String

    /**
     * @param name is used to override the computed name for @JvmName, generated getters, etc.
     * @param suffix is used in the case of links of a nullable type
     * @see forReference
     */
    fun linkForReference(dri: DRI, name: String? = null, suffix: String = ""): Link {
        val ref = forReference(dri)
        return DefaultLink(Link.Params((name ?: ref.name) + suffix, ref.url))
    }

    /**
     * Creates a deep link to a symbol or type. Links to packages, class-likes, top-level/extension
     * functions, and symbols within a type are supported.
     */
    fun forReference(dri: DRI): ReferencePath {
        val documentable = findInDocumentablesGraph(dri)
        val packageName = dri.packageName.orEmpty().ifBlank { "[JVM root]" }
        val className = dri.classNames
        val outerClassName = getOuterClassName(className)
        val innerClassName = documentable?.name
        val symbol = dri.callable
        val isInnerClassEnumEntry = documentable is DEnumEntry

        // Exclude specific references from being linked (even if DokkaLocationProvider might be
        // able to resolve them).
        // In the future we might want to only link to things we *know* we've generated docs for by
        // passing around a collection of valid locations but that could have performance
        // implications
        val fullName = "$packageName.$className"
        if (NON_DOCUMENTABLE_PREFIXES.any { fullName.startsWith(it) }) {
            return ReferencePath(className ?: packageName, "")
        }

        // if the DokkaLocationProvider can resolve the dri, then we accept that
        locationProvider?.resolve(dri)?.let {
            val text = symbol?.name ?: className ?: packageName
            return ReferencePath(text, it)
        }

        val (typeName, typeUrl) =
            if (className == null) {
                packageName to forType(packageName, PACKAGE_SUMMARY_NAME)
            } else {
                className to forType(packageName, className)
            }

        // if we have an enum value instead of an inner class, we need a link to the enum class
        // (Foo) without the value (Foo.ENUM) and append the enum value as a hash.
        // For example, /Foo#ENUM instead /Foo.ENUM
        if (isInnerClassEnumEntry && innerClassName != null && outerClassName != null) {
            val outerTypeUrl = forType(packageName, outerClassName)
            return ReferencePath(typeName, "$outerTypeUrl#$innerClassName")
        }

        // If this is the child of a companion object that is documented on the page of the
        // companion's containing class, link to the class page instead of the companion page.
        if (
            symbol != null &&
                outerClassName != null &&
                documentable != null &&
                isCompanion(dri.parent) &&
                documentable.isHoistedFromCompanion(language)
        ) {
            val outerTypeUrl = forType(packageName, outerClassName)
            return ReferencePath(symbol.name, "$outerTypeUrl#${symbol.anchor()}")
        }

        return if (symbol == null) {
            ReferencePath(typeName, typeUrl)
        } else {
            ReferencePath(symbol.name, "$typeUrl#${symbol.anchor()}")
        }
    }

    /** Returns a [Documentable] with the given [DRI] if it exists, else null. */
    fun findInDocumentablesGraph(dri: DRI): Documentable? {
        return documentablesGraph[dri]
    }

    /**
     * Checks if the [dri] represents a companion object based on if the parent is a class and has a
     * companion with an identical DRI.
     */
    fun isCompanion(dri: DRI): Boolean {
        val parentDocumentable = findInDocumentablesGraph(dri.parent)
        return parentDocumentable is DClasslike && parentDocumentable.companion()?.dri == dri
    }

    data class ReferencePath(val name: String, val url: String)

    /**
     * Removes the innermost class from a list if it exists
     *
     * Outer.Inner -> Outer A.B.C -> A.B MyClass -> MyClass
     */
    private fun getOuterClassName(className: String?): String? {
        val classNames = className?.split(".") ?: emptyList()
        return if (classNames.size > 1) {
            classNames.dropLast(1).joinToString(".")
        } else {
            className
        }
    }

    val ANY: TypeProjectionComponent

    companion object {
        fun joinPaths(pathComponent: String, vararg pathComponents: String): String =
            Paths.get(pathComponent, *pathComponents).pathString
    }
}

internal val ANY_DRI: Map<Language, DRI> =
    mapOf(
        Language.JAVA to DRI("java.lang", "Object"),
        Language.KOTLIN to DRI("kotlin", "Any"),
    )
    
