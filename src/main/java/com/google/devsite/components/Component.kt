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

import com.google.devsite.components.symbols.Platform
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.symbols.devsiteId
import com.google.devsite.components.symbols.selectorDisplayName
import kotlinx.html.Entities
import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.attributesMapOf
import kotlinx.html.br
import kotlinx.html.option
import kotlinx.html.select
import kotlinx.html.visit

/** A component renders some data into a UI container. */
internal interface Component<T> {
    /** Render this component's data into the container. */
    fun render(into: T)
}

inline fun FlowContent.devsiteFilter(crossinline block: HTMLTag.() -> Unit = {}) =
    HTMLTag(
            tagName = "devsite-filter ",
            consumer = consumer,
            initialAttributes = attributesMapOf("select-el-container-id", "platform"),
            namespace = null,
            inlineTag = false,
            emptyTag = false,
        )
        .visit(block)

fun FlowContent.devsitePlatformSelector(
    platforms: List<Platform>,
) =
    HTMLTag(
            tagName = "devsite-select ",
            consumer = consumer,
            initialAttributes = attributesMapOf("id", "platform", "label", "Select a platform"),
            namespace = null,
            inlineTag = false,
            emptyTag = false,
        )
        .visit {
            this@devsitePlatformSelector.select {
                multiple = true
                platforms.map { platform ->
                    option {
                        selected = true
                        value = platform.devsiteId()
                        +platform.selectorDisplayName()
                    }
                }
            }
        }

internal fun List<Component<FlowContent>>.render(
    into: FlowContent,
    shouldBreak: ShouldBreak = ShouldBreak.MAYBE,
    separator: String? = ",",
    brackets: String = "",
    header: (() -> Unit)? = null,
    terminator: (() -> Unit)? = null,
) =
    into.run {
        if (isEmpty() && brackets != "()")
            return@run // do not print empty brackets except fun() parens
        if (header != null) header()
        if (brackets != "") +brackets[0].toString()
        if (shouldBreak == ShouldBreak.AND_INDENT) br()
        for (parameter in this@render) {
            if (shouldBreak == ShouldBreak.AND_INDENT) repeat(4) { +Entities.nbsp }
            // Group type parameters within a single set of <>s, rather than each making their own
            if (parameter is TypeParameterComponent) {
                parameter.render(this, false)
            } else parameter.render(this)

            if (
                parameter !== last() && separator != null
            ) { // null separator -> no separation/spaces
                +separator
                when (shouldBreak) {
                    ShouldBreak.YES,
                    ShouldBreak.AND_INDENT -> {
                        br()
                    }
                    ShouldBreak.MAYBE -> {
                        +" "
                    }
                    ShouldBreak.NO -> {
                        +Entities.nbsp
                    }
                }
            }
        }
        if (shouldBreak == ShouldBreak.YES || shouldBreak == ShouldBreak.AND_INDENT) br()
        if (brackets != "") +brackets[1].toString()
        if (terminator != null) terminator()
    }

internal fun List<String>.render(
    into: FlowContent,
    nbsp: Boolean = true,
    separator: String = "",
    header: (() -> Unit)? = null,
    terminator: (() -> Unit)? = null,
) =
    into.run {
        if (isEmpty()) return@run
        if (header != null) header()
        for (string in this@render) {
            +string
            if (string != last()) {
                +separator
                if (nbsp) +Entities.nbsp else +" "
            }
        }
        if (terminator != null) terminator()
    }

internal fun List<String>.length(separator: String = " ") =
    sumOf { it.length } + (size - 1) * separator.length

internal val String?.length: Int
    get() = this?.let { this.length } ?: 0

internal enum class ShouldBreak {
    YES, //   Manually break
    AND_INDENT, // Manually break and indent
    MAYBE, // Use spaces instead of nbsps. If it breaks, it breaks
    NO, //     Do not break line. nbsps everywhere
}

/** Infer from the length of the Sizeable whether to explicitly break the line */
internal fun Sizeable.shouldBreak() =
    if (length() < ASSUMED_MINIMUM_LINE_LENGTH) {
        ShouldBreak.NO
    } else ShouldBreak.AND_INDENT

/** We assume that the two-column tables will be more than 70 chars long, but not far more */
private const val ASSUMED_MINIMUM_LINE_LENGTH = 70
