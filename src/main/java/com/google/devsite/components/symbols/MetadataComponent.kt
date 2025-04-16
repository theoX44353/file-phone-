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

package com.google.devsite.components.symbols

import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.util.LibraryMetadata

/** Represents the page's metadata section. */
internal interface MetadataComponent : ContextFreeComponent {
    val data: Params

    data class Params(
        val libraryMetadata: LibraryMetadata?,
        val sourceLinkUrl: String?,
        val versionMetadata: VersionMetadataComponent?,
    ) {
        internal val sourceLink: Link?
            get() =
                sourceLinkUrl?.let {
                    DefaultLink(Link.Params(name = "View Source", url = it, externalLink = true))
                }

        internal val isEmpty: Boolean =
            libraryMetadata == null && sourceLinkUrl == null && versionMetadata == null
    }
}
