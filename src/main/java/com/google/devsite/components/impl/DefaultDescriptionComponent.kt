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

import com.google.devsite.components.DescriptionComponent
import kotlinx.html.DIV
import kotlinx.html.DL
import kotlinx.html.FlowContent
import kotlinx.html.OL
import kotlinx.html.TABLE
import kotlinx.html.TBODY
import kotlinx.html.TFOOT
import kotlinx.html.THEAD
import kotlinx.html.TR
import kotlinx.html.UL
import kotlinx.html.a
import kotlinx.html.aside
import kotlinx.html.b
import kotlinx.html.blockQuote
import kotlinx.html.br
import kotlinx.html.caption
import kotlinx.html.code
import kotlinx.html.dd
import kotlinx.html.del
import kotlinx.html.dl
import kotlinx.html.dt
import kotlinx.html.em
import kotlinx.html.hr
import kotlinx.html.htmlVar
import kotlinx.html.img
import kotlinx.html.li
import kotlinx.html.ol
import kotlinx.html.p
import kotlinx.html.pre
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import kotlinx.html.strong
import kotlinx.html.sub
import kotlinx.html.sup
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tfoot
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.html.unsafe
import kotlinx.html.visit
import org.jetbrains.dokka.model.doc.A
import org.jetbrains.dokka.model.doc.B
import org.jetbrains.dokka.model.doc.Big
import org.jetbrains.dokka.model.doc.BlockQuote
import org.jetbrains.dokka.model.doc.Br
import org.jetbrains.dokka.model.doc.Caption
import org.jetbrains.dokka.model.doc.Cite
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.Dd
import org.jetbrains.dokka.model.doc.Dfn
import org.jetbrains.dokka.model.doc.Dir
import org.jetbrains.dokka.model.doc.Div
import org.jetbrains.dokka.model.doc.Dl
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.Dt
import org.jetbrains.dokka.model.doc.Em
import org.jetbrains.dokka.model.doc.Font
import org.jetbrains.dokka.model.doc.Footer
import org.jetbrains.dokka.model.doc.Frame
import org.jetbrains.dokka.model.doc.FrameSet
import org.jetbrains.dokka.model.doc.H1
import org.jetbrains.dokka.model.doc.H2
import org.jetbrains.dokka.model.doc.H3
import org.jetbrains.dokka.model.doc.H4
import org.jetbrains.dokka.model.doc.H5
import org.jetbrains.dokka.model.doc.H6
import org.jetbrains.dokka.model.doc.Head
import org.jetbrains.dokka.model.doc.Header
import org.jetbrains.dokka.model.doc.HorizontalRule
import org.jetbrains.dokka.model.doc.Html
import org.jetbrains.dokka.model.doc.I
import org.jetbrains.dokka.model.doc.IFrame
import org.jetbrains.dokka.model.doc.Img
import org.jetbrains.dokka.model.doc.Index
import org.jetbrains.dokka.model.doc.Input
import org.jetbrains.dokka.model.doc.Li
import org.jetbrains.dokka.model.doc.Link
import org.jetbrains.dokka.model.doc.Listing
import org.jetbrains.dokka.model.doc.Main
import org.jetbrains.dokka.model.doc.Menu
import org.jetbrains.dokka.model.doc.Meta
import org.jetbrains.dokka.model.doc.Nav
import org.jetbrains.dokka.model.doc.NoFrames
import org.jetbrains.dokka.model.doc.NoScript
import org.jetbrains.dokka.model.doc.Ol
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Pre
import org.jetbrains.dokka.model.doc.Script
import org.jetbrains.dokka.model.doc.Section
import org.jetbrains.dokka.model.doc.Small
import org.jetbrains.dokka.model.doc.Span
import org.jetbrains.dokka.model.doc.Strikethrough
import org.jetbrains.dokka.model.doc.Strong
import org.jetbrains.dokka.model.doc.Sub
import org.jetbrains.dokka.model.doc.Sup
import org.jetbrains.dokka.model.doc.TBody
import org.jetbrains.dokka.model.doc.TFoot
import org.jetbrains.dokka.model.doc.THead
import org.jetbrains.dokka.model.doc.Table
import org.jetbrains.dokka.model.doc.Td
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.doc.Th
import org.jetbrains.dokka.model.doc.Title
import org.jetbrains.dokka.model.doc.Tr
import org.jetbrains.dokka.model.doc.Tt
import org.jetbrains.dokka.model.doc.U
import org.jetbrains.dokka.model.doc.Ul
import org.jetbrains.dokka.model.doc.Var

