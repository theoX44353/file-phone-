/*
 * Copyright 2024 The Android Open Source Project
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

import com.google.devsite.components.symbols.ReferenceObject
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.itemProp
import kotlinx.html.meta

/** See https://developers.google.com/devsite/reference/microdata/referenceobject */
internal data class DefaultReferenceObject(
    override val data: ReferenceObject.Params,
) : ReferenceObject {
    override fun render(into: FlowContent) =
        into.run {
            div {
                // The itemscope attribute doesn't need a value, but kotlinx.html requires one.
                attributes["itemscope"] = ""
                attributes["itemtype"] = "http://developers.google.com/ReferenceObject"
                meta {
                    itemProp = "name"
                    content = data.name
                }
                data.path?.let {
                    meta {
                        itemProp = "path"
                        content = it
                    }
                }
                for (property in data.properties) {
                    meta {
                        itemProp = "property"
                        content = property
                    }
                }
                meta {
                    itemProp = "language"
                    content = data.language.name
                }
            }
        }
}
