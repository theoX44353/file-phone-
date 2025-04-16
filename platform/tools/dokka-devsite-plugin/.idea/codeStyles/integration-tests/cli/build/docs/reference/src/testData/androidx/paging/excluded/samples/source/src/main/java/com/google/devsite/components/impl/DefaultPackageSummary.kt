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

import com.google.devsite.FunctionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.WithDescriptionList
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.components.render
import com.google.devsite.renderer.Language
import kotlinx.html.FlowContent
import kotlinx.html.h2

/** Default implementation of the package summary page. */
internal data class DefaultPackageSummary(
    override val data: PackageSummary.Params,
) : PackageSummary {
    override fun render(into: FlowContent) =
        into.run {
            data.header?.render(into)
            data.description.render(into, separator = "")
            // The reason for checking display language here is to match the ordering of the page
            // sections of existing docs.
            when (data.displayLanguage) {
                Language.JAVA -> {
                    renderSummary(data.annotations, "Annotations")
                    renderSummary(data.interfaces, "Interfaces")
                    renderSummary(data.classes + data.objects, "Classes")
                    renderSummary(data.enums, "Enums")
                    renderSummary(data.exceptions, "Exceptions")
                }
                Language.KOTLIN -> {
                    renderSummary(data.interfaces, "Interfaces")
                    renderSummary(data.classes, "Classes")
                    renderSummary(data.exceptions, "Exceptions")
                    renderSummary(data.objects, "Objects")
                    renderSummary(data.annotations, "Annotations")
                    renderSummary(data.enums, "Enums")
                }
            }

            if (data.displayLanguage == Language.KOTLIN) {
                renderSummary(data.typeAliases, "Type aliases")

                renderSummary(data.topLevelConstantsSummary, "Constants summary")
                renderSummary(data.topLevelFunctionsSummary, "Top-level functions summary")
                renderSummary(data.extensionFunctionsSummary, "Extension functions summary")
                renderSummary(data.topLevelPropertiesSummary, "Top-level properties summary")
                renderSummary(data.extensionPropertiesSummary, "Extension properties summary")

                renderDetails(data.topLevelConstants, "Constants")
                renderDetails(data.topLevelFunctions, "Top-level functions")
                renderDetails(data.extensionFunctions, "Extension functions")
                renderDetails(data.topLevelProperties, "Top-level properties")
                renderDetails(data.extensionProperties, "Extension properties")
            }
        }

    private fun <T : ContextFreeComponent> FlowContent.renderSummary(
        summary: WithDescriptionList<T>,
        title: String,
    ) {
        if (summary.hasContent()) {
            h2 { +title }
        }
        summary.render(this)
    }

    @JvmName("renderPropertiesSummarySectionSummary")
    private fun FlowContent.renderSummary(summary: PropertySummaryList, title: String) {
        if (summary.hasContent()) {
            h2 { +title }
        }
        summary.render(this)
    }

    @JvmName("renderFunctionsSummarySectionSummary")
    private fun FlowContent.renderSummary(summary: FunctionSummaryList, title: String) {
        if (summary.hasContent()) {
            h2 { +title }
        }
        summary.render(this)
    }

    private fun FlowContent.renderDetails(details: List<ContextFreeComponent>, title: String) {
        details.render(this, separator = null, header = { h2 { +title } })
    }
}