/** Default implementation of the hand-written documentation for a symbol. */
internal data class DefaultDescriptionComponent(
    override val data: DescriptionComponent.Params,
) : DescriptionComponent {
    override fun render(into: FlowContent) =
        into.run {
            if (data.deprecation == null) {
                if (data.summary) {
                    renderTags(data.components, State())
                } else {
                    renderTags(data.components, State())
                }
            } else {
                if (data.summary) {
                    // Displays the deprecation message in a table cell (e.g. class summary table)
                    p {
                        strong { +data.deprecation }
                        if (data.components.firstOrNull() is Text) +" "
                        renderTags(data.components, State())
                    }
                } else {
                    // Displays the deprecation message in notice box with "caution" styling
                    aside("caution") {
                        strong { +data.deprecation }
                        br()
                        renderTags(data.components, State())
                    }
                }
            }
        }

    // TODO: make this work for non-english docstrings
    private val periodSpaceCapital = """\.\s+[A-Z]""".toRegex()
    private val periodSpaceNonLowercase = """\.\s+[0-9A-Z{<`@"(\\\[]""".toRegex()
    private val doesntEnd = listOf("e.g.", "i.e.", "viz.")

    // A doc tag indicating the component uses MathJax (https://docs.mathjax.org/en/v2.7-latest).
    // Dokka does not turn this into a CustomTagWrapper, so it needs to be separately handled.
    private val mathJaxDocTag = "{@usesMathJax}"

    // See go/devsite-reference/widgets/mathjax. While this only _needs_ to be inserted once per
    // page, it works to include it multiple times, so it is injected for each appearance of the
    // doc tag in a detail component.
    private val mathJaxHtml = "<devsite-mathjax config=\"TeX-AMS_SVG\"></devsite-mathjax>"

    /**
     * @param tags the DocTags in this context. This DocTag should be one of them.
     * @Returns whether this Text DocTag ends with the end of a sentence. Specifically, whether it
     *   meets one of the following conditions (each of which indicate that this tag ends a
     *   sentence):
     * 1. Has no children or subsequent tags.
     * 2. Ends in a period, and the next tag/child starts in a capital letter.
     * 3. Ends in a period, and the next tag starts with an ambiguous character (neither upper nor
     *    lower case), AND this tag *does not* end in a known-non-sentence-ending-period-structure,
     *    e.g. "e.g." or "i.e.".
     *
     *    WARNING: only works for english
     */
    private fun Text.breaksAtEndOfTag(tags: List<DocTag>): Boolean {
        if (!this.body.trim().endsWith(".")) return false
        val tagIndex = tags.indexOf(this)
        // Identify the first sentence of the description. Summaries only contain that.
        val followingText = // Simply render all tags into one string for this check.
            (this.children.text(separator = " ") +
                    tags.subList(tagIndex + 1, tags.size).text(separator = " "))
                .trim()
        if (followingText == "") return true
        // In some cases the first sentence doesn't end at the first period e.g. when
        // there is an "e.g.". We detect this by checking whether the first character
        // after the period (if it exists) is capitalized.
        if (followingText[0] in 'A'..'Z') return true
        // If the tag's text ends with "e.g.", "i.e.", or similar, the sentence continues.
        if (doesntEnd.any { this.body.trim().endsWith(it) }) return false
        // It is possible the next character's capitalization can't answer the question
        // --e.g. if it is a number, a link, or a code literal.
        // In such ambiguous cases we (currently) default to sentence-ends-at-period
        if (followingText[0] in '0'..'9' || followingText[0] in "{<`@\"([") return true
        // If the next tag is e.g. a Code tag that begins with a lower case, it's ambiguous.
        if (tagIndex + 1 < tags.size && tags[tagIndex + 1] !is Text) return true
        // If the next tag is a Text that begins with a lower case, the sentence has not ended.
        if (followingText[0] in 'a'..'z') return false
        data.docsHolder
            ?.logger
            ?.warn(
                "You have a strange period in these docs that may or may not end a sentence: " +
                    tags.text(),
            )
        return true // This should never happen--it would require period-space-something-weird
    }

    /**
     * @Returns whether this string contains a sentence end-and-start-a-new at the given index. Uses
     *   similar criteria to the above function.
     */
    private fun String.containsSentenceBreakAt(index: Int): Boolean {
        if (doesntEnd.any { this.subSequence(0, index + 1).endsWith(it) }) return false
        if (periodSpaceCapital.matchesAt(this, index)) return true
        if (!periodSpaceNonLowercase.matchesAt(this, index)) return false
        return true
    }

    /**
     * @Returns the indexes of all period-space-non-lowercase-known-char sequences in this string.
     *   Pair with the above fun to tell whether there is a sentence-end strictly-inside this
     *   string.
     */
    private fun String.matchPeriodSpaceNonLowercase(): List<Int> {
        // check if there is a sentence-end strictly-inside the tag.body
        var matches = periodSpaceNonLowercase.findAll(this).map { it.range.first }.toList()
        matches = matches.filter { this.containsSentenceBreakAt(it) }
        return matches
    }

    private fun FlowContent.renderTags(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            if (state.terminate) {
                break
            }
            val link = tag.params["href"]?.handleDocRoot()
            val isHtml = tag.params["content-type"] == "html"
            when (tag) {
                is Text ->
                    if (data.summary) {
                        // Remove the MathJax tag. The HTML doesn't need to be injected here because
                        // it
                        // will be injected for the detail component.
                        val text = tag.body.removeMathJax()
                        // If there is a sentence-end, break on the first
                        val matches = text.matchPeriodSpaceNonLowercase()
                        if (matches.isNotEmpty()) {
                            +(text.subSequence(0, matches.minOrNull()!! + 1).toString())
                            state.terminate = true
                        } else if (tag.breaksAtEndOfTag(tags)) { // If this tag is a full sentence
                            +text.trimEnd()
                            state.terminate = true
                        } else if (isHtml) {
                            consumer.onTagContentUnsafe { raw(text.handleDocRoot()) }
                        } else {
                            +text
                        }
                    } else {
                        if (tag.children.isEmpty()) {
                            if (isHtml) {
                                consumer.onTagContentUnsafe { raw(tag.body.handleDocRoot()) }
                            } else if (tag.body.contains(mathJaxDocTag)) {
                                // Remove the MathJax doc tag and inject HTML to enable MathJax.
                                consumer.onTagContentUnsafe { raw(mathJaxHtml) }
                                +tag.body.removeMathJax()
                            } else {
                                +tag.body
                            }
                        } else {
                            if (link == null) {
                                renderTags(tag.children, state)
                            } else {
                                a(link) { +tag.body }
                            }
                        }
                    }
                is P -> {
                    p { renderTags(tag.children, state) }
                    // Sometimes developers forget periods. Max one paragraph per sentence though.
                    if (data.summary) state.terminate = true
                }
                is A -> a(link) { renderTags(tag.children, state) }
                is B,
                is Strong -> b { renderTags(tag.children, state) }
                Br -> br { renderTags(tag.children, state) }
                is H1 ->
                    kotlinx.html.H1(tag.params, consumer).visit { renderTags(tag.children, state) }
                is H2 ->
                    kotlinx.html.H2(tag.params, consumer).visit { renderTags(tag.children, state) }
                is H3 ->
                    kotlinx.html.H3(tag.params, consumer).visit { renderTags(tag.children, state) }
                is H4 ->
                    kotlinx.html.H4(tag.params, consumer).visit { renderTags(tag.children, state) }
                is H5 ->
                    kotlinx.html.H5(tag.params, consumer).visit { renderTags(tag.children, state) }
                is H6 ->
                    kotlinx.html.H6(tag.params, consumer).visit { renderTags(tag.children, state) }
                is I,
                is Em -> em { renderTags(tag.children, state) }
                is Div -> DIV(tag.params, consumer).visit { renderTags(tag.children, state) }
                is Dl -> dl { renderDescriptionList(tag.children, state) }
                is Span -> span { renderTags(tag.children, state) }
                is Strikethrough -> del { renderTags(tag.children, state) }
                is Sub -> sub { renderTags(tag.children, state) }
                is Sup -> sup { renderTags(tag.children, state) }
                is Table -> TABLE(tag.params, consumer).visit { renderTable(tag.children, state) }
                is Ol -> ol { renderOrderedList(tag.children, state) }
                is Ul -> ul { renderUnorderedList(tag.children, state) }
                HorizontalRule -> hr { renderTags(tag.children, state) }
                is CodeInline -> code { renderTags(tag.children, state) }
                // Turn CodeBlock into pre. TODO: guess codeblock-literal's language b/279184834
                is Pre,
                is CodeBlock -> {
                    pre((tag.params["class"] ?: "").addIfNotContained("prettyprint")) {
                        renderTags(tag.children, state)
                    }
                }
                is DocumentationLink ->
                    code {
                        val url = data.pathProvider!!.forReference(tag.dri).url
                        // TODO: improve enforcement/warning for broken links in description
                        // b/192556649
                        a(url) { renderTags(tag.children, state) }
                    }
                is Img ->
                    img(src = tag.params.getValue("href"), alt = tag.params["alt"]) {
                        renderTags(tag.children, state)
                    }
                is BlockQuote -> blockQuote { renderTags(tag.children, state) }
                is CustomDocTag -> {
                    renderTags(tag.children, state)
                }
                is Var -> htmlVar { renderTags(tag.children, state) }
                is Html,
                is Head,
                is Meta,
                is Header,
                is Title,
                is Footer,
                is IFrame,
                is Main,
                is Menu,
                is Nav,
                is Index, ->
                    throw NotImplementedError(
                        "Inline HTML pages are not supported: " +
                            "${tag.javaClass.simpleName}. Context: ${tags.text()}.",
                    )
                is Small,
                is Big,
                is Cite,
                is Dfn,
                is Dir,
                is Font,
                is Frame,
                is FrameSet,
                is Input,
                is Link,
                is Listing,
                is NoFrames,
                is Tt,
                is U,
                is Script,
                is NoScript,
                is Section, ->
                    throw NotImplementedError(
                        "Unknown use case for " +
                            "${tag.javaClass.simpleName}.  Context: ${tags.text()}.",
                    )
                is THead,
                is TBody,
                is Td,
                is TFoot,
                is Th,
                is Tr ->
                    error("Not in table context: ${tag.javaClass.simpleName}.  Context: $tags.")
                is Li ->
                    error(
                        "Not in list context: ${tag.javaClass.simpleName}. The <li> tag " +
                            "must be contained in a parent element (such as <ol>, <ul>, or <menu>). " +
                            "Context: ${tags.text()}.",
                    )
                is Dd,
                is Dt ->
                    error(
                        "Not in list context: ${tag.javaClass.simpleName}. The <dt> or <dd> tag " +
                            "<must be contained in a <dl> element. Context: ${tags.text()}.",
                    )
                is Caption -> TODO("Support this tag")
            }
        }
    }

    private fun String.handleDocRoot(): String {
        return replace("{@docRoot}", "/")
    }

    private fun String.removeMathJax(): String {
        return replace(mathJaxDocTag, "")
    }

    // TODO: remove improper handling of dt b/217941159
    private fun DL.renderDescriptionList(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Dd -> dd { renderTags(tag.children, state) }
                is Dt -> dt { unsafe { +createHTML().p { renderTags(tag.children, state) } } }
                is Dl -> renderTags(listOf(tag), state)
                else -> error("Invalid tag inside of DescriptionList: ${tag.javaClass.simpleName}.")
            }
        }
    }

    private fun TABLE.renderTable(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is THead -> thead { renderTableHeader(tag.children, state) }
                is TBody -> tbody { renderTableBody(tag.children, state) }
                is TFoot -> tfoot { renderTableFooter(tag.children, state) }
                is Th -> tr { renderTableRow(tag.children, isHeader = true, state) }
                is Tr -> tr { renderTableRow(tag.children, isHeader = false, state) }
                is Caption -> caption { renderTags(tag.children, state) }
                else ->
                    error(
                        "Invalid tag inside of Table: ${tag.javaClass.simpleName}. " +
                            "Context: ${tags.text()}.",
                    )
            }
        }
    }

    private fun THEAD.renderTableHeader(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = true, state) }
                else ->
                    error(
                        "Invalid tag inside of TableHeader: ${tag.javaClass.simpleName}. " +
                            "Context: ${tags.text()}.",
                    )
            }
        }
    }

    private fun TBODY.renderTableBody(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = false, state) }
                else ->
                    error(
                        "Invalid tag inside of TableBody: ${tag.javaClass.simpleName}. " +
                            "Context: ${tags.text()}.",
                    )
            }
        }
    }

    private fun TFOOT.renderTableFooter(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Tr -> tr { renderTableRow(tag.children, isHeader = false, state) }
                else ->
                    error(
                        "Invalid tag inside of TableFooter: ${tag.javaClass.simpleName}. " +
                            "Context: ${tags.text()}.",
                    )
            }
        }
    }

    private fun TR.renderTableRow(tags: List<DocTag>, isHeader: Boolean, state: State) {
        for (tag in tags) {
            when (tag) {
                is Td -> td { renderTags(tag.children, state) }
                is P ->
                    if (isHeader) {
                        // KotlinX.HTML seems broken here: we can't render paragraphs or divs
                        // TODO(b/164125463): Figure out how to add other elements
                        th { +(tag.children.single() as Text).body }
                    } else {
                        td { renderTags(tag.children, state) }
                    }
                // <th> is being converted to Text class
                // TODO(b/193096057): determine root cause
                is Text -> th { +tag.body }
                else ->
                    error(
                        "Invalid tag inside of TableRow: ${tag.javaClass.simpleName}. " +
                            "Context: ${tags.text()}.",
                    )
            }
        }
    }

    private fun OL.renderOrderedList(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Li -> li { renderTags(tag.children, state) }
                is Ol,
                is Ul -> renderTags(listOf(tag), state)
                else ->
                    error(
                        "Invalid tag inside of OrderedList: ${tag.javaClass.simpleName}. " +
                            "Context: ${tags.text()}.",
                    )
            }
        }
    }

    private fun UL.renderUnorderedList(tags: List<DocTag>, state: State) {
        for (tag in tags) {
            when (tag) {
                is Li -> li { renderTags(tag.children, state) }
                is Ol,
                is Ul -> renderTags(listOf(tag), state)
                else ->
                    error(
                        "Invalid tag inside of UnorderedList: ${tag.javaClass.simpleName}. " +
                            "Context: ${tags.text()}.",
                    )
            }
        }
    }

    /**
     * Mutable state holder for tag rendering. We aren't using fields to preserve thread safety and
     * idempotency of the component.
     */
    private class State(var terminate: Boolean = false)

    /** Function for printing context */
    fun List<DocTag>.text(separator: String = ", "): String =
        joinToString(separator) {
            when (it) {
                is DocumentationLink -> it.dri.toString() + it.children.text()
                is CustomDocTag -> it.name + it.children.text()
                is Text -> it.body + it.children.text()
                else -> it.children.text()
            }
        }

    override fun toString() =
        if (data.summary) {
            "summary of "
        } else
            "" +
                if (data.deprecation != null) {
                    data.deprecation + " "
                } else "" + data.components.joinToString { it.toString() }
}

private fun String.addIfNotContained(addend: String) = if (addend in this) this else this + addend
