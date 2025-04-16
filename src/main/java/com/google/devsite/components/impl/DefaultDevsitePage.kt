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

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.pages.DevsitePage
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.title
import kotlinx.html.unsafe

/** Default implementation of the root component for devsite. */
internal data class DefaultDevsitePage<T : ContextFreeComponent>(
    override val data: DevsitePage.Params<T>,
) : DevsitePage<T> {

    override fun render(into: HTML) =
        into.run {
            attributes["devsite"] = "true"
            head {
                title { +data.title }
                unsafe { +"{% setvar book_path %}${data.bookPath}{% endsetvar %}\n" }
                data.includedHeadTagPath?.let { unsafe { +"{% include \"${it}\" %}\n" } }
            }

            body {
                // Add devsite ReferenceObject metadata
                data.referenceObject?.render(this)

                div {
                    // CSS id declared in internal codebase (cl/548038546)
                    id = "header-block"

                    // Devsite appends a tooltip element to h1 declarations, so a div is needed here
                    // to
                    // group the h1 and tooltip elements together for Flexbox usage. Otherwise,
                    // Flexbox
                    // will split up the h1, tooltip, and metadata elements equally into thirds.
                    div { h1 { +data.title } }

                    data.metadataComponent?.render(this)
                }

                if (data.pathForSwitcher != null) {
                    // When devsite injects the switcher, it prefixes this path with `kotlin` if
                    // needed.
                    unsafe { +"\n{% setvar page_path %}${data.pathForSwitcher}{% endsetvar %}" }
                    unsafe { +"\n{% setvar can_switch %}1{% endsetvar %}" }
                    unsafe {
                        +"\n{% include \"reference/_${data.displayLanguage}_switcher2.md\" %}\n"
                    }
                }

                data.content.render(this)
            }
        }

    override fun toString() =
        "DevsitePage: ${data.title} at ${data.bookPath}\n${data.content} " +
            "with metadata " +
            (data.metadataComponent?.toString() ?: "")
}
