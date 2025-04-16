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
import com.google.devsite.components.Sizeable

/** Represents an annotation. */
internal interface AnnotationComponent : ContextFreeComponent, Sizeable {
    val data: Params

    override fun length(): Int {
        val typeSize = data.type.length()
        val paramSize = data.parameters.sumOf { it.length() }

        return typeSize + paramSize
    }

    data class Params(
        val type: Link,
        val parameters: List<AnnotationParameter> = emptyList(),
    )

    val name: String
        get() = data.type.data.name
}
