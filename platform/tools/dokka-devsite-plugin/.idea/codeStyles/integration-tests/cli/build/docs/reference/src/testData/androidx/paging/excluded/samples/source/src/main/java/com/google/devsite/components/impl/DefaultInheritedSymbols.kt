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

import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.table.InheritedSymbolsList
import com.google.devsite.components.table.TableTitle
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.unsafe

/** Default implementation of inherited symbols. */
internal data class DefaultInheritedSymbols<T : SymbolSignature>(
    override val data: InheritedSymbolsList.Params<T>,
) : InheritedSymbolsList<T> {
    override fun render(into: FlowContent) {
        if (data.inheritedSymbolSummaries.isEmpty()) return

        into.run {
            div("devsite-table-wrapper") {
                table("responsive") {
                    attributes["id"] = "inhmethods"
                    thead { tr { data.header.render(this) } }

                    tbody(classes = "list") {
                        for (item in data.inheritedSymbolSummaries) {
                            tr {
                                td {
                                    unsafe { +"<devsite-expandable>" }
                                    span("expand-control") {
                                        +"From "
                                        item.key.render(this)
                                    }

                                    item.value.render(this)
                                    unsafe { +"</devsite-expandable>" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun toString() =
        if (!hasContent()) {
            ""
        } else
            data.header.toString() +
                " " +
                data.inheritedSymbolSummaries
                    .map { (from, summaries) -> "from $from, inherited $summaries" }
                    .joinToString()
}

internal fun <T : SymbolSignature> emptyInheritedSymbolsList() =
    DefaultInheritedSymbols(
        InheritedSymbolsList.Params<T>(
            DefaultTableTitle(TableTitle.Params("")),
            emptyMap(),
        ),
    )
