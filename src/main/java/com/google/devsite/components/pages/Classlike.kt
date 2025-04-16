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

import com.google.devsite.ClasslikeSummaryList
import com.google.devsite.ConstructorSummaryList
import com.google.devsite.FunctionSummaryList
import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.components.Component
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.symbols.ClasslikeDescription
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.table.InheritedSymbolsList
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import kotlinx.html.FlowContent

/** Represents class-like pages (class, interface, exception, etc). */
internal interface Classlike : ContextFreeComponent {
    val data: Params

    data class Params(
        val displayLanguage: Language,
        val description: ClasslikeDescription,
        val nestedTypesSummary: ClasslikeSummaryList,
        val enumValuesSummary: LinkDescriptionSummaryList,
        val enumValuesDetails: TitledList<SymbolDetail<PropertySignature>>,
        val constantsSummary: PropertySummaryList,
        val constantsDetails: TitledList<SymbolDetail<PropertySignature>>,
        val publicCompanionFunctionsSummary: FunctionSummaryList,
        val publicCompanionFunctionsDetails: TitledList<SymbolDetail<FunctionSignature>>,
        val protectedCompanionFunctionsSummary: FunctionSummaryList,
        val protectedCompanionFunctionsDetails: TitledList<SymbolDetail<FunctionSignature>>,
        val publicCompanionPropertiesSummary: PropertySummaryList,
        val publicCompanionPropertiesDetails: TitledList<SymbolDetail<PropertySignature>>,
        val protectedCompanionPropertiesSummary: PropertySummaryList,
        val protectedCompanionPropertiesDetails: TitledList<SymbolDetail<PropertySignature>>,
        val publicPropertiesSummary: PropertySummaryList,
        val publicPropertiesDetails: TitledList<SymbolDetail<PropertySignature>>,
        val protectedPropertiesSummary: PropertySummaryList,
        val protectedPropertiesDetails: TitledList<SymbolDetail<PropertySignature>>,
        val publicFunctionsSummary: FunctionSummaryList,
        val publicFunctionsDetails: TitledList<SymbolDetail<FunctionSignature>>,
        val protectedFunctionsSummary: FunctionSummaryList,
        val protectedFunctionsDetails: TitledList<SymbolDetail<FunctionSignature>>,
        val publicConstructorsSummary: ConstructorSummaryList,
        val publicConstructorsDetails: TitledList<SymbolDetail<FunctionSignature>>,
        val protectedConstructorsSummary: ConstructorSummaryList,
        val protectedConstructorsDetails: TitledList<SymbolDetail<FunctionSignature>>,
        val extensionFunctionsSummary: FunctionSummaryList,
        val extensionFunctionsDetails: TitledList<SymbolDetail<FunctionSignature>>,
        val extensionPropertiesSummary: PropertySummaryList,
        val extensionPropertiesDetails: TitledList<SymbolDetail<PropertySignature>>,
        val inheritedConstants: InheritedSymbolsList<PropertySignature>,
        val inheritedFunctions: InheritedSymbolsList<FunctionSignature>,
        val inheritedProperties: InheritedSymbolsList<PropertySignature>,
    )

    data class TitledList<T : SymbolDetail<*>>(val title: String, val symbols: List<T>) : List<T> {
        override val size = symbols.size

        override fun contains(element: T) = symbols.contains(element)

        override fun containsAll(elements: Collection<T>) = symbols.containsAll(elements)

        override fun get(index: Int) = symbols[index]

        override fun indexOf(element: T) = symbols.indexOf(element)

        override fun isEmpty() = symbols.isEmpty()

        override fun iterator() = symbols.iterator()

        override fun lastIndexOf(element: T) = symbols.lastIndexOf(element)

        override fun listIterator() = symbols.listIterator()

        override fun listIterator(index: Int) = symbols.listIterator(index)

        override fun subList(fromIndex: Int, toIndex: Int) = symbols.subList(fromIndex, toIndex)
    }

