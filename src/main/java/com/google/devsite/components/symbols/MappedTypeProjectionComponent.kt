package com.google.devsite.components.symbols

import com.google.devsite.components.Link
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Nullability

internal interface MappedTypeProjectionComponent : TypeProjectionComponent {
    override val data: Params

    override fun length() = super.length() + "()".length + data.alternativePrefix.length()

    data class Params(
        override val type: Link,
        val alternativePrefix: Link,
        override val annotationComponents: List<AnnotationComponent> = emptyList(),
        override val nullability: Nullability,
        override val generics: List<TypeProjectionComponent> = emptyList(),
    ) :
        TypeProjectionComponent.Params(
            type = type,
            nullability = nullability,
            displayLanguage = Language.KOTLIN,
            generics = generics,
            annotationComponents = annotationComponents,
        )
}
