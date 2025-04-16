/*
 * Copyright 2021 The Android Open Source Project
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
import com.google.devsite.components.Sizeable

/** Base type for annotation parameters. Necessary because there are several different types. */
internal interface AnnotationParameter : ContextFreeComponent, Sizeable

internal interface NamedValueAnnotationParameter : AnnotationParameter {
    val data: Params

    override fun length(): Int {
        val nameSize = (data.name?.length?.plus(3)) ?: 0 // " = "
        val valueSize = data.value.length
        return nameSize + valueSize
    }

    // Name is null if inside an array, in which case only the array is named
    data class Params(val name: String?, val value: String)
}

internal interface AnnotationValueAnnotationParameter : AnnotationParameter {
    val data: Params

    override fun length(): Int {
        val nameSize = (data.name?.length?.plus(3)) ?: 0 // " = "
        val valueSize = data.annotationComponentValue.length()
        return nameSize + valueSize
    }

    data class Params(val name: String?, val annotationComponentValue: AnnotationComponent)
}

internal interface ArrayValueAnnotationParameter : AnnotationParameter {
    val data: Params

    override fun length(): Int {
        val nameSize = (data.name?.length?.plus(3)) ?: 0 // " = "
        val valueSize = data.innerAnnotationParameters.sumOf { it.length() }
        return nameSize + valueSize
    }

    data class Params(val name: String?, val innerAnnotationParameters: List<AnnotationParameter>)
}

internal val AnnotationParameter.name: String
    get() =
        when (this) {
            is AnnotationValueAnnotationParameter -> this.data.name!!
            is ArrayValueAnnotationParameter -> this.data.name!!
            is NamedValueAnnotationParameter -> this.data.name!!
            else -> throw RuntimeException("impossible subtype of AnnotationParameter")
        }

/** This is expected to be used as e.g. assertThat(parameter.value).isEqualTo("a string") */
internal val AnnotationParameter.value: Any
    get() =
        when (this) {
            is AnnotationValueAnnotationParameter -> this.data.annotationComponentValue
            is ArrayValueAnnotationParameter -> this.data.innerAnnotationParameters
            is NamedValueAnnotationParameter -> this.data.value
            else -> throw RuntimeException("impossible subtype of AnnotationParameter")
        }
