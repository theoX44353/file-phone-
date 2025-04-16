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

import com.google.devsite.components.symbols.MetadataComponent
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.id

/** Default implementation of a MetadataComponent. */
internal data class DefaultMetadataComponent(
    override val data: MetadataComponent.Params,
) : MetadataComponent {

    override fun render(into: FlowContent): Unit =
        into.run {
            // Only renders the div when there will be something inside it
            if (!data.isEmpty) {
                div {
                    // CSS id declared in internal codebase (cl/485657528)
                    id = "metadata-info-block"
                    data.libraryMetadata?.let {
                        div {
                            id = "maven-coordinates"
                            +"Artifact: "
                            it.link.render(this)
                        }
                    }
                    data.sourceLink?.let {
                        div {
                            id = "source-link"
                            it.render(this)
                        }
                    }
                    data.versionMetadata?.render(this)
                }
            }
        }

    override fun toString() =
        "Metadata:" +
            data.libraryMetadata?.link?.let { " Release Notes URL: $it" }.orEmpty() +
            data.sourceLinkUrl?.let { " Source Link URL: $it" }.orEmpty() +
            data.versionMetadata?.let { " Version metadata: $it" }.orEmpty()
}
