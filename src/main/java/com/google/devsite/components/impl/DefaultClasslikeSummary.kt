/*
 * Copyright 2022 The Android Open Source Project
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

import com.google.devsite.components.symbols.ClasslikeSummary
import kotlinx.html.FlowContent
import kotlinx.html.code
import kotlinx.html.div

internal data class DefaultClasslikeSummary(
    override val data: ClasslikeSummary.Params,
) : ClasslikeSummary {
    override fun render(into: FlowContent) =
        into.run {
            div { code { data.signature.render(this) } }

            data.description.render(this)
        }

    override fun toString() = "${data.signature}: ${data.description}"
}
