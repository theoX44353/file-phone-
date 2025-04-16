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
import com.google.devsite.components.nobr
import com.google.devsite.components.render
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.joinMaybePrefix
import kotlinx.html.Entities
import kotlinx.html.FlowContent

/** Default implementation of a function parameter type. */
internal data class DefaultLambdaTypeProjectionComponent(
    override val data: LambdaTypeProjectionComponent.Params,
) : LambdaTypeProjectionComponent {
    override fun render(into: FlowContent) =
        into.run {
            if (data.nullability.nullable) +"("
            data.annotationComponents.render(this, separator = "", terminator = { +" " })
            data.lambdaModifiers.render(this, terminator = { +Entities.nbsp })
            if (data.receiver != null) {
                data.receiver.render(this)
                +"."
            }
            data.lambdaParams.render(this, brackets = "()")
            +" "
            nobr { +"->" }
            +" "
            data.returnType.render(this)
            // Render any generics on the return type
            data.generics.render(into, ShouldBreak.NO, brackets = "<>")
            if (data.nullability.nullable) +")?"
        }

    override fun toString(): String {
        val result =
            data.annotationComponents.joinToString() +
                data.lambdaModifiers.joinToString() +
                if (data.receiver != null) {
                    "${data.receiver}."
                } else
                    "" +
                        data.lambdaParams.joinMaybePrefix(prefix = "(", postfix = ")") +
                        " -> " +
                        data.returnType +
                        data.generics.joinMaybePrefix(prefix = "<", postfix = ">")

        return if (data.nullability.nullable) "($result)?" else result
    }
}
