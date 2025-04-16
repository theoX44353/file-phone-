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
import com.google.devsite.components.Link
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Modifiers

/** Represents class signature. */
internal interface ClasslikeSignature : ContextFreeComponent {
    val data: Params

    data class Params(
        val displayLanguage: Language,
        val modifiers: Modifiers,
        val type: String,
        val name: Link,
        val implements: List<Link>,
        val extends: List<Link>,
        val typeParameters: List<TypeParameterComponent>,
        val annotationComponents: List<AnnotationComponent>,
        val typeAliasEquals: TypeProjectionComponent? = null,
    )
}
