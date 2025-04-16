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

package com.google.devsite.renderer.impl.paths

import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Nullability
import com.google.devsite.renderer.impl.DocumentablesGraph
import com.google.devsite.renderer.impl.paths.FilePathProvider.Companion.joinPaths

/** Directory structure tailored for devsite tenants. */
internal open class DevsiteFilePathProvider(
    final override val language: Language,
    docRootPath: String,
    languagePath: String,
    projectPath: String,
    override val includedHeadTagsPath: String?,
    override val locationProvider: ExternalDokkaLocationProvider? = null,
    override val documentablesGraph: DocumentablesGraph,
) : FilePathProvider {
    final override val rootPath = joinPaths("/", docRootPath, languagePath)

    override val packageList = joinPaths(rootPath, projectPath, MACHINE_PACKAGE_LIST_FILE)

    override val packages = joinPaths(rootPath, projectPath, PACKAGE_INDEX_FILE)

    override val classes = joinPaths(rootPath, projectPath, CLASS_INDEX_FILE)

    override val rootIndex = joinPaths(rootPath, projectPath, DIR_INDEX_FILE)

    override val toc = joinPaths(rootPath, projectPath, TOC_FILE)

    override val book = joinPaths(rootPath, projectPath, BOOK_FILE)

    override fun forType(packageName: String, name: String): String {
        val packageAsPath = packageName.replace(".", "/")
        return joinPaths(rootPath, packageAsPath, "$name.html")
    }

    // Set value of ANY based on the language
    override val ANY: TypeProjectionComponent =
        when (language) {
            Language.KOTLIN ->
                DefaultTypeProjectionComponent(
                    TypeProjectionComponent.Params(
                        type = this.linkForReference(ANY_DRI[Language.KOTLIN]!!),
                        nullability = Nullability.KOTLIN_DEFAULT,
                        displayLanguage = Language.KOTLIN,
                    ),
                )
            Language.JAVA ->
                DefaultTypeProjectionComponent(
                    TypeProjectionComponent.Params(
                        type = this.linkForReference(ANY_DRI[Language.JAVA]!!),
                        nullability = Nullability.JAVA_NOT_ANNOTATED,
                        displayLanguage = Language.JAVA,
                    ),
                )
        }
}
