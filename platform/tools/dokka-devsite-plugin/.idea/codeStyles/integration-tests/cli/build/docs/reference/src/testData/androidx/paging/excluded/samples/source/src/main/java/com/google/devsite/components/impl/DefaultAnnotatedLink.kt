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
import com.google.devsite.components.symbols.AnnotatedLink
import com.google.devsite.joinMaybePrefix
import kotlinx.html.FlowContent

internal data class DefaultAnnotatedLink(
    override val data: AnnotatedLink.Params,
) : AnnotatedLink {

    override fun render(into: FlowContent) =
        into.run {
            data.annotations.render(into, separator = " ")
            data.link.render(into)
        }

    override fun toString() = data.annotations.joinMaybePrefix(postfix = " ") + data.link
}
