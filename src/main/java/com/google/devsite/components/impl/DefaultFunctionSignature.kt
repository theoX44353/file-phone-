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
import com.google.devsite.components.shouldBreak
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.joinMaybePrefix
import kotlinx.html.FlowContent
import kotlinx.html.span
import kotlinx.html.unsafe

/** Default implementation of a function signature. */
internal data class DefaultFunctionSignature(
    override val data: FunctionSignature.Params,
) : FunctionSignature {
    override fun render(into: FlowContent) =
        into.run {
            data.typeParameters.render(this, brackets = "<>", shouldBreak = ShouldBreak.NO)
            if (data.typeParameters.isNotEmpty()) +" "

            if (data.receiver != null) {
                data.receiver.render(this)
                +"."
            }

            if (data.isDeprecated) {
                // Bug in kotlinx: <del> tag adds a new line before and after using it
                // https://github.com/Kotlin/kotlinx.html/issues/113
                // Manually declare <del> instead
                span {
                    unsafe { +"<del>" }
                    data.name.render(into)
                    unsafe { +"</del>" }
                }
            } else {
                data.name.render(this)
            }
            data.parameters.render(
                into,
                this@DefaultFunctionSignature.shouldBreak(),
                brackets = "()",
            )
        }

    override fun toString() =
        data.typeParameters.joinMaybePrefix(postfix = " ") +
            if (data.receiver != null) {
                data.receiver.toString() + "."
            } else
                "" +
                    if (data.isDeprecated) {
                        "deprecated "
                    } else
                        "" +
                            data.name +
                            data.parameters.joinMaybePrefix(prefix = "(", postfix = ")")
}
