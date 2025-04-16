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

import com.google.devsite.components.symbols.Platform
import com.google.devsite.components.symbols.PlatformComponent
import com.google.devsite.components.symbols.render
import com.google.devsite.components.symbols.selectorDisplayName
import kotlinx.html.FlowContent
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.span
import org.jetbrains.dokka.DokkaConfiguration

private val FIXED_INSTANCES = mutableMapOf<PlatformComponent.Params, DefaultPlatformComponent>()

internal fun DefaultPlatformComponent(sourceSets: Set<DokkaConfiguration.DokkaSourceSet>) =
    with(sourceSets.map { Platform.from(it.analysisPlatform) }.toSortedSet()) {
        FIXED_INSTANCES.getOrPut(PlatformComponent.Params(platforms = this)) {
            DefaultPlatformComponent(PlatformComponent.Params(platforms = this))
        }
    }

/** Default implementation of a table header. */
internal data class DefaultPlatformComponent(
    override val data: PlatformComponent.Params,
) : PlatformComponent {

    override fun render(into: FlowContent) =
        into.run {
            data.platforms.forEach {
                div {
                    // Hooks up to devsite/android/en/assets/css/reference-docs.css
                    classes = setOf("kotlin-platform")
                    attributes["data-title"] = it.selectorDisplayName()
                    it.render(into)
                }
                comment("platform-${it.selectorDisplayName()}")
            }
        }

    override fun renderForDetail(into: FlowContent) =
        into.run {
            data.platforms.forEach {
                span {
                    // TODO(improve detail section display, e.g. move to right-aligned)
                    classes = setOf("kotlin-platform")
                    attributes["data-title"] = it.selectorDisplayName()
                    it.render(into)
                }
                comment("platform-${it.selectorDisplayName()}")
            }
        }

    override fun toString() = data.platforms.toString()
}
