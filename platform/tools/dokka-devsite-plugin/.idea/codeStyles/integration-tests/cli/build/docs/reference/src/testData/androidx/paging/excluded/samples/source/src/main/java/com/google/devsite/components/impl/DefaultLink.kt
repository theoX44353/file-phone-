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

import com.google.devsite.components.Link
import kotlinx.html.FlowContent
import kotlinx.html.a

/** Default implementation of a link. */
internal data class DefaultLink(
    override val data: Link.Params,
) : Link {
    override fun render(into: FlowContent) =
        into.run {
            if (data.url.isEmpty()) {
                +data.name
            } else if (data.externalLink) {
                a(data.url, classes = "external") { +data.name }
            } else {
                a(data.url) { +data.name }
            }
        }

    override fun toString() = data.name + if (data.url.isNotEmpty()) "<ref=${data.url}>" else ""
}
