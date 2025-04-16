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
import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.table.ClassHierarchy
import com.google.devsite.components.table.RelatedSymbols

/** Represents class signature. */
internal interface ClasslikeDescription : ContextFreeComponent {
    val data: Params

    open class Params(
        open val header: DefaultDevsitePlatformSelector?,
        open val primarySignature: ClasslikeSignature,
        open val hierarchy: ClassHierarchy,
        open val relatedSymbols: RelatedSymbols,
        open val descriptionDocs: List<ContextFreeComponent>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params) return false

            if (header != other.header) return false
            if (primarySignature != other.primarySignature) return false
            if (hierarchy != other.hierarchy) return false
            if (relatedSymbols != other.relatedSymbols) return false
            if (descriptionDocs != other.descriptionDocs) return false

            return true
        }

        override fun hashCode(): Int {
            var result = header?.hashCode() ?: 0
            result = 31 * result + primarySignature.hashCode()
            result = 31 * result + hierarchy.hashCode()
            result = 31 * result + relatedSymbols.hashCode()
            result = 31 * result + descriptionDocs.hashCode()
            return result
        }

        override fun toString() =
            "Classlike Description:\n$header\n$primarySignature\n$hierarchy" +
                (if (!relatedSymbols.isEmpty) relatedSymbols else "") +
                (descriptionDocs.ifEmpty { "" })
    }
}
