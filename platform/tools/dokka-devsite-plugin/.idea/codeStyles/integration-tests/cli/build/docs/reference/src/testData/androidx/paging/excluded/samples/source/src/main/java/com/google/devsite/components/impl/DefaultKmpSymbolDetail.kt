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

import com.google.devsite.components.ShouldBreak
import com.google.devsite.components.devsiteFilter
import com.google.devsite.components.render
import com.google.devsite.components.symbols.KmpSymbolDetail
import com.google.devsite.components.symbols.SymbolDetail.SymbolKind
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.renderer.Language
import kotlinx.html.DIV
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h3
import kotlinx.html.pre

/** Default implementation of a fully documented function. */
internal data class DefaultKmpSymbolDetail<T : SymbolSignature>(
    override val data: KmpSymbolDetail.Params<T>,
) : KmpSymbolDetail<T> {
    override fun render(into: FlowContent) =
        into.div(classes = "api-item") {
            for (anchor in data.anchors.drop(1)) {
                a { attributes["name"] = anchor }
            }

            // CSS is declared in internal codebase (cl/553846444)
            div("api-name-block") {
                // Wrap the h3 element in a div in case devsite modifies h3 elements in the future.
                // This preserves the Flexbox spacing between h3 and the container for the platform
                // icons and the metadata component container.
                div {
                    h3 {
                        data.anchors.firstOrNull()?.let { attributes["id"] = it }
                        if (
                            data.displayLanguage == Language.JAVA && data.extFunctionClass != null
                        ) {
                            +data.extFunctionClass
                            +"."
                        }
                        +data.name
                    }
                }
                div("api-name-platform-and-metadata") {
                    div("api-name-platform-icons") { data.platforms.renderForDetail(into) }
                    data.metadataComponent?.render(this)
                }
            }
            pre("api-signature no-pretty-print") {
                data.annotationComponents.render(into, ShouldBreak.YES, separator = "")
                data.modifiers.render(this, terminator = { +Entities.nbsp })

                when (data.displayLanguage) {
                    Language.JAVA -> {
                        if (data.symbolKind != SymbolKind.CONSTRUCTOR) {
                            data.returnType.render(this)
                            +Entities.nbsp
                        }

                        data.signature.render(this)
                    }
                    Language.KOTLIN -> {
                        +data.symbolKind.keyword
                        if (data.symbolKind != SymbolKind.CONSTRUCTOR) +Entities.nbsp
                        data.signature.render(this)

                        if (data.symbolKind != SymbolKind.CONSTRUCTOR) {
                            +":"
                            +Entities.nbsp
                            data.returnType.render(this)
                        }
                    }
                }
            }

            data.metadata.sortedBy { descriptionSorter(it) }.render(this, separator = null)
        }

    override fun layout(into: FlowContent, contents: DIV.() -> Unit) =
        into.run { devsiteFilter { into.div(classes = "list") { contents() } } }

    override fun toString() =
        (data.extFunctionClass ?: "") +
            data.name +
            " at " +
            data.anchors.joinToString() +
            data.annotationComponents.joinToString() +
            data.modifiers.joinToString() +
            (if (data.displayLanguage == Language.KOTLIN) {
                "${data.returnType} : ${data.signature}"
            } else "" + data.signature + data.returnType) +
            data.metadata.sortedBy { descriptionSorter(it) }
}
