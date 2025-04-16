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
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.EmptyModifiers
import com.google.devsite.renderer.converters.Modifiers
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.div

/** Represents a fully documented function or property. */
internal interface SymbolDetail<T : SymbolSignature> : ContextFreeComponent {
    val data: Params<T>

    fun layout(into: FlowContent, contents: DIV.() -> Unit) =
        into.run { div(classes = "list") { contents() } }

    // Because this is not the bottom of a class hierarchy (KmpSymbolDetail), it can't be `data`
    open class Params<T : SymbolSignature>(
        open val name: String,
        open val returnType: TypeProjectionComponent,
        open val symbolKind: SymbolKind,
        open val signature: T,
        open val anchors: LinkedHashSet<String>,
        open val metadata: List<ContextFreeComponent>,
        open val displayLanguage: Language,
        open val modifiers: Modifiers = EmptyModifiers,
        open val extFunctionClass: String? = null,
        open val annotationComponents: List<AnnotationComponent> = emptyList(),
        open val metadataComponent: MetadataComponent? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params<*>) return false
            if (name != other.name) return false
            if (returnType != other.returnType) return false
            if (symbolKind != other.symbolKind) return false
            if (signature != other.signature) return false
            if (anchors != other.anchors) return false
            if (metadata != other.metadata) return false
            if (displayLanguage != other.displayLanguage) return false
            if (modifiers != other.modifiers) return false
            if (extFunctionClass != other.extFunctionClass) return false
            if (annotationComponents != other.annotationComponents) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + returnType.hashCode()
            result = 31 * result + symbolKind.hashCode()
            result = 31 * result + signature.hashCode()
            result = 31 * result + anchors.hashCode()
            result = 31 * result + metadata.hashCode()
            result = 31 * result + displayLanguage.hashCode()
            result = 31 * result + modifiers.hashCode()
            result = 31 * result + (extFunctionClass?.hashCode() ?: 0)
            result = 31 * result + annotationComponents.hashCode()
            return result
        }
    }

    /** Holds the Kotlin keywords for various symbol types. */
    enum class SymbolKind(val keyword: String) {
        READ_ONLY_PROPERTY("val"),
        PROPERTY("var"),
        FUNCTION("fun"),
        CONSTRUCTOR(""),
    }
}
