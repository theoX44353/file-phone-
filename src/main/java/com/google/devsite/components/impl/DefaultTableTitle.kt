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

import com.google.devsite.components.table.TableTitle
import kotlinx.html.TR
import kotlinx.html.th
import kotlinx.html.unsafe

/** Default implementation of a table header. */
internal data class DefaultTableTitle(
    override val data: TableTitle.Params,
) : TableTitle {
    override fun render(into: TR) =
        into.run {
            th {
                attributes["colspan"] = "100%"

                if (data.big) {
                    // TODO(b/164125463): th isn't flow content :(
                    unsafe { +"<h3>" }
                    +data.title
                    unsafe { +"</h3>" }
                } else {
                    +data.title
                }
            }
        }

    override fun toString() = data.title
}
