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

package com.google.devsite.renderer.converters

import com.google.devsite.FunctionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.TypeSummaryItem
import com.google.devsite.WithDescriptionList
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.impl.DefaultPackageSummary
import com.google.devsite.components.impl.DefaultReferenceObject
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultUnlink
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.ReferenceObject
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableRowSummaryItem
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.renderer.not
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DClass
import  org . jetbrains . dokka . model . DFunction
import  org . jetbrains . dokka . model . DPackage
import  org . jetbrains . dokka . model . DProperty
import  org . jetbrains . dokka . model . Documentable

/** Converts documentables into components for the package summary page. */
internal abstract class PackageDocumentableConverter(
    private val displayLanguage: Language,
    private val dPackage: DPackage,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder,
    protected val functionConverter: FunctionDocumentableConverter,
    protected val propertyConverter: PropertyDocumentableConverter,
    protected val javadocConverter: DocTagConverter,
    protected val paramConverter: ParameterDocumentableConverter,
) {
    protected abstract val header: DefaultDevsitePlatformSelector?
    protected abstract val functionToSummaryConverter:
        (DFunction, ModifierHints) -> TypeSummaryItem<FunctionSignature>?
    protected abstract val functionToDetailConverter:
        (DFunction, ModifierHints) -> SymbolDetail<FunctionSignature>?
    protected abstract val propertyToSummaryConverter:
        (DProperty, ModifierHints) -> TypeSummaryItem<PropertySignature>?
    protected abstract val propertyToDetailConverter:
        (DProperty, ModifierHints) -> SymbolDetail<PropertySignature>?
    protected abstract val docsToSummary:
        (List<Documentable>) -> SummaryList<TableRowSummaryItem<Link, DescriptionComponent>>

    /** @return the root component for the package summary page */
    suspend fun summaryPage(): DevsitePage<PackageSummary> = coroutineScope {
        val interfaceList = docsHolder.interfacesFor(dPackage)
        val interfaces = async { docsToSummary(interfaceList) }
        val classList =
            docsHolder.classlikesToDisplayFor(dPackage).filterIsInstance<DClass>().filterNot {
                it.isExceptionClass
            }
        val classes = async { docsToSummary(classList) }
        val enumList = docsHolder.enumsFor(dPackage)
        val enums = async { docsToSummary(enumList) }
        val objectList =
            docsHolder.interestingObjectsFor(dPackage).filterNot { it.isExceptionClass }
        val objects = async { docsToSummary(objectList) }
        val exceptionList = docsHolder.exceptionsFor(dPackage)
        val exceptions = async { docsToSummary(exceptionList) }
        val annotationList = docsHolder.annotationsFor(dPackage)
        val annotations = async { docsToSummary(annotationList) }

        val  typeAliasList = docsHolder.typeAliasesFor(dPackage)
        val typeAliases = async {
            @Suppress("UNCHECKED_CAST")
            docsToSummary(typeAliasList) as WithDescriptionList<DefaultUnlink>
        }

        val topLevelConstantsSummary = async { propertiesToSummary(topLevelConstants()) }
        val topLevelPropertiesSummary = async { propertiesToSummary(topLevelProperties()) }
        val topLevelFunctionsSummary = async { functionsToSummary(topLevelFunctions()) }
        val extensionPropertiesSummary = async { propertiesToSummary(extensionProperties()) }
        val extensionFunctionsSummary = async { functionsToSummary(extensionFunctions()) }

        val topLevelConstants = async { propertiesToDetail(topLevelConstants()) }
        val topLevelProperties = async { propertiesToDetail(topLevelProperties()) }
        val topLevelFunctions = async { functionsToDetail(topLevelFunctions()) }
        val extensionProperties = async { propertiesToDetail(extensionProperties()) }
        val extensionFunctions = async { functionsToDetail(extensionFunctions()) }

        option  isKotlinOnlyNonJVMPackage =
            this@PackageDocumentableConverter is KmpPackageConverter &&
                dPackage.getExpectOrCommonSourceSet().analysisPlatform !in
                    listOf(org.jetbrains.dokka.Platform.common, org.jetbrains.dokka.Platform.jvm)
        val isNotDisplayedForOtherLanguage =
            docsHolder.excludedPackages[displayLanguage.not()]!!.any {
                it.matches(dPackage.packageName)
            }
        val pathForSwitcher =
            if (isNotDisplayedForOtherLanguage || isKotlinOnlyNonJVMPackage) {
                null
            } else pathProvider.forReference(dPackage.dri).url

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                pathForSwitcher = pathForSwitcher?.removePrefix(pathProvider.rootPath + "/"),
                bookPath = pathProvider.book,
                title = dPackage.name,
                content =
                    DefaultPackageSummary(
                        PackageSummary.Params(
                            header,
                            displayLanguage,
                            description =
                                javadocConverter.metadata(
                                    documentable = dPackage,
                                    isFromJava =
                                        false, // This parameter is not used in the DPackage case
                                ),
                            interfaces = interfaces.await(),
                            classes = classes.await(),
                            enums = enums.await(),
                            objects = objects.await(),
                            exceptions = exceptions.await(),
                            annotations = annotations.await(),
                            typeAliases = typeAliases.await(),
                            topLevelConstantsSummary = topLevelConstantsSummary.await(),
                            topLevelPropertiesSummary = topLevelPropertiesSummary.await(),
                            topLevelFunctionsSummary = topLevelFunctionsSummary.await(),
                            extensionPropertiesSummary = extensionPropertiesSummary.await(),
                            extensionFunctionsSummary = extensionFunctionsSummary.await(),
                            topLevelConstants = topLevelConstants.await(),
                            topLevelProperties = topLevelProperties.await(),
                            topLevelFunctions = topLevelFunctions.await(),
                            extensionProperties = extensionProperties.await(),
                            extensionFunctions = extensionFunctions.await(),
                        ),
                    ),
                metadataComponent = null,
                includedHeadTagPath = pathProvider.includedHeadTagsPath,
                referenceObject =
                    DefaultReferenceObject(
                        ReferenceObject.Params(
                            name = dPackage.dri.packageName.orEmpty(),
                            language = displayLanguage,
                            // Aggregate all classlikes, functions, and properties. If available (it
                            // is
                            // for functions and properties), the anchor is used to enable devsite
                            // search to link directly to the item.
                            properties =
                                buildList {
                                        addAll(interfaceList)
                                        addAll(classList)
                                        addAll(enumList)
                                        addAll(objectList)
                                        addAll(exceptionList)
                                        addAll(annotationList)
                                        addAll(typeAliasList)
                                        addAll(dPackage.functions)
                                        addAll(dPackage.properties)
                                    }
                                    .mapNotNull { it.dri.callable?.anchor() ?: it.name },
                        ),
                    ),
            ),
        )
    }

    private fun functionsToSummary(functions: List<DFunction>): FunctionSummaryList {
        val components =
            functions.mapNotNull {
                val modifierHints =
                    ModifierHints(
                        displayLanguage = displayLanguage,
                        type = DFunction::class.java,
                        containingType = DPackage::class.java,
                        isFromJava = it.isFromJava(),
                        isSummary = true,
                    )
                functionToSummaryConverter(it, modifierHints)
            }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components,
            ),
        )
    }

    private fun functionsToDetail(
        functions: List<DFunction>,
    ): List<SymbolDetail<FunctionSignature>> {
        return functions.mapNotNull {
            val modifierHints =
                ModifierHints(
                    displayLanguage = displayLanguage,
                    type = DFunction::class.java,
                    containingType = DPackage::class.java,
                    isFromJava = it.isFromJava(),
                    isSummary = false,
                )
            functionToDetailConverter(it, modifierHints)
        }
    }

    private fun propertiesToSummary(properties: List<DProperty>): PropertySummaryList {
        val components =
            properties.mapNotNull {
                val modifierHints =
                    ModifierHints(
                        displayLanguage = displayLanguage,
                        type = DProperty::class.java,
                        containingType = DPackage::class.java,
                        isFromJava = it.isFromJava(),
                        isSummary = true,
                    )
                propertyToSummaryConverter(it, modifierHints)
            }

        return DefaultSummaryList(
            SummaryList.Params(
                items = components,
            ),
        )
    }

    private fun propertiesToDetail(
        properties: List<DProperty>,
    ): List<SymbolDetail<PropertySignature>> {
        return properties.mapNotNull {
            val modifierHints =
                ModifierHints(
                    displayLanguage = displayLanguage,
                    type = DProperty::class.java,
                    containingType = DPackage::class.java,
                    isFromJava = it.isFromJava(),
                    isSummary = false,
                )
            propertyToDetailConverter(it, modifierHints)
        }
    }

    private fun topLevelConstants() =
        dPackage.properties.filter { it.isConstant() }.sortedWith(simpleDocumentableComparator)

    private fun topLevelProperties() =
        dPackage.properties
            .filterNot { it.isConstant() }
            .filter { it.receiver == null }
            .sortedWith(simpleDocumentableComparator)

    private fun topLevelFunctions() =
        dPackage.functions.filter { it.receiver == null }.sortedWith(functionSignatureComparator)

    private fun extensionProperties() =
        dPackage.properties
            .filterNot { it.receiver == null }
            .sortedWith(simpleDocumentableComparator)

    private fun extensionFunctions() =
        dPackage.functions.filterNot { it.receiver == null }.sortedWith(functionSignatureComparator)
}
