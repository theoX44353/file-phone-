/*
 * Copyright 2022 The Android Open Source Project
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

import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.impl.DefaultKmpClasslikeDescription
import com.google.devsite.components.impl.DefaultPlatformComponent
import com.google.devsite.components.symbols.ClasslikeDescription
import com.google.devsite.components.symbols.ClasslikeSignature
import com.google.devsite.components.symbols.KmpClasslikeDescription
import com.google.devsite.components.symbols.Platform
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.DClasslike

internal class NonKmpClasslikeConverter(
    displayLanguage: Language,
    classlike: DClasslike,
    pathProvider: FilePathProvider,
    docsHolder: DocumentablesHolder,
    functionConverter: FunctionDocumentableConverter,
    propertyConverter: PropertyDocumentableConverter,
    enumConverter: EnumValueDocumentableConverter,
    javadocConverter: DocTagConverter,
    paramConverter: ParameterDocumentableConverter,
    annotationConverter: AnnotationDocumentableConverter,
    metadataConverter: MetadataConverter,
) :
    ClasslikeDocumentableConverter(
        displayLanguage,
        classlike,
        pathProvider,
        docsHolder,
        functionConverter,
        propertyConverter,
        enumConverter,
        javadocConverter,
        paramConverter,
        annotationConverter,
        metadataConverter,
    ) {
    override val header: DefaultDevsitePlatformSelector? = null
    override val functionToSummaryConverter = functionConverter::summary
    override val functionToDetailConverter = functionConverter::detail
    override val propertyToSummaryConverter = propertyConverter::summary
    override val propertyToDetailConverter = propertyConverter::detail
    override val constructorToSummaryConverter = functionConverter::summaryForConstructor
    override val constructorToDetailConverter = functionConverter::detailForConstructor
}

internal class KmpClasslikeConverter(
    displayLanguage: Language,
    classlike: DClasslike,
    pathProvider: FilePathProvider,
    docsHolder: DocumentablesHolder,
    functionConverter: FunctionDocumentableConverter,
    propertyConverter: PropertyDocumentableConverter,
    enumConverter: EnumValueDocumentableConverter,
    javadocConverter: DocTagConverter,
    paramConverter: ParameterDocumentableConverter,
    annotationConverter: AnnotationDocumentableConverter,
    metadataConverter: MetadataConverter,
    platforms: List<Platform>,
) :
    ClasslikeDocumentableConverter(
        displayLanguage,
        classlike,
        pathProvider,
        docsHolder,
        functionConverter,
        propertyConverter,
        enumConverter,
        javadocConverter,
        paramConverter,
        annotationConverter,
        metadataConverter,
    ) {
    override val header = DefaultDevsitePlatformSelector(platforms)
    override val functionToSummaryConverter = functionConverter::summaryKmp
    override val functionToDetailConverter = functionConverter::detailKmp
    override val propertyToSummaryConverter = propertyConverter::summaryKmp
    override val propertyToDetailConverter = propertyConverter::detailKmp
    override val constructorToSummaryConverter = functionConverter::summaryForKmpConstructor
    override val constructorToDetailConverter = functionConverter::detailForKmpConstructor

    override suspend fun getClasslikeDescription(): ClasslikeDescription = coroutineScope {
        val signatures =
            ConcurrentHashMap<ClasslikeSignature, MutableSet<DokkaConfiguration.DokkaSourceSet>>()
        var primarySignature: ClasslikeSignature? = null
        val primarySourceSet = classlike.getExpectOrCommonSourceSet()

        coroutineScope {
            classlike.sourceSets.map { sourceSet ->
                launch {
                    // We must do this computation every time, because we don't know what will and
                    // what won't affect the signature until after we calculate it for each
                    // sourceSet. E.g. the JVM sourceSet might have an `@JvmName` but otherwise have
                    // the same signature.
                    // `@JvmName` doesn't affect displayed signature, so those should all be
                    // collapsed.
                    val sig =
                        computeSignature(
                            classlike = classlike,
                            classGraph = docsHolder.classGraph(),
                            sourceSet = sourceSet,
                        )
                    if (!signatures.containsKey(sig)) signatures[sig] = mutableSetOf()
                    signatures[sig]!!.add(sourceSet)
                    if (sourceSet == primarySourceSet) primarySignature = sig
                }
            }
        }

        val hierarchy = async { computeHierarchy() }
        val relatedSymbols = async { findRelatedSymbols() }
        DefaultKmpClasslikeDescription(
            KmpClasslikeDescription.Params(
                header = header,
                primarySignature = primarySignature!!,
                hierarchy = hierarchy.await(),
                relatedSymbols = relatedSymbols.await(),
                descriptionDocs = javadocConverter.metadata(classlike),
                platform = DefaultPlatformComponent(setOf(classlike.getExpectOrCommonSourceSet())),
                allSignatures =
                    signatures
                        .mapValues { (_, v) -> DefaultPlatformComponent(v) }
                        .toList()
                        .sortedBy { it.second.toString() },
            ),
        )
    }
