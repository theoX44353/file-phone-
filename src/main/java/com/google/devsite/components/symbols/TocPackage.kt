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

import com.google.devsite.components.TextComponent

/** Represents a package section in the Devsite _toc.yaml. */
internal interface TocPackage : TextComponent {
    val data: Params

    data class Params(
        val name: String,
        val packageUrl: String,
        val interfaces: List<Type> = emptyList(),
        val classes: List<Type> = emptyList(),
        val enums: List<Type> = emptyList(),
        val exceptions: List<Type> = emptyList(),
        val annotations: List<Type> = emptyList(),
        val objects: List<Type> = emptyList(),
    ) // Typealiases do not appear in the toc because they do not get their own pages

    data class Type(val name: String, val url: String)
}
