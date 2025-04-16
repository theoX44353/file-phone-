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

import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.render
import com.google.devsite.components.symbols.KmpSymbolDetail
import com.google.devsite.components.symbols.devsiteId
import com.google.devsite.joinMaybePrefix
import kotlinx.html.FlowContent
import kotlinx.html.H2
import kotlinx.html.attributesMapOf
import kotlinx.html.h2
import kotlinx.html.visit

/** Default implementation of class-like pages. */
internal data class DefaultClasslike(
    override val data: Classlike.Params,
) : Classlike {
    override fun render(into: FlowContent) =
        into.run {
            data.description.render(this)
            // The ordering logic for these summaries is in Classlike.kt
            allVisibleSummaries.render(into, separator = null, header = { h2 { +"Summary" } })

            for (symbolType in allDetailsSections.filter { it.symbols.isNotEmpty() }) {
                // Delegate choices of table layout (e.g. filters available) to the first item.
                // This is the same method we use for layout of the summary section tables.
                // We know that all items will want the same thing, because they will all be Ts
                symbolType.symbols.first().layout(this) {
                    symbolType.symbols.render(
                        into,
                        separator = null,
                        header =
                            if (symbolType.symbols.first() is KmpSymbolDetail) {
                                { ->
                                    val unionPlatformsString =
                                        symbolType.symbols
                                            // platforms of each detail
                                            .map {
                                                (it as KmpSymbolDetail)
                                                    .data
                                                    .platforms
                                                    .data
                                                    .platforms
                                            }
                                            // all platforms of any detail
                                            .reduce { acc, next -> acc.union(next) }
                                            .joinToString { it.devsiteId() }
                                    H2(
                                            attributesMapOf("data-title", unionPlatformsString),
                                            consumer,
                                        )
                                        .visit {
                                            +symbolType.title
                                            comment(unionPlatformsString)
                                        }
                                }
                            } else {
                                { h2 { +symbolType.title } }
                            },
                    )
                }
            }
        }

    override fun toString() =
        data.description.toString() +
            inheritedSummarySections.filter { it.hasContent() } +
            allSummarySections.filter { it.hasContent() }.joinMaybePrefix(prefix = "Summaries") +
            allDetailsSections
                .filter { it.symbols.isNotEmpty() }
                .joinMaybePrefix(prefix = "Details")
}
