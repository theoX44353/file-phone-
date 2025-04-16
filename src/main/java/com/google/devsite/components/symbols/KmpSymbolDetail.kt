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

package com.google.devsite.components.symbols

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.symbols.SymbolDetail.Params
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.EmptyModifiers
import com.google.devsite.renderer.converters.Modifiers

/** Represents a fully documented function or property. */
internal interface KmpSymbolDetail<T : SymbolSignature> : SymbolDetail<T> {
    override val data: Params<T>

    data class Params<T : SymbolSignature>(
        override val name: String,
        override val returnType: TypeProjectionComponent,
        override val symbolKind: SymbolDetail.SymbolKind,
        override val signature: T,
        override val anchors: LinkedHashSet<String>,
        override val metadata: List<ContextFreeComponent>,
        override val displayLanguage: Language,
        val platforms: PlatformComponent,
        override val modifiers: Modifiers = EmptyModifiers,
        override val extFunctionClass: String? = null,
        override val annotationComponents: List<AnnotationComponent> = emptyList(),
        override val metadataComponent: MetadataComponent? = null,
    ) :
        SymbolDetail.Params<T>(
            name,
            returnType,
            symbolKind,
            signature,
            anchors,
            metadata,
            displayLanguage,
            modifiers,
            extFunctionClass,
            annotationComponents,
            metadataComponent,
        )
}
