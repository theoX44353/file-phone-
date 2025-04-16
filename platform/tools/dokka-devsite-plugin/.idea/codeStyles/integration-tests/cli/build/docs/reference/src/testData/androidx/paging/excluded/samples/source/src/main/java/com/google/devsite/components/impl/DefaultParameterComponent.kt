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

import com.google.devsite.components.render
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.joinMaybePrefix
import com.google.devsite.renderer.Language
import kotlinx.html.Entities
import kotlinx.html.FlowContent

/** Default implementation of a function parameter. */
internal data class DefaultParameterComponent(
    override val data: ParameterComponent.Params,
) : ParameterComponent {

    override fun render(into: FlowContent) =
        into.run {
            data.annotationComponents.render(into, separator = "", terminator = { +" " })

            when (data.displayLanguage) {
                Language.JAVA -> {
                    data.type.render(into)
                    if (data.name.isNotEmpty()) {
                        +Entities.nbsp
                        +data.name
                    }
                }
                Language.KOTLIN -> {
                    // vararg is the only valid parameter modifier I know of
                    for (modifier in data.modifiers) {
                        +modifier
                        +Entities.nbsp
                    }

                    if (data.name.isNotEmpty()) {
                        +data.name
                        +":"
                        +Entities.nbsp
                    }

                    data.type.render(into)

                    if (data.defaultValue != null) {
                        +" = ${data.defaultValue}"
                    }
                }
            }
        }

    override fun toString() =
        data.annotationComponents.joinMaybePrefix(postfix = " ") +
            data.modifiers.joinMaybePrefix(postfix = " ") +
            data.type +
            if (data.name.isNotEmpty()) {
                " " + data.name
            } else "" + (data.defaultValue ?: "")
}
