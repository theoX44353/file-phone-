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

package com.google.devsite.renderer.converters

import com.google.devsite.renderer.Language
import kotlin.math.min
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DefinitelyNonNullable
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Variance
import org.jetbrains.dokka.model.Void

/**
 * What we know about a component's nullability
 *
 * Note: this is unrelated to the source language. Rather, the source will be make to look like the
 * rendering language.
 *
 * Note: java source can have KotlinNullable components, e.g. a type parameter bound
 */
enum class Nullability {
    KOTLIN_NULLABLE,
    KOTLIN_DEFAULT,
    JAVA_ANNOTATED_NOT_NULL,
    JAVA_ANNOTATED_NULLABLE,
    JAVA_NEVER_NULL, // This refers to types that cannot be nullable, e.g. int
    JAVA_NOT_ANNOTATED, // This means "platform type," which mostly means nullable
    DONT_CARE, // Sometimes we don't print nullability, usually if it should be obvious from context
    ;

    fun renderAsJavaAnnotation() =
        when (this) {
            // DO NOT inject @Nullable. Even if it would be correct, it would not be useful to Java
            // clients.
            KOTLIN_NULLABLE,
            JAVA_ANNOTATED_NULLABLE -> null //
            KOTLIN_DEFAULT,
            JAVA_ANNOTATED_NOT_NULL -> AT_NON_NULL // @NonNull
            JAVA_NOT_ANNOTATED -> null
            JAVA_NEVER_NULL -> null // `int` does not need a nullability annotation
            DONT_CARE -> null
        }

    fun renderAsKotlinSuffix() =
        when (this) {
            KOTLIN_NULLABLE,
            JAVA_ANNOTATED_NULLABLE -> "?"
            KOTLIN_DEFAULT,
            JAVA_ANNOTATED_NOT_NULL,
            JAVA_NEVER_NULL, -> ""
            JAVA_NOT_ANNOTATED -> "!"
            DONT_CARE -> ""
        }

    private fun isNullable() =
        when (this) {
            KOTLIN_NULLABLE,
            JAVA_ANNOTATED_NULLABLE,
            JAVA_NOT_ANNOTATED,
            DONT_CARE, -> true
            KOTLIN_DEFAULT,
            JAVA_ANNOTATED_NOT_NULL,
            JAVA_NEVER_NULL, -> false
        }

    val nullable
        get() = this.isNullable()

    infix fun or(other: Nullability?): Nullability =
        if (other == null) {
            this
        } else {
            NULLABILITY_PRECEDENCE_LIST[
                min(
                    NULLABILITY_PRECEDENCE_LIST.indexOf(this),
                    NULLABILITY_PRECEDENCE_LIST.indexOf(other),
                ),
            ]
        }

    companion object {
        internal val NULLABILITY_PRECEDENCE_LIST =
            listOf(
                DONT_CARE,
                JAVA_NEVER_NULL,
                KOTLIN_NULLABLE,
                JAVA_ANNOTATED_NULLABLE,
                JAVA_ANNOTATED_NOT_NULL,
                KOTLIN_DEFAULT,
                JAVA_NOT_ANNOTATED,
            )
    }
}

/**
 * @return true if this is a nullable type, false otherwise
 *
 * Re: KMP: Kotlin enforces that the nullability of `actual`s matches the nullability of `expect`s,
 * as part of enforcing that the type of `actual`s and `expect`s match. HOWEVER, it does not do this
 * for `actual typealias`es into java code, because if they did that they wouldn't be able to have
 * the `actual typealias`es they want in kotlin standard lib, like `RuntimeException`. Here, we are
 * ignoring that error, and claiming the nullability of the `expect` as canonical.
 */
