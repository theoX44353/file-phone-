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

import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.impl.DefaultDescriptionComponent

/** Represents a symbols' signature along with its description. */
internal interface SymbolSummary<T : SymbolSignature> : DescriptionComponent {
    override val data: Params<T>

    data class Params<T : SymbolSignature>(
        val signature: T,
        val description: DescriptionComponent,
        val annotationComponents: List<AnnotationComponent>,
    ) :
        DescriptionComponent.Params(
            pathProvider = description.guarded?.data?.pathProvider,
            components = description.guarded?.data?.components ?: emptyList(),
            summary = description.guarded?.data?.summary ?: true,
            deprecation = description.guarded?.data?.deprecation,
            docsHolder = description.guarded?.data?.docsHolder,
        )
}

// We can't directly access e.g. description.data.pathProvider, because attempting to throws for
// NoopSymbolSummary and UndocumentedSymbolSummary, because we have ~mocks in our primary hierarchy.
private val DescriptionComponent.guarded: DescriptionComponent?
    get() = if (this is DefaultDescriptionComponent) this else null
