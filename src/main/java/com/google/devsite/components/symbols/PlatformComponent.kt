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
import com.google.devsite.components.symbols.Platform.COMMON
import com.google.devsite.components.symbols.Platform.JS
import com.google.devsite.components.symbols.Platform.JVM
import com.google.devsite.components.symbols.Platform.NATIVE
import com.google.devsite.components.symbols.Platform.WASM
import kotlinx.html.FlowContent
import kotlinx.html.span
import org.jetbrains.dokka.Platform as DokkaPlatform

/** Represents a platform. */
internal interface PlatformComponent : ContextFreeComponent {
    val data: Params

    data class Params(
        val platforms: Set<Platform>,
    )

    fun renderForDetail(into: FlowContent)
}

/**
 * Converts a Dokka Platform into a Dackka platform. These dackka platforms are what can be used to
 * filter functions by sourceSet on dac
 */
enum class Platform {
    // The order in which these constants appear here is the order in which they are sorted
    COMMON,
    JVM,
    NATIVE,
    WASM,
    JS,
    ;

    /* Not used yet; strategy undecided
    ANDROID,
    IOS
    // Things to consider:
    ANDROID_JVM
    ANDROID_NATIVE
     */

    companion object {
        fun from(analysisPlatform: DokkaPlatform): Platform {
            return when (analysisPlatform) {
                DokkaPlatform.jvm -> JVM
                DokkaPlatform.js -> JS
                DokkaPlatform.native -> NATIVE
                DokkaPlatform.common -> COMMON
                DokkaPlatform.wasm -> WASM
            }
        }
    }
}

/** Returns the shortname displayed inline with documentables */
fun Platform.shortName() =
    when (this) {
        JS -> "JS"
        NATIVE -> "N"
        JVM -> "A"
        COMMON -> "Cmn"
        WASM -> "WASM"
    }

fun Platform.render(into: FlowContent) =
    into.run {
        when (this@render) {
            WASM,
            JS,
            NATIVE,
            COMMON -> +shortName()
            JVM -> span(classes = "material-symbols-outlined") { +"android" }
        }
    }

fun Platform.devsiteId() = "platform-${selectorDisplayName()}"

/** Returns the display name for the platform that is used in the dropdown selector */
fun Platform.selectorDisplayName() =
    when (this) {
        JS -> "JavaScript"
        WASM -> "Web Assembly"
        NATIVE -> "Native/C/iOS"
        JVM -> "Android/JVM"
        COMMON -> "Common/All"
    }

// Docs:
// https://github.com/Kotlin/dokka/blob/master/plugins/base/src/main/kotlin/translators/documentables/DefaultPageCreator.kt
// This class has a number of places that it does things by platform that we don't currently support
// Such as: See tags, Descriptions,  Params, and Throws. TODO(b/254490320)
