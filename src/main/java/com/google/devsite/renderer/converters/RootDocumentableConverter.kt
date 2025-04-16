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

import com.google.devsite.components.impl.DefaultClassIndex
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultPackageIndex
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableOfContents
import com.google.devsite.components.impl.DefaultTocPackage
import com.google.devsite.components.pages.ClassIndex
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.pages.PackageIndex
import com.google.devsite.components.pages.TableOfContents
import com.google.devsite.components.symbols.TocPackage
import com.google.devsite.components.table.SummaryList
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.model.DClass
import  org . jetbrains . dokka . model . DClasslike
import  org . jetbrains . dokka . model . DPackage

/** Converts documentables into components for the root metadata (class/package index). */
internal class RootDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder,
    private val javadocConverter: DocTagConverter,
) {
    /**
     * This actually provides an index for all (displayed) classlikes, not just classes
     *
     * @return the root component for the class index page
     */
    // ALL(KMP b/256171288)
    suspend fun classesIndexPage(): DevsitePage<ClassIndex> {
        val allClasses = docsHolder.allClasslikesToDisplay()
        // Custom sorting for this because of the alphabetization scheme. Grouping preserves sort.
        val alphabetizedClasses = allClasses.sortedBy { it.name() }.groupBy(::categorizeClasslikes)
        val componentClasses =
            alphabetizedClasses.mapValues { (_, nodes) ->
                DefaultSummaryList(
                    SummaryList.Params(
                        items = nodes.map { javadocConverter.summaryForDocumentable(it) },
                    ),
                )
            }

        return DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage = displayLanguage,
                pathForSwitcher = pathProvider.classes.removePrefix(pathProvider.rootPath + "/"),
                bookPath = pathProvider.book,
                title = "Class Index",
                content =
                    DefaultClassIndex(
                        ClassIndex.Params(
                            pathProvider.packages,
                            componentClasses,
                        ),
                    ),
                metadataComponent = null,
                includedHeadTagPath = pathProvider.includedHeadTagsPath,
            ),
        )
    }

    /** @return the root component for the package index page */
    // ALL(KMP b/256171288)
    suspend fun packagesIndexPage(): DevsitePage<PackageIndex> {
        val packages = docsHolder.packages()
        val componentPackages =
            DefaultSummaryList(
                SummaryList.Params(
                    items =
                        packages
                            .filter {
                                it.name != "[root]"
                            } // this synthetic package has broken self-links
                            .map {
                                javadocConverter.summaryForDocumentable(it, showAnnotations = false)
                            },
                ),
            )

        return DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage = displayLanguage,
                pathForSwitcher = pathProvider.packages.removePrefix(pathProvider.rootPath + "/"),
                bookPath = pathProvider.book,
                title = "Package Index",
                content =
                    DefaultPackageIndex(
                        PackageIndex.Params(
                            pathProvider.classes,
                            componentPackages,
                        ),
                    ),
                metadataComponent = null,
                includedHeadTagPath = pathProvider.includedHeadTagsPath,
            ),
        )
    }

    /** @return the Devsite _toc.yaml */
    suspend fun tocPage(packagePrefixToRemove: String?): TableOfContents {
        val packageComponents =
            docsHolder
                .packages()
                .map { dPackage ->
                    coroutineScope { packageForTocAsync(dPackage, packagePrefixToRemove) }
                }
                .awaitAll()

        return DefaultTableOfContents(
            TableOfContents.Params(
                classesUrl = pathProvider.classes,
                packagesUrl = pathProvider.packages,
                packages = packageComponents,
            ),
        )
    }

    /** Groups class-like types into buckets of their first letter. */
    private fun categorizeClasslikes(classlike: DClasslike): Char {
        return classlike.name().first().uppercaseChar()
    }

    private fun CoroutineScope.packageForTocAsync(
        dPackage: DPackage,
        packagePrefixToRemove: String?,
    ): Deferred<DefaultTocPackage> = async {
        val  interfaces = docsHolder.interfacesFor(dPackage).map(::typeForToc)
        val objects =
            docsHolder
                .interestingObjectsFor(dPackage)
                .filterNot { it.isExceptionClass }
                .map(::typeForToc)
        val classes =
            docsHolder
                .classlikesToDisplayFor(dPackage)
                .filterIsInstance<DClass>()
                .filterNot { it.isExceptionClass }
                .map(::typeForToc)
        val enums = docsHolder.enumsFor(dPackage).map(::typeForToc)
        val  exceptions = docsHolder.exceptionsFor(dPackage).map(::typeForToc)
        val  annotations = docsHolder.annotationsFor(dPackage).map(::typeForToc)

        // Update the string to trim to end with a `.` if it doesn't already.
        val prefixToTrim = (packagePrefixToRemove?.removeSuffix(".")?.plus(".")) ?: ""

        DefaultTocPackage(
            TocPackage.Params(
                name = dPackage.name.removePrefix(prefixToTrim),
                packageUrl = pathProvider.forReference(dPackage.dri).url,
                interfaces = interfaces,
                classes =
                    if (displayLanguage == Language.KOTLIN) {
                        classes
                    } else (classes + objects).sortedBy { it.name },
                enums = enums,
                exceptions = exceptions,
                annotations = annotations,
                objects = if (displayLanguage == Language.KOTLIN) objects else emptyList(),
            ), // Typealiases do not appear in the toc because they do not get their own pages
        )
    }

    private fun typeForToc(classlike: DClasslike): TocPackage.Type {
        val url = pathProvider.forReference(classlike.dri).url
        return TocPackage.Type(classlike.name(), url)
    }
}
