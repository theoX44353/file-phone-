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

import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.components.Link
import com.google.devsite.components.render
import com.google.devsite.components.table.RelatedSymbols
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.html.unsafe

/** Default implementation of related symbols. */
internal data class DefaultRelatedSymbols(
    override val data: RelatedSymbols.Params,
) : RelatedSymbols {
    override fun render(into: FlowContent) =
        into.run {
            renderRelatedSymbolsFor(data.directSubclasses, data.directSummary, "direct")
            renderRelatedSymbolsFor(data.indirectSubclasses, data.indirectSummary, "indirect")
        }

    private fun FlowContent.renderRelatedSymbolsFor(
        subclasses: List<Link>,
        summary: LinkDescriptionSummaryList,
        relatedness: String,
    ) {
        if (subclasses.isEmpty()) return

        div("devsite-table-wrapper") {
            unsafe { +"<devsite-expandable>" }

            span("expand-control jd-sumtable-subclasses") {
                +"Known $relatedness subclasses"

                div("showalways") {
                    attributes["id"] = "subclasses-$relatedness"
                    subclasses.render(this)
                }
            }

            div {
                attributes["id"] = "subclasses-$relatedness-summary"
                summary.render(this)
            }

            unsafe { +"</devsite-expandable>" }
        }
    }

    override fun toString() =
        "Direct subclasses: ${data.directSubclasses.joinToString()}, summar" +
            "y ${data.directSummary}. Indirect subclasses: ${data.indirectSubclasses.joinToString()}," +
            " summary ${data.indirectSummary}"
}
