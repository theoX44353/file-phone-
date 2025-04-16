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

import com.google.devsite.components.devsiteFilter
import com.google.devsite.components.render
import com.google.devsite.components.symbols.KmpClasslikeDescription
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.hr
import kotlinx.html.li
import kotlinx.html.pre
import kotlinx.html.style
import kotlinx.html.ul

internal data class DefaultKmpClasslikeDescription(
    override val data: KmpClasslikeDescription.Params,
) : KmpClasslikeDescription {

    override fun render(into: FlowContent) =
        into.run {
            if (data.header != null) data.header.render(this)
            devsiteFilter {
                into.div {
                    into.ul("list") {
                        style = "list-style: none; padding-left: 0"
                        li { // sadly, we can't easily sort allSignatures by platform; sig ->
                            // List<Platform>
                            data.platform.render(this)
                            pre { data.primarySignature.render(this) }
                        }
                        data.allSignatures.forEach { (signature, platform) ->
                            if (signature == data.primarySignature) return@forEach
                            li {
                                platform.render(this)
                                pre { signature.render(this) }
                            }
                        }
                    }
                }
            }
            data.hierarchy.render(this)
            data.relatedSymbols.render(this)
            data.descriptionDocs.render(this, separator = null, header = { hr() })
        }
}
