/*
 * Copyright 2022 The Android Open Source Project
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

import com.google.devsite.components.impl.DefaultAnnotationComponent
import com.google.devsite.components.impl.DefaultAnnotationValueAnnotationParameter
import com.google.devsite.components.impl.DefaultArrayValueAnnotationParameter
import com.google.devsite.components.impl.DefaultNamedValueAnnotationParameter
import com.google.devsite.components.symbols.AnnotationComponent
import com.google.devsite.components.symbols.AnnotationParameter
import com.google.devsite.components.symbols.AnnotationValueAnnotationParameter
import com.google.devsite.components.symbols.ArrayValueAnnotationParameter
import com.google.devsite.components.symbols.NamedValueAnnotationParameter
import com.google.devsite.defaultValidNullabilityAnnotations
import com.google.devsite.hasBeenHidden
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.FilePathProvider
import org.jetbrains.dokka.model.AnnotationParameterValue
import org.jetbrains.dokka.model.AnnotationValue
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.ArrayValue
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.dokka.model.EnumValue
import org.jetbrains.dokka.model.LiteralValue
import org.jetbrains.dokka.model.StringValue

/** Converts annotations into their components. */
internal class AnnotationDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val annotationsNotToDisplay: Set<String>,
    // Default value is provided for testing purposes only. The only real instantiation provides it.
    private val validNullabilityAnnotations: List<String> = defaultValidNullabilityAnnotations,
) {
    /**
     * @param nullability the nullability of the annotated element. Contains information such as
     *   source language and whether we care about the nullability of the annotated element.
     * @return the AnnotationComponents for the given annotations on the annotated element
     */
    fun annotationComponents(
        annotations: List<Annotations.Annotation>,
        nullability: Nullability,
    ): List<AnnotationComponent> {
        // Convert android.nullable to androidx., because those are the public versions
        @Suppress("NAME_SHADOWING") val annotations = annotations.map { it.fixNullability() }

        val injectedAnnotations = mutableListOf<Annotations.Annotation>()
        if (annotations.any { it.isBadNullability }) {
            throw RuntimeException(
                "Used a nullability annotation ${annotations.filter { it.isBadNullability }} not " +
                    "in the list of validNullabilityAnnotations passed to dackka.",
            )
        }

        // NOTE: we inject @NonNull, but not @Nullable, as that is usually not useful to Java devs
        if (displayLanguage == Language.JAVA) {
            if (!annotations.any { it.isNullabilityAnnotation }) {
                nullability.renderAsJavaAnnotation()?.let { injectedAnnotations += it }
            }
        }

        return (annotations + injectedAnnotations)
            .filter { annotation -> shouldDocumentAnnotation(annotation, nullability) }
            .distinctBy { it.identifier }
            .map { annotation -> annotation.toDackkaAnnotation() }
    }

    /** @return true if a developer would find this annotation useful, false otherwise */
    private fun shouldDocumentAnnotation(
        annotation: Annotations.Annotation,
        nullability: Nullability,
    ): Boolean {
        // Not useful to developers
        if (
            annotation.isSuppressAnnotation() ||
                annotation.dri.packageName == "kotlin.jvm" ||
                annotation.dri.fullName in annotationsNotToDisplay
        ) {
            return false
        }
        // Surfaced separately
        if (annotation.isDeprecated()) return false

        if (annotation.dri.classNames in NULLABILITY_ANNOTATION_NAMES) {
            // Explicitly hidden nullability annotations
            if (nullability == Nullability.DONT_CARE) return false
            // Nullability annotations do not appear in Kotlin, even if explicit in Kotlin source
            if (displayLanguage == Language.KOTLIN) return false
        }
       
      return !hasBeenHidden(annotation.dri)
    }

    private fun Annotations.Annotation.toDackkaAnnotation(): AnnotationComponent {
        val type = pathProvider.linkForReference(dri)
        val params = params.map { (name, contents) -> contents.toComponent(name) }
        return DefaultAnnotationComponent(AnnotationComponent.Params(type, params))
    }

    private fun AnnotationParameterValue.toComponent(
        name: String? = null,
    ): AnnotationParameter =
        when (this) {
            is StringValue ->
                DefaultNamedValueAnnotationParameter(
                    NamedValueAnnotationParameter.Params(name, "\"${asString()}\""),
                )
            is LiteralValue,
            is EnumValue,
            is ClassValue ->
                DefaultNamedValueAnnotationParameter(
                    NamedValueAnnotationParameter.Params(name, asString()),
                )
            is ArrayValue ->
                DefaultArrayValueAnnotationParameter(
                    ArrayValueAnnotationParameter.Params(
                        name,
                        innerAnnotationParameters = value.map { it.toComponent() },
                    ),
                )
            is AnnotationValue ->
                DefaultAnnotationValueAnnotationParameter(
                    AnnotationValueAnnotationParameter.Params(
                        name,
                        annotationComponentValue = annotation.toDackkaAnnotation(),
                    ),
                )
        }

    private val Annotations.Annotation.isNullabilityAnnotation
        get() = dri.classNames in listOf("Nullable", "NonNull", "NotNull")

    private val Annotations.Annotation.isBadNullability
        get() = isNullabilityAnnotation && dri.fullName !in validNullabilityAnnotations

    private fun Annotations.Annotation.fixNullability() =
        when (this.dri) {
            ANDROID_NULLABLE_DRI -> AT_NULLABLE
            ANDROID_NON_NULL_DRI -> AT_NON_NULL
            else -> this
        }
}