    private val earlySummaries: List<SummaryList<*>>
        get() =
            listOfNotNull(
                data.nestedTypesSummary,
                data.enumValuesSummary,
                data.constantsSummary,
            )

    private val kotlinOnlySummaries: List<SummaryList<*>>
        get() =
            listOfNotNull(
                data.publicCompanionFunctionsSummary,
                data.protectedCompanionFunctionsSummary,
                data.publicCompanionPropertiesSummary,
                data.protectedCompanionPropertiesSummary,
            )

    private val functionSummaries: List<SummaryList<*>>
        get() =
            listOfNotNull(
                data.publicConstructorsSummary,
                data.protectedConstructorsSummary,
                data.publicFunctionsSummary,
                data.protectedFunctionsSummary,
            )

    private val propertiesSummaries: List<PropertySummaryList>
        get() =
            listOfNotNull(
                data.publicPropertiesSummary,
                data.protectedPropertiesSummary,
            )

    private val extensionFunctionsSummary: List<FunctionSummaryList>
        get() = listOf(data.extensionFunctionsSummary)

    private val extensionPropertiesSummary: List<PropertySummaryList>
        get() = listOf(data.extensionPropertiesSummary)

    val allSummarySections: List<SummaryList<*>>
        get() =
            earlySummaries +
                when (data.displayLanguage) {
                    Language.JAVA ->
                        propertiesSummaries + functionSummaries + extensionFunctionsSummary
                    Language.KOTLIN ->
                        kotlinOnlySummaries +
                            functionSummaries +
                            propertiesSummaries +
                            extensionFunctionsSummary +
                            extensionPropertiesSummary
                }

    val inheritedSummarySections: List<InheritedSymbolsList<*>>
        get() =
            when (data.displayLanguage) {
                Language.JAVA ->
                    listOfNotNull(
                        data.inheritedConstants,
                        data.inheritedProperties,
                        data.inheritedFunctions,
                    )
                Language.KOTLIN ->
                    listOfNotNull(
                        data.inheritedConstants,
                        data.inheritedFunctions,
                        data.inheritedProperties,
                    )
            }

    val allVisibleSummaries: List<Component<FlowContent>>
        get() =
            allSummarySections.filter { it.hasContent() } +
                inheritedSummarySections.filter { it.hasContent() }

    private val earlyDetails
        get() =
            listOfNotNull(
                // nested types has no Details section
                data.enumValuesDetails,
                data.constantsDetails,
            )

    private val kotlinOnlyDetails
        get() =
            listOfNotNull(
                data.publicCompanionFunctionsDetails,
                data.protectedCompanionFunctionsDetails,
                data.publicCompanionPropertiesDetails,
                data.protectedCompanionPropertiesDetails,
            )

    private val functionDetails
        get() =
            listOfNotNull(
                data.publicConstructorsDetails,
                data.protectedConstructorsDetails,
                data.publicFunctionsDetails,
                data.protectedFunctionsDetails,
            )

    private val propertiesDetails
        get() =
            listOfNotNull(
                data.publicPropertiesDetails,
                data.protectedPropertiesDetails,
            )

    private val extensionFunctionsDetails
        get() = listOf(data.extensionFunctionsDetails)

    private val extensionPropertiesDetails
        get() = listOf(data.extensionPropertiesDetails)

    val allDetailsSections: List<TitledList<out SymbolDetail<out SymbolSignature>>>
        get() =
            earlyDetails +
                when (data.displayLanguage) {
                    Language.JAVA -> propertiesDetails + functionDetails + extensionFunctionsDetails
                    Language.KOTLIN ->
                        kotlinOnlyDetails +
                            functionDetails +
                            propertiesDetails +
                            extensionFunctionsDetails +
                            extensionPropertiesDetails
                }
}

internal fun <U : SymbolSignature, T : SymbolDetail<U>> emptyTitledList() =
    Classlike.TitledList<T>("", emptyList())
