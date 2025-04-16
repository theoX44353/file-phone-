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

import com.google.devsite.components.Link
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Nullability

/** Represents a symbol type such as function parameter types. */
internal interface LambdaTypeProjectionComponent : TypeProjectionComponent {
    override val data: Params

    override fun length(): Int {
        val typeSize = data.returnType.length()
        val annotationSize = data.annotationComponents.sumOf { it.length() }
        val lambdaParamsSize = data.lambdaParams.sumOf { it.length() }
        val lambdaModifiersSize = data.lambdaModifiers.sumOf { it.length }
        return "() ->".length + typeSize + annotationSize + lambdaModifiersSize + lambdaParamsSize
    }

    data class Params(
        val returnType: TypeProjectionComponent,
        override val type: Link = returnType.data.type, // should never be accessed directly, use ^^
        override val nullability: Nullability,
        override val displayLanguage: Language,
        val lambdaModifiers: List<String> = emptyList(),
        // Lambda parameters can be named
        val lambdaParams: List<ParameterComponent> = emptyList(),
        val receiver: TypeProjectionComponent? = null,
        // Generics on the return type
        override val generics: List<TypeProjectionComponent> = emptyList(),
        override val annotationComponents: List<AnnotationComponent> = emptyList(),
    ) :
        TypeProjectionComponent.Params(
            type = type,
            nullability = nullability,
            displayLanguage = displayLanguage,
            generics = emptyList(),
            annotationComponents = annotationComponents,
        )
}
