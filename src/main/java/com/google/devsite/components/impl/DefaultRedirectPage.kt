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

import com.google.devsite.components.pages.RedirectPage
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.title
import kotlinx.html.unsafe

/** Default implementation of a client-side redirect. */
internal data class DefaultRedirectPage(
    override val data: RedirectPage.Params,
) : RedirectPage {
    override fun render(into: HTML) =
        into.run {
            head {
                meta(charset = "utf-8")
                meta(content = "0; url=${data.url}") { httpEquiv = "refresh" }
                meta(name = "robots", content = "noindex")
                link(rel = "canonical", href = data.url)

                title {
                    +"Redirecting"
                    unsafe { +"&hellip;" }
                }
            }

            body {
                h1 {
                    +"Redirecting"
                    unsafe { +"&hellip;" }
                }

                a(data.url) { +"Click here if you are not redirected." }
            }
        }

    override fun toString() = "Redirect page to ${data.url}"
}
