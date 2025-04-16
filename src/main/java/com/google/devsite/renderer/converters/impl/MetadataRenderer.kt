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

package com.google.devsite.renderer.impl

import com.google.devsite.components.impl.DefaultPackageList
import com.google.devsite.components.impl.DefaultRedirectPage
import com.google.devsite.components.pages.PackageList
import com.google.devsite.components.pages.RedirectPage
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.DocTagConverter
import com.google.devsite.renderer.converters.RootDocumentableConverter
import com.google.devsite.renderer.impl.paths.FilePathProvider
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.OutputWriter

/** Renders root metadata files that provide a global overview of the entire packages surface. */
internal class MetadataRenderer(
    private val outputWriter: OutputWriter,
    private val pathProvider: FilePathProvider,
    displayLanguage: Language,
    private val docsHolder: DocumentablesHolder,
    javadocConverter: DocTagConverter,
) {
    private val converter =
        RootDocumentableConverter(
            displayLanguage,
            pathProvider,
            docsHolder,
            javadocConverter,
        )

    /** Writes the list of packages in machine-readable format. */
    suspend fun writePackageList() {
        val component =
            DefaultPackageList(PackageList.Params(docsHolder.packages().map { it.name }))
        val packageList = buildString { component.render(this) }
        outputWriter.write(pathProvider.packageList, packageList, "")
    }

    /** Writes the home page. */
    suspend fun writeRootIndex() {
        val redirectUrl = pathProvider.classes
        val redirectComponent = DefaultRedirectPage(RedirectPage.Params(redirectUrl))
        val rootIndex = createHTML().html { redirectComponent.render(this) }
        outputWriter.write(pathProvider.rootIndex, rootIndex, "")
    }

    /** Writes the list of packages in human-readable format. */
    suspend fun writePackages() {
        val page = converter.packagesIndexPage()
        val packageIndex = createHTML().html { page.render(this) }
        outputWriter.write(pathProvider.packages, packageIndex, "")
    }

    /** Writes the list of classes in human-readable format. */
    suspend fun writeClasses() {
        val page = converter.classesIndexPage()
        val classIndex = createHTML().html { page.render(this) }
        outputWriter.write(pathProvider.classes, classIndex, "")
    }

    /** Writes the ToC for devsite consumption. */
    suspend fun writeToc(packagePrefixToRemove: String?) {
        val toc = buildString { converter.tocPage(packagePrefixToRemove).render(this) }
        outputWriter.write(pathProvider.toc, toc, "")
    }
}
