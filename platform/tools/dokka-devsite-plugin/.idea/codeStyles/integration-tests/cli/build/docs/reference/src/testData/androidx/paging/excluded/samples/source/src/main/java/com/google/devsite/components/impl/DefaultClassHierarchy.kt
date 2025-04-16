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

import com.google.devsite.components.table.ClassHierarchy
import kotlin.math.max
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr

/** Default implementation of a class hierarchy. */
internal data class DefaultClassHierarchy(
    override val data: ClassHierarchy.Params,
) : ClassHierarchy {
    override fun render(into: FlowContent) =
        into.run {
            if (data.parents.isEmpty()) return

            div("devsite-table-wrapper") {
                table("jd-inheritance-table") {
                    tbody {
                        for ((level, parent) in data.parents.withIndex()) {
                            tr {
                                repeat(max(0, level - 1)) { td { +Entities.nbsp } }

                                if (level != 0) {
                                    td(classes = "jd-inheritance-space") {
                                        repeat(3) { +Entities.nbsp }
                                        +"↳"
                                    }
                                }

                                td {
                                    attributes["colspan"] = (data.parents.size - level).toString()

                                    parent.render(this)
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun toString() =
        data.parents.withIndex().reversed().joinToString { (level, parent) -> "$level: $parent" }
}
