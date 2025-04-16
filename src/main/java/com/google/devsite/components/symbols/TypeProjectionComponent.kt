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
internal interface TypeProjectionComponent : SymbolBase {
    val data: Params

    override fun length(): Int {
        val typeSize = data.type.length()
        val annotationSize = data.annotationComponents.sumOf { it.length() }
        val genericsSize = data.generics.sumOf { it.length() + 2 }

        return typeSize + annotationSize + genericsSize
    }

    open class Params(
        open val type: Link,
        open val nullability: Nullability,
        open val displayLanguage: Language,
        open val generics: List<TypeProjectionComponent> = emptyList(),
        open val annotationComponents: List<AnnotationComponent> = emptyList(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params) return false

            if (type != other.type) return false
            if (annotationComponents != other.annotationComponents) return false
            if (nullability != other.nullability) return false
            if (generics != other.generics) return false
            if (displayLanguage != other.displayLanguage) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + annotationComponents.hashCode()
            result = 31 * result + nullability.hashCode()
            result = 31 * result + generics.hashCode()
            result = 31 * result + displayLanguage.hashCode()
            return result
        }

        override fun toString(): String {
            return "type: $type, " +
                "annotationComponents: $annotationComponents, " +
                "nullability: $nullability, " +
                "generics: $generics, " +
                "displayLanguage: $displayLanguage"
        }
    }

    val nullable: Boolean
        get() = data.nullability.nullable

    val annotations: List<AnnotationComponent>
        get() = data.annotationComponents
}
