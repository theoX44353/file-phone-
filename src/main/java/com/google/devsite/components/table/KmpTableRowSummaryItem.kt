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

package com.google.devsite.components.table

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.devsiteFilter
import com.google.devsite.components.symbols.PlatformComponent
import kotlinx.html.COL
import kotlinx.html.DIV
import kotlinx.html.TABLE
import kotlinx.html.attributesMapOf
import kotlinx.html.col
import kotlinx.html.colGroup
import kotlinx.html.table
import kotlinx.html.visit

/** Builds a three-pane layout item with a Platform indicator for DevsiteSelector in column 3. */
internal interface KmpTableRowSummaryItem<T : ContextFreeComponent?, V : ContextFreeComponent> :
    TableRowSummaryItem<T, V> {
    override val data: Params<T, V>

    data class Params<T : ContextFreeComponent?, V : ContextFreeComponent>(
        override val title: T,
        override val description: V,
        val platforms: PlatformComponent,
    ) : TableRowSummaryItem.Params<T, V>(title, description)

    private val nColumns
        get() = listOfNotNull(data.title, data.description, data.platforms).size

    override fun layout(into: DIV, contents: TABLE.() -> Unit) =
        into.run {
            when (nColumns) {
                3 ->
                    devsiteFilter {
                        into.table("fixed") {
                            colGroup {
                                COL(attributesMapOf("width", "35%"), consumer).visit {}
                                COL(attributesMapOf("width", "58%"), consumer).visit {}
                                col() // Last col should be floating-width. (This method can't set
                                // "width".)
                            }
                            contents()
                        }
                    }
                2 ->
                    devsiteFilter { // This happens for KMP constructors
                        into.table("fixed") {
                            colGroup {
                                COL(attributesMapOf("width", "93%"), consumer).visit {}
                                col() // Last col should be floating-width. (This method can't set
                                // "width".)
                            }
                            contents()
                        }
                    }
                else ->
                    throw RuntimeException("tried to create KmpSummaryList with $nColumns columns!")
            }
        }
    }
