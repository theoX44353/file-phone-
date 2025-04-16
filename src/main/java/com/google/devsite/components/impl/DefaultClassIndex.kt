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

import com.google.devsite.components.pages.ClassIndex
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.em
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.unsafe

/** Default implementation of the list of classes page. */
internal data class DefaultClassIndex(
    override val data: ClassIndex.Params,
) : ClassIndex {
    override fun render(into: FlowContent) =
        into.run {
            p {
                +"These are all the API classes. See all "
                a(data.packagesUrl) { +"API packages" }
                +"."
            }

            if (data.alphabetizedClasses.isEmpty()) {
                p { em { +"This project has no classes." } }
                return
            }

            div("jd-letterlist") {
                for ((letter) in data.alphabetizedClasses.entries) {
                    a("#letter_$letter") { +letter.toString() }
                    unsafe { +"&nbsp;&nbsp;" }
                }
            }

            for ((letter, summary) in data.alphabetizedClasses.entries) {
                h2 {
                    id = "letter_$letter"
                    +letter.toString()
                }

                summary.render(this)
            }
        }

    override fun toString() =
        data.packagesUrl + " " + data.alphabetizedClasses.map { "${it.key}: ${it.value}" }
}
