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

import com.google.devsite.components.ShouldBreak
import com.google.devsite.components.render
import com.google.devsite.components.symbols.ClasslikeSignature
import com.google.devsite.joinMaybePrefix
import com.google.devsite.renderer.Language
import kotlinx.html.FlowContent

internal data class DefaultClasslikeSignature(
    override val data: ClasslikeSignature.Params,
) : ClasslikeSignature {

    override fun render(into: FlowContent) =
        into.run {
            data.annotationComponents.render(into, ShouldBreak.YES, separator = "")

            (data.modifiers + data.type).render(into, nbsp = false, terminator = { +" " })
            data.name.render(into)

            data.typeParameters.render(into, ShouldBreak.NO, brackets = "<>")

            data.typeAliasEquals?.let {
                +" = "
                it.render(into)
            }

            when (data.displayLanguage) {
                Language.JAVA -> {
                    data.extends.render(into, header = { +" extends " })
                    data.implements.render(into, header = { +" $interfaceInheritsPhrase " })
                }
                Language.KOTLIN -> {
                    (data.extends + data.implements).render(into, header = { +" : " })
                }
            }
        }

    override fun toString() =
        (data.modifiers + data.type + data.name).joinToString(separator = " ") +
            data.typeParameters.joinMaybePrefix(prefix = "<", postfix = ">") +
            if (data.displayLanguage == Language.JAVA) {
                data.extends.joinMaybePrefix(prefix = " extends ") +
                    data.implements.joinMaybePrefix(prefix = interfaceInheritsPhrase)
            } else
                (data.extends + data.implements).joinMaybePrefix(prefix = " : ") +
                    data.typeAliasEquals?.let { "= ${data.typeAliasEquals}" }

    // Classes _implement_ interfaces, but interfaces _extend_ other interfaces
    private val interfaceInheritsPhrase = if (data.type == "interface") "extends" else "implements"
}
