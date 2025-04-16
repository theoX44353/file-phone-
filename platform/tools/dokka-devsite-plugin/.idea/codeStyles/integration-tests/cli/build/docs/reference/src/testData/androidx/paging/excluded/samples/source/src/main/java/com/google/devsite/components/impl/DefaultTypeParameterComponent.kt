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
import com.google.devsite.components.render
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.joinMaybePrefix
import com.google.devsite.renderer.Language
import kotlinx.html.Entities.nbsp
import kotlinx.html.FlowContent

/** Default implementation of a function or class type parameter. */
internal data class DefaultTypeParameterComponent(
    override val data: TypeParameterComponent.Params,
) : TypeParameterComponent {
    init {
        validate()
    }

    override fun render(into: FlowContent) = render(into, true)

    /**
     * When rendering a single type param, e.g. in the left column of the parameters table, wrap <>s
     * When rendering a list of type params, group all within a single <>. Handled in List.render()
     */
    override fun render(into: FlowContent, angleBrackets: Boolean) =
        into.run {
            if (angleBrackets) {
                +"<"
            }
            data.annotationComponents.render(
                into,
                ShouldBreak.NO,
                separator = "",
                terminator = { +nbsp },
            )
            when (data.displayLanguage) {
                Language.JAVA -> {
                    +data.name
                    // TODO: handle "implements"
                    when (data.projections.size) {
                        0 -> {
                            /* do nothing */
                        }
                        1 -> {
                            +nbsp
                            +"extends"
                            +nbsp
                            data.projections.single().render(into)
                        }
                        // Temporary solution, because some rewriting would be necessary to hoist
                        // the
                        // projection information, so it can go at the end of the function signature
                        else -> {
                            +nbsp
                            +"extends"
                            +nbsp
                            for (projection in data.projections) {
                                projection.render(into)
                                if (projection !== data.projections.last()) {
                                    +nbsp
                                    +"&"
                                    +nbsp
                                }
                            }
                        }
                    }
                }
                Language.KOTLIN -> {
                    +data.name
                    data.modifiers.render(into)
                    // TODO: handle in/out
                    when (data.projections.size) {
                        0 -> {
                            /* do nothing */
                        }
                        1 -> {
                            +nbsp
                            +":"
                            +nbsp
                            data.projections.single().render(into)
                        }
                        // Temporary solution, because some rewriting would be necessary to hoist
                        // the
                        // projection information, so it can go at the end of the function signature
                        else -> {
                            +nbsp
                            +":"
                            +nbsp
                            for (projection in data.projections) {
                                projection.render(into)
                                if (projection !== data.projections.last()) {
                                    +nbsp
                                    +"&"
                                    +nbsp
                                }
                            }
                        }
                    }
                }
            }
            if (angleBrackets) {
                +">"
            }
        }

    override fun toString() = toString(true)

    fun toString(showAngleBrackets: Boolean) =
        (if (showAngleBrackets) "<" else "") +
            data.annotationComponents.joinMaybePrefix { it.toString() } +
            data.modifiers.joinMaybePrefix(postfix = " ") +
            (if (data.projections.isNotEmpty()) {
                ((if (data.displayLanguage == Language.KOTLIN) ":" else "extends") +
                    data.projections.joinMaybePrefix(separator = " & ") { it.toString() })
            } else {
                ""
            }) +
            (if (showAngleBrackets) ">" else "")

    override fun validate() {
        require(data.name.isNotEmpty()) { "Type parameters must have names." }
    }
}
