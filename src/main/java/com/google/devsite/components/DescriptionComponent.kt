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

package com.google.devsite.components

import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.doc.DocTag

/** Represents the hand-written documentation for a symbol. */
internal interface DescriptionComponent : ContextFreeComponent {
    val data: Params

    open class Params(
        val pathProvider: FilePathProvider?,
        val components: List<DocTag> = emptyList(),
        val summary: Boolean = false,
        val deprecation: String? = null,
        val docsHolder: DocumentablesHolder? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params) return false

            // NOTE: DocTag is a sealed class that is all data classes
            if (components != other.components) return false
            if (summary != other.summary) return false
            if (deprecation != other.deprecation) return false

            return true
        }

        override fun hashCode(): Int {
            var result = components.hashCode()
            result = 31 * result + summary.hashCode()
            result = 31 * result + (deprecation?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "components: $components, " + "summary: $summary, " + "deprecation: $deprecation"
        }
    }
}
