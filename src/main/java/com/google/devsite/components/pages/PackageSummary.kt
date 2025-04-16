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

package com.google.devsite.components.pages

import com.google.devsite.FunctionSummaryList
import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.WithDescriptionList
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.impl.DefaultUnlink
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.renderer.Language

/** Represents the package summary page. */
internal interface PackageSummary : ContextFreeComponent {
    val data: Params

    data class Params(
        val header: DefaultDevsitePlatformSelector?,
        val displayLanguage: Language,
        val description: List<ContextFreeComponent>,
        val interfaces: LinkDescriptionSummaryList,
        val classes: LinkDescriptionSummaryList,
        val enums: LinkDescriptionSummaryList,
        val objects: LinkDescriptionSummaryList,
        val exceptions: LinkDescriptionSummaryList,
        val annotations: LinkDescriptionSummaryList,
        val typeAliases: WithDescriptionList<DefaultUnlink>,
        val topLevelConstantsSummary: PropertySummaryList,
        val topLevelPropertiesSummary: PropertySummaryList,
        val topLevelFunctionsSummary: FunctionSummaryList,
        val extensionPropertiesSummary: PropertySummaryList,
        val extensionFunctionsSummary: FunctionSummaryList,
        val topLevelConstants: List<SymbolDetail<PropertySignature>>,
        val topLevelProperties: List<SymbolDetail<PropertySignature>>,
        val topLevelFunctions: List<SymbolDetail<FunctionSignature>>,
        val extensionProperties: List<SymbolDetail<PropertySignature>>,
        val extensionFunctions: List<SymbolDetail<FunctionSignature>>,
    )
}
