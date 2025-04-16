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

import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.EmptyModifiers
import com.google.devsite.renderer.converters.Modifiers

/** Represents a function or method parameter. */
internal interface ParameterComponent : SymbolBase {
    val data: Params

    override fun length(): Int {
        var result = data.name.length

        result += data.defaultValue?.length ?: 0
        result += data.annotationComponents.sumOf { it.length() }
        result += data.modifiers.sumOf { it.length }
        result += data.type.length()

        return result
    }

    open class Params(
        open val name: String,
        open val type: TypeProjectionComponent,
        open val displayLanguage: Language,
        open val modifiers: Modifiers = EmptyModifiers,
        val defaultValue: String? = null,
        open val annotationComponents: List<AnnotationComponent> = emptyList(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params) return false

            if (displayLanguage != other.displayLanguage) return false
            if (name != other.name) return false
            if (modifiers != other.modifiers) return false
            if (type != other.type) return false
            if (annotationComponents != other.annotationComponents) return false
            if (defaultValue != other.defaultValue) return false

            return true
        }

        override fun hashCode(): Int {
            var result = displayLanguage.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + modifiers.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + annotationComponents.hashCode()
            result = 31 * result + (defaultValue?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "displayLanguage: $displayLanguage, " +
                "name: $name, " +
                "modifiers: $modifiers, " +
                "type: $type, " +
                "annotationComponents: $annotationComponents, " +
                "defaultValue: $defaultValue"
        }
    }

    val nullable: Boolean
        get() = data.type.nullable || data.annotationComponents.any { it.name == "Nullable" }

    val annotations: List<AnnotationComponent>
        get() = data.annotationComponents
}
