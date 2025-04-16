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

package com.google.devsite.components.impl

import com.google.devsite.components.ShouldBreak
import com.google.devsite.components.render
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.joinMaybePrefix
import com.google.devsite.renderer.Language
import kotlinx.html.FlowContent

/** Default implementation of a function parameter type. */
internal data class DefaultTypeProjectionComponent(
    override val data: TypeProjectionComponent.Params,
) : TypeProjectionComponent {
    override fun render(into: FlowContent) =
        into.run {
            data.annotationComponents.render(this, separator = "", terminator = { +" " })
            data.type.render(this)
            data.generics.render(into, ShouldBreak.NO, brackets = "<>")
            if (data.displayLanguage == Language.KOTLIN) +data.nullability.renderAsKotlinSuffix()
        }

    override fun toString() =
        data.annotationComponents.joinMaybePrefix(postfix = " ") +
            data.type +
            data.generics.joinMaybePrefix(prefix = " <", postfix = ">") +
            // Do this regardless of displayLanguage, we don't care, it only appears in debugging
            data.nullability.renderAsKotlinSuffix()
}
