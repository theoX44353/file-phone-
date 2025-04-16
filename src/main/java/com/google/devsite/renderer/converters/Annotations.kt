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

package com.google.devsite.renderer.converters

import com.google.devsite.strictSingleOrNull
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.AnnotationParameterValue
import org.jetbrains.dokka.model.AnnotationValue
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Annotations.Annotation
import org.jetbrains.dokka.model.ArrayValue
import org.jetbrains.dokka.model.Bound
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.dokka.model.DefinitelyNonNullable
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.EnumValue
import org.jetbrains.dokka.model.FunctionalTypeConstructor
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.LiteralValue
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Void
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal val Annotation.identifier: String
    get() = "${dri.fullName}(${params.values.map { "$it" }})"

internal val AT_NULLABLE = Annotation(DRI("androidx.annotation", "Nullable"), emptyMap())
internal val AT_NON_NULL = Annotation(DRI("androidx.annotation", "NonNull"), emptyMap())
internal val ANDROID_NULLABLE_DRI = DRI("android.annotation", "Nullable")
internal val ANDROID_NON_NULL_DRI = DRI("android.annotation", "NonNull")

/** @return true if an `@Nullable` annotation is present, false otherwise */
internal fun List<Annotation>.hasAtNullable(): Boolean = any { it.dri.classNames == "Nullable" }

/** @return true if an `@NonNull` annotation is present, false otherwise */
internal fun List<Annotation>.hasAtNonNull(): Boolean = any {
    it.dri.classNames in listOf("NonNull", "NotNull")
}

/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun List<Annotation>.isDeprecated(): Boolean = any { it.isDeprecated() }

/** @return the complete list of annotations for this type */
private fun WithExtraProperties<*>.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet?) =
    extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.directAnnotations[sourceSet] ?: emptyList()
    }

internal fun Documentable.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet?) =
    (this as? WithExtraProperties<*>)?.annotations(sourceSet) ?: emptyList()

internal fun Projection.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet?) =
    (this as? Bound)?.annotations(sourceSet)
        ?: (this as? WithExtraProperties<*>)?.annotations(sourceSet)
        ?: emptyList()

internal fun Projection.sourceSetIndependentAnnotations(): List<Annotation> =
    (this as? WithExtraProperties<*>)?.sourceSetIndependentAnnotations() ?: emptyList()

internal fun WithExtraProperties<*>.sourceSetIndependentAnnotations(): List<Annotation> =
    extra.allOfType<Annotations>().strictSingleOrNull()?.directAnnotations?.values?.firstOrNull()
        ?: emptyList()

private fun Bound.annotations(sourceSet: DokkaConfiguration.DokkaSourceSet?): List<Annotation> =
    when (this) {
        is TypeParameter,
        is GenericTypeConstructor,
        is FunctionalTypeConstructor -> (this as WithExtraProperties<*>).annotations(sourceSet)
        is Nullable -> this.inner.annotations(sourceSet)
        is TypeAliased -> this.inner.annotations(sourceSet)
        is PrimitiveJavaType,
        Void,
        is JavaObject,
        Dynamic,
        is UnresolvedBound -> emptyList()
        is DefinitelyNonNullable -> this.inner.annotations(sourceSet).filter { it != AT_NULLABLE }
    }

/** @return all annotations on this element, in any sourceSet, including file-level ones */
internal fun Documentable.allAnnotations() =
    (this as? WithExtraProperties<*>)?.extra?.allOfType<Annotations>()?.flatMap { annotations ->
        annotations.directAnnotations.flatMap { it.value } +
            annotations.fileLevelAnnotations.flatMap { it.value }
    } ?: emptyList()

// TODO(KMP per-sourceset variance of deprecation status b/262711247)
internal fun Documentable.deprecationAnnotation() = allAnnotations().deprecationAnnotation()

internal fun List<Annotation>.deprecationAnnotation() =
    toSet().filter { it.isDeprecated() }.strictSingleOrNull()

/**
 * All existing WithSources are WithExtraProperties, and fileLevelAnnotations require sources.
 *
 * @return the list of file-level annotations on this WithSource's source file
 */
internal fun <T> T.fileLevelAnnotations(sourceSet: DokkaConfiguration.DokkaSourceSet?) where
T : WithSources,
T : Documentable =
    (this as WithExtraProperties<*>).extra.allOfType<Annotations>().flatMap { annotations ->
        annotations.fileLevelAnnotations[sourceSet] ?: emptyList()
    }

/** @return true if the `@Deprecated` annotation is present, false otherwise */
internal fun Annotation.isDeprecated(): Boolean = dri == deprecatedDri || dri == javaDeprecatedDri

internal val deprecatedDri = DRI(packageName = "kotlin", classNames = "Deprecated")
internal val javaDeprecatedDri = DRI(packageName = "java.lang", classNames = "Deprecated")

internal fun Annotation.belongsOnReturnType() =
    dri.classNames in NULLABILITY_ANNOTATION_NAMES || dri.classNames?.shouldBeTypebound() ?: false

private val SUPPRESSION_ANNOTATION_NAMES = listOf("Suppress", "SuppressWarnings", "SuppressLint")

internal fun Annotation.isSuppressAnnotation() = dri.classNames in SUPPRESSION_ANNOTATION_NAMES

// We transform javax.validation.constraints.NotNull into androidx.annotation.NonNull and WARN:
internal val NULLABILITY_ANNOTATION_NAMES = listOf("NonNull", "Nullable", "NotNull")

// List of androidx annotations that (now that we are on Java 8) ideally would be migrated
// ANNOTATION_TARGET.METHOD -> ANNOTATION_TARGET.TYPE. If on a function, they refer to return type
private val KNOWN_TYPEBOUND_ANNOTATION_NAMES = listOf("Dimension", "Px", "Size")

// For androidx annotations. E.g. IntRes, IntRange, GravityInt, HalfFloat, ColorLong, UiContext
private val KNOWN_TYPEBOUND_ANNOTATION_SUFFIXES =
    listOf("Res", "Range", "Long", "Int", "Float", "Context")

private fun String.shouldBeTypebound() =
    finalWord() in KNOWN_TYPEBOUND_ANNOTATION_SUFFIXES || this in KNOWN_TYPEBOUND_ANNOTATION_NAMES

private fun String.finalWord(): String {
    val lastIndexOfCapital = lastOrNull { it.isUpperCase() }?.let { indexOf(it) } ?: 0
    return substring(lastIndexOfCapital)
}

internal fun AnnotationParameterValue?.asString() =
    when (this) {
        null -> ""
        is StringValue -> value
        is EnumValue -> enumName
        is ClassValue -> className
        is LiteralValue -> text()
        is AnnotationValue -> annotation.toString()
        is ArrayValue -> value.toString()
    }

internal fun Annotation.nameAsString(): String = params["name"].asString()
