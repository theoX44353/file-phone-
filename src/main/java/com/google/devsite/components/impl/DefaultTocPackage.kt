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

package com.google.devsite.components.impl

import com.google.devsite.components.symbols.TocPackage

/** Default implementation of the toc. */
internal data class DefaultTocPackage(
    override val data: TocPackage.Params,
) : TocPackage {
    override fun render(into: StringBuilder) =
        into.run {
            appendLine("- title: \"${data.name}\"")
            appendLine("  path: \"${data.packageUrl}\"")

            val content =
                mapOf(
                        "Interfaces" to data.interfaces,
                        "Classes" to data.classes,
                        "Enums" to data.enums,
                        "Exceptions" to data.exceptions,
                        "Annotations" to data.annotations,
                        "Objects" to data.objects,
                    )
                    .filter { (_, contents) -> contents.isNotEmpty() }
            if (content.isEmpty()) return@run

            appendLine()
            appendLine("  section:")

            content.onEachIndexed { i, (name, contents) ->
                if (i != 0) {
                    appendLine()
                }
                renderTypes(name, contents)
            }
        }

    private fun StringBuilder.renderTypes(sectionName: String, types: List<TocPackage.Type>) {
        if (types.isEmpty()) return

        appendLine("  - title: \"$sectionName\"")
        appendLine()
        appendLine("    section:")

        for (type in types) {
            renderType(type)
        }
    }

    private fun StringBuilder.renderType(type: TocPackage.Type) {
        appendLine("    - title: \"${type.name}\"")
        appendLine("      path: \"${type.url}\"")
    }

    override fun toString() =
        "Table of Contents for package ${data.name} at ${data.packageUrl}. " +
            "Interfaces: ${data.interfaces}, Classes: ${data.classes}, Enums: ${data.enums}, " +
            "Exceptions: ${data.exceptions}, Annotations: ${data.annotations}, " +
            "Objects: ${data.objects}."
}
