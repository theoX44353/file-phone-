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

package com.google.devsite.components

import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.unsafe

/**
 * Represents a component that doesn't need to be in a specific HTML context to render itself
 * correctly.
 */
internal interface ContextFreeComponent : HtmlComponent<FlowContent>

/**
 * Recreates the functionality of non-standard <nobr> tag, used to prevent browser from inserting
 * line breaks in the given content to render.
 */
internal fun FlowContent.nobr(render: HTMLTag.() -> Unit) {
    if (this !is HTMLTag) {
        return
    }
    unsafe { +"<span style=\"white-space: nowrap;\">" }
    render()
    unsafe { +"</span>" }
}
