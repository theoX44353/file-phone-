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

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.HtmlComponent
import com.google.devsite.components.symbols.MetadataComponent
import com.google.devsite.components.symbols.ReferenceObject
import com.google.devsite.renderer.Language
import kotlinx.html.HTML

/** Represents any devsite page and should be used as the root component. */
internal interface DevsitePage<T : ContextFreeComponent> : HtmlComponent<HTML> {
    val data: Params<T>

    data class Params<T : ContextFreeComponent>(
        val displayLanguage: Language,
        // of the form andoidx/paging/PagingSource.html. Does not have a /reference/language prefix.
        val pathForSwitcher: String?,
        val bookPath: String,
        val title: String,
        val content: T,
        val metadataComponent: MetadataComponent?,
        val includedHeadTagPath: String?,
        val referenceObject: ReferenceObject? = null,
    )
}
