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
import com.google.devsite.components.Sizeable

/** Represents a function or method signature (aka just the name and params). */
internal interface FunctionSignature : SymbolSignature, Sizeable {
    override val data: Params

    data class Params(
        override val name: Link,
        override val receiver: ParameterComponent? = null,
        val typeParameters: List<TypeParameterComponent> = emptyList(),
        val parameters: List<ParameterComponent> = emptyList(),
        val isDeprecated: Boolean = false,
    ) : SymbolSignature.Params

    override fun length(): Int {
        val nameSize = data.name.length()
        val allParams = data.typeParameters + listOfNotNull(data.receiver) + data.parameters
        val paramSize = allParams.sumOf { it.length() + 2 }

        return nameSize + paramSize
    }
}