internal fun Projection.getNullability(
    displayLanguage: Language,
    isJavaSource: Boolean? = null,
    injectedAnnotations: List<Annotations.Annotation> = emptyList(),
): Nullability {
    // TODO: hoist this into the renderer, it's what should know and care about displayLanguage
    if (!this.typeIsNullableAtAll(displayLanguage = displayLanguage)) {
        return Nullability.DONT_CARE // This can overwrite annotations (could have been propagated)
    }

    return when (this) {
        is Nullable -> Nullability.KOTLIN_NULLABLE
        is DefinitelyNonNullable -> Nullability.KOTLIN_DEFAULT
        is Variance<*> -> inner.getNullability(displayLanguage, isJavaSource)
        is TypeAliased -> inner.getNullability(displayLanguage, isJavaSource)
        Void -> Nullability.JAVA_NEVER_NULL // Not nullable by definition
        Dynamic,
        Star -> Nullability.KOTLIN_DEFAULT // Can come from Kotlin source only
        // Unannotated java projections are nullable, default Kotlin aren't
        is TypeParameter,
        is TypeConstructor,
        is JavaObject,
        is UnresolvedBound,
        is PrimitiveJavaType, -> {
            // Java arrays are nullable; non-array primitives aren't
            if (this is PrimitiveJavaType) if ("[" !in name) Nullability.JAVA_NEVER_NULL
            // This is the only case where annotations can override the normal nullability
            val annotations = injectedAnnotations + sourceSetIndependentAnnotations()

            // We hide nullability annotations on Kotlin docs even if they were explicit in Kotlin
            // source. This is highly opinionated. As such, we throw a warningto make this explicit.
            /*if (isJavaSource == false && (this is TypeParameter || this is TypeConstructor) &&
                (allAnnotations.hasAtNullable() || allAnnotations.hasAtNonNull())) {
                val name = if (this is TypeParameter) this.name
                    else (this as TypeConstructor).presentableName
                println("WARN: Java nullability annotation on Kotlin-source $name. Context: $this")
            }*/
            // WARNING: Kotlin primitives that come from Java sources could have started out as
            // non-nullable java primitives, or as nullable java boxed primitives. There is no way
            // to tell, so we conservatively default to JAVA_NOT_ANNOTATED. b/234132128
            /* if (isJavaSource == true && this is TypeConstructor &&
                this.dri.packageName == "kotlin" && this.dri.classNames in kotlinPrimitives
            ) return Nullability.JAVA_NEVER_NULL */
            annotations.inferNullability()?.let {
                return@getNullability it
            }
            // If there are no nullability annotations:
            defaultNullability(isJavaSource)
        }
    }
}

internal fun List<Annotations.Annotation>.inferNullability(): Nullability? {
    if (hasAtNullable()) return Nullability.JAVA_ANNOTATED_NULLABLE
    if (hasAtNonNull()) return Nullability.JAVA_ANNOTATED_NOT_NULL
    return null
}

internal val Language.defaultNullability
    get() =
        when (this) {
            Language.KOTLIN -> Nullability.KOTLIN_DEFAULT
            Language.JAVA -> Nullability.JAVA_NOT_ANNOTATED
        }

internal fun defaultNullability(isFromJava: Boolean?) =
    when (isFromJava) {
        true -> Nullability.JAVA_NOT_ANNOTATED
        false -> Nullability.KOTLIN_DEFAULT
        null -> Nullability.JAVA_NOT_ANNOTATED // default
    }

internal fun Projection.typeIsNullableAtAll(displayLanguage: Language) =
    when (this) {
        is TypeConstructor ->
            // It's possible to explicitly declare a Kotlin Int?, but in Java this TODO add test
            if (dri.packageName == "kotlin" && displayLanguage == Language.JAVA) {
                val className = dri.classNames.orEmpty()
                when {
                    // kotlin types we convert to java primitives don't get nullability
                    (className in ParameterDocumentableConverter.kotlinPrimitives) -> false
                    // Nothing is converted to void, so nullability isn't useful
                    (className == "Nothing") -> false
                    // Unit can be nullable, but that information is basically always useless
                    (className == "Unit") -> false
                    else -> true
                }
            } else {
                true
            }
        is Void -> false
        is PrimitiveJavaType -> "[" in this.name
        else -> true
    } 
