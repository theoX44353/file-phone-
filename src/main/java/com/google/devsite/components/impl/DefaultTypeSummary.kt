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
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.joinMaybePrefix
import kotlinx.html.FlowContent

/** Default implementation of a type summary. */
internal data class DefaultTypeSummary(
    override val data: TypeSummary.Params,
) : TypeSummary {
    override fun render(into: FlowContent) =
        into.run {
            data.modifiers.render(into, terminator = { +" " })
            data.type.render(into)
        }

    override fun toString() = data.modifiers.joinMaybePrefix(postfix = " ") + data.type
}
