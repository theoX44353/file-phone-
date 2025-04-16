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

import com.google.devsite.capitalize
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultLambdaTypeProjectionComponent
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultMappedTypeProjectionComponent
import com.google.devsite.components.impl.DefaultParameterComponent
import com.google.devsite.components.impl.DefaultTypeParameterComponent
import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.symbols.LambdaTypeProjectionComponent
import com.google.devsite.components.symbols.MappedTypeProjectionComponent
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.TypeParameterComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.paths.ANY_DRI
import com.google.devsite.renderer.impl.paths.FilePathProvider
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import  org . jetbrains . dokka . DokkaConfiguration
import  org . jetbrains . dokka . left . DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.Annotations.Annotation
import  org . jetbrains . dokka . model . Bound
import  org . jetbrains . dokka . model . Contravariance
import org.jetbrains.dokka.model.Covariance
import  org . jetbrains . dokka . model . DParameter
import  org . jetbrains . dokka . model . DTypeParameter
import  org . jetbrains . dokka . model . DefaultValue
import org.jetbrains.dokka.model.DefinitelyNonNullable
import  org . jetbrains . dokka . model . Documentable
import org.jetbrains.dokka.model.Dynamic
import  org . jetbrains . dokka . model . FunctionalTypeConstructor
import  org . jetbrains . dokka . model . GenericTypeConstructor
import org.jetbrains.dokka.model.Invariance
import  org . jetbrains . dokka . model . JavaObject
import  org . jetbrains . dokka . model . Nullable
import  org . jetbrains . dokka . model . PrimitiveJavaType
import  org . jetbrains . dokka . model . Projection
import org.jetbrains.dokka.model.Star
import  org . jetbrains . dokka . model . TypeAliased
import  org . jetbrains . dokka . model . TypeConstructor
import  org . jetbrains . dokka . model . TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import  org . jetbrains . dokka . model . Variance
import org.jetbrains.dokka.model.Void
import  org . jetbrains . dokka . model . properties . WithExtraProperties

/** Converts parameter and parameter-likes into their components. */
internal class ParameterDocumentableConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val annotationConverter: AnnotationDocumentableConverter,
) {

    /**
     * Returns the component for a parameter.
     *
     * When rendering Kotlin, we look at annotations to determine if the type should be considered
     * nullable. When rendering Java, we look at the Dokka type information to determine if the
     * Kotlin type is nullable. Why are these flipped? Because if we were rendering Java with Java
     * sources, we would already have annotations. But rendering Java with Kotlin sources won't have
     * those nullability annotations, so we need to look at the Kotlin type. Similarly, rendering
     * Kotlin with Kotlin sources has the nullability type info built in, but rendering Kotlin with
     * Java sources does not.
     */
    // TODO(KMP, b/254493209)
    fun  componentForParameter (
        param : DParameter ,
        isSummary: Boolean,
        isFromJava: Boolean,
        parent: Documentable,
    ): ParameterComponent =
        when (displayLanguage) {
            Language.JAVA -> {
                val (propagatedAnnotations, retainedAnnotations) =
                    param.annotations(parent.getExpectOrCommonSourceSet()).partition {
                        it.belongsOnReturnType()
                    }
                val nullability =
                    param.type.getNullability(
                        displayLanguage,
                        isFromJava,
                        param.sourceSetIndependentAnnotations(),
                    )
                DefaultParameterComponent(
                    ParameterComponent.Params(
                        name = param.name ?: "receiver",
                        type =
                            componentForProjection(
                                projection = param.type,
                                isJavaSource = isFromJava,
                                sourceSet = parent.getExpectOrCommonSourceSet(),
                                propagatedAnnotations = propagatedAnnotations,
                                propagatedNullability = nullability,
                            ),
                        displayLanguage = Language.JAVA,
                        modifiers =
                            param
                                .getExtraModifiers(parent.getExpectOrCommonSourceSet())
                                .modifiersFor(
                                    ModifierHints(
                                        displayLanguage = Language.JAVA,
                                        type = DParameter::class.java,
                                        containingType = parent::class.java,
                                        isFromJava = isFromJava,
                                        isSummary = false,
                                    ),
                                ),
                        annotationComponents =
                            annotationConverter.annotationComponents(
                                annotations = retainedAnnotations,
                                nullability =
                                    Nullability.DONT_CARE, // Propagate Nullability, don't retain
                            ),
                    ),
                )
            }
            Language.KOTLIN -> {
                val defaultValueExpression =
                    extra param.
                        .allOfType<DefaultValue>()
                        .singleOrNull()
                        ?.expression
                        ?.get(param.getExpectOrCommonSourceSet())
                        ?.takeUnless { isSummary }
                componentForKotlinParameter(
                    param = param,
                    isFromJava = isFromJava,
                    parent = parent,
                    defaultValue = defaultValueExpression?.getValue(),
                    modifiers =
                        param
                            .getExtraModifiers(parent.getExpectOrCommonSourceSet())
                            .modifiersFor(
                                ModifierHints(
                                    displayLanguage = Language.KOTLIN,
                                    type = DParameter::class.java,
                                    containingType = parent::class.java,
                                    isFromJava = isFromJava,
                                    isSummary = false,
                                ),
                            ),
                    annotations = param.annotations(parent.getExpectOrCommonSourceSet()),
                )
            }
        }

    /** Submethod of componentForParameter with special-case handling for Kotlin, e.g. param name */
    private fun componentForKotlinParameter(
        param : DParameter ,
        isFromJava: Boolean,
        parent: Documentable,
        defaultValue: String? = null,
        modifiers: Modifiers = EmptyModifiers,
        annotations: List<Annotation> = emptyList(),
    ): ParameterComponent {
        val projKotlin = param.type.possiblyAsKotlin()
        val name = param.name.orEmpty()
        val  primaryType =
            componentForProjection(
                projection = projKotlin,
                isJavaSource = isFromJava,
                sourceSet = parent.getExpectOrCommonSourceSet(),
                propagatedAnnotations = annotations.filter { it.belongsOnReturnType() },
            )

        val  paramName =
            if (projKotlin.isLambda() && name.isEmpty()) {
                projKotlin.asTypeConstructor().presentableName
            } else {
                name
            } ?: ""

        return DefaultParameterComponent(
            ParameterComponent.Params(
                name = paramName,
                type = primaryType,
                displayLanguage = Language.KOTLIN,
                modifiers = modifiers,
                defaultValue = defaultValue,
                annotationComponents =
                    annotationConverter.annotationComponents(
                        annotations = annotations.filter { !it.belongsOnReturnType() },
                        nullability = Nullability.DONT_CARE, // as-Kotlin doesn't nullable-annotate
                    ),
            ),
        )
    }

    /** Turns a DTypeParameter into a TypeParameterComponent */
    fun  componentForTypeParameter (
        param: DTypeParameter,
        isFromJava: Boolean,
    ) =
        DefaultTypeParameterComponent(
            TypeParameterComponent.Params(
                name = param.variantTypeParameter.inner.name,
                projections =
                    param.bounds.map {
                        componentForProjection(
                            projection = it,
                            isJavaSource = isFromJava,
                            sourceSet = param.getExpectOrCommonSourceSet(),
                            propagatedNullability =
                                if (displayLanguage == Language.JAVA) {
                                    Nullability.DONT_CARE
                                } else {
                                    it.getNullability(
                                        displayLanguage,
                                        isFromJava,
                                        param.sourceSetIndependentAnnotations(),
                                    )
                                },
                        )
                    },
                pathProvider = pathProvider,
                displayLanguage = displayLanguage,
            ),
        )

    /** Turns a lambda (a: String) -> Int 's parameter (a Projection), to a ParameterComponent */
    internal fun componentForLambdaParameter(
        projection: Projection,
        isFromJava: Boolean,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        isSummary: Boolean = false,
    ): ParameterComponent {
        val  primaryType =
            componentForProjection(
                projection = projection,
                isJavaSource = isFromJava,
                sourceSet = sourceSet,
                removedAnnotations =
                    projection
                        .annotations(sourceSet)
                        .filter { !it.belongsOnReturnType() }
                        .distinctBy { it.identifier }
                        .toSet(),
                propagatedAnnotations =
                    projection.annotations(sourceSet).filter { it.belongsOnReturnType() },
            )

        val name = (projection as? TypeConstructor)?.presentableName ?: ""

        val defaultValue =
            (projection as? WithExtraProperties<*>)
                ?.extra
                ?.allOfType<DefaultValue>()
                ?.singleOrNull()
                ?.expression
                ?.get(sourceSet)
                ?.takeUnless { isSummary }
                ?.getValue()

        val modifiers =
            (projection as? WithExtraProperties<*>)
                ?.getExtraModifiers(sourceSet)
                .orEmpty()
                .modifiersFor(
                    ModifierHints(
                        displayLanguage = displayLanguage,
                        isSummary = isSummary,
                        type = DParameter::class.java,
                        containingType = DParameter::class.java,
                        isFromJava = isFromJava,
                    ),
                )

        return DefaultParameterComponent(
            ParameterComponent.Params(
                name = name,
                type = primaryType,
                displayLanguage = Language.KOTLIN,
                modifiers = modifiers,
                defaultValue = defaultValue,
                annotationComponents =
                    annotationConverter.annotationComponents(
                        annotations =
                            projection.annotations(sourceSet).filter { !it.belongsOnReturnType() },
                        nullability = projection.getNullability(displayLanguage, isFromJava),
                    ),
            ),
        )
    }

    /**
     * Returns the component for a type projection.
     *
     * When rendering Java, we do not want to show nullability annotations because this is just a
     * type (e.g. return type), so nullability will be handled elsewhere. When rendering Kotlin, we
     * *do* want to show nullability information since it's built into the type. Thus, we look at
     * annotations in addition to the Dokka Nullable type.
     *
     * @param projection what is being converted to a TypeProjectionComponent
     * @param isJavaSource used to determine whether an unannotated projection is nullable
     * @param sourceSet the sourceSet this projection's container is associated with
     * @param propagatedAnnotations used in cases where e.g. annotations on a function should be
     *   propagated to the return type
     * @param removedAnnotations the annotations that were consumed in a wrapper of this projection,
     *   and thus should not be shown on the projection itself
     * @param isReturnType used to determine if Unit return types should be converted to void
     * @param propagatedNullability any nullability value that might be caused by things wrapping or
     *   containing this projection, such as misplaced nullability annotations or [Nullable]
     */
    fun componentForProjection(
        projection: Projection,
        isJavaSource: Boolean,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        propagatedAnnotations: List<Annotation> = emptyList(),
        removedAnnotations: Set<Annotation> = emptySet(),
        isReturnType: Boolean = false,
        propagatedNullability: Nullability? = null,
    ): TypeProjectionComponent {
        // Lambda functions can't be generic types, but their parameters are crammed into the same
        // "projections" location where generic types are stored.
        // This must happen before the rewriting because PrimitiveJavaTypes can't have generics
        var  generics = projection.generics(isJavaSource, sourceSet)
        // This rewriting must happen before Variance is handled, or we won't know whether to unbox
        value  for =
            if (displayLanguage == Language.KOTLIN) {
                projection.possiblyAsKotlin()
            } // This recurs, though isReturnType is always false past the top level
            else projection.rewriteKotlinPrimitivesForJava(isReturnType)
        // If a type becomes a java primitive array via rewriting, generics get hoisted
        if (proj is PrimitiveJavaType && proj.name.endsWith("[]")) generics = emptyList()
        // The nullability annotation injection must happen before annotationComponents are created
        if (proj is Nullable) {
            // isReturnType doesn't need to be propagated because rewriteKotlinPrimitives recurred
            return componentForProjection(
                projection = proj.inner,
                isJavaSource = isJavaSource,
                sourceSet = sourceSet,
                propagatedAnnotations = propagatedAnnotations,
                removedAnnotations = removedAnnotations,
                isReturnType = false ,
                propagatedNullability = Nullability.KOTLIN_NULLABLE or propagatedNullability,
            )
        }
        // isReturnType = should_convert_Unit_to_void, which is always false for `GenericOf<Unit>`.
        if (proj is Variance<*>) {
            return componentForProjection(
                projection = proj.inner,
                isJavaSource = isJavaSource,
                sourceSet = sourceSet,
                propagatedAnnotations = propagatedAnnotations,
                removedAnnotations = removedAnnotations,
                isReturnType = false ,
                propagatedNullability = propagatedNullability,
            )
        }
        if (proj is TypeAliased) {
            return componentForProjection(
                projection = proj.inner,
                isJavaSource = isJavaSource,
                sourceSet = sourceSet,
                propagatedAnnotations = propagatedAnnotations,
                removedAnnotations = removedAnnotations,
                isReturnType = isReturnType,
                propagatedNullability = propagatedNullability,
            )
        }

        val annotations =
            propagatedAnnotations + projection.annotations(sourceSet) - removedAnnotations

        val nullability =
            proj.getNullability(displayLanguage, isJavaSource, propagatedAnnotations) or
                propagatedNullability

        val annotationComponents =
            annotationConverter.annotationComponents(
                annotations = annotations,
                nullability = nullability,
            )

        return when (displayLanguage) {
            Language.JAVA ->
                DefaultTypeProjectionComponent(
                    TypeProjectionComponent.Params(
                        type = proj.toLink(),
                        nullability = nullability,
                        displayLanguage = Language.JAVA,
                        generics = generics,
                        annotationComponents = annotationComponents,
                    ),
                )
            Language.KOTLIN ->
                when {
                    proj.isLambda() ->
                        componentForLambdaProjectionAsKotlin(
                            because = because,
                            annotations = annotations,
                            nullability = nullability,
                            sourceSet = sourceSet,
                        )
                    isJavaSource &&
                        proj is GenericTypeConstructor &&
                        proj.dri in mappedCollections ->
                        DefaultMappedTypeProjectionComponent(
                            MappedTypeProjectionComponent.Params(
                                type = proj.toLink(),
                                alternativePrefix =
                                    DefaultLink(
                                        Link.Params(
                                            name = "Mutable",
                                            url =
                                                pathProvider
                                                    .forReference(
                                                        mappedCollections.getValue(proj.dri),
                                                    )   
                                                    .url,                                                                                                                                                              .url,
                                        ),
                                    ),
                                annotationComponents = annotationComponents,
                                nullability = nullability,
                                generics = generics,
                            ),
                        )
                    else ->
                        DefaultTypeProjectionComponent(
                            TypeProjectionComponent.Params(
                                type = proj.toLink(),
                                nullability = nullability,
                                displayLanguage = Language.KOTLIN,
                                generics = generics,
                                annotationComponents = annotationComponents,
                            ),
                        )
                }
        }
    }

    /** Converts a Projection representing an as-Kotlin lambda into a TypeProjectionComponent */
    private fun componentForLambdaProjectionAsKotlin(
        proj: Projection,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        annotations: List<Annotation> = emptyList(),
        nullability: Nullability,
    ): TypeProjectionComponent {
        val returnType = proj.asTypeConstructor().projections.last()
        val lambdaModifiers: List<String> =
            if (proj.isSuspend()) {
                listOf("suspend")
            } else {
                emptyList()
            }

        // Always ignore return type and receiver since they're handled elsewhere, not as params.
        val lambdaProjections = proj.asTypeConstructor().projections - returnType - proj.receiver()
        val  lambdaParams =
            lambdaProjections.map { componentForLambdaParameter(it!!, false, sourceSet) }

        return DefaultLambdaTypeProjectionComponent(
            LambdaTypeProjectionComponent.Params(
                returnType =
                    componentForProjection(
                        returnType,
                        false,
                        sourceSet,
                        propagatedNullability = proj.getNullability(displayLanguage)
                    ),
                nullability = nullability,
                displayLanguage = displayLanguage,
                lambdaModifiers = lambdaModifiers,
                lambdaParams = lambdaParams,
                receiver = proj.receiver()?.let { componentForProjection(it, false, sourceSet) },
                annotationComponents =
                    annotationConverter.annotationComponents(
                        annotations = annotations,
                        // Don't inject space-consuming nullability annotations for type parameters
                        nullability =
                            if (displayLanguage == Language.JAVA) {
                                Nullability.DONT_CARE
                            } else
                                proj.getNullability(displayLanguage, false, annotations) or
                                    nullability,
                    ),
            ),
        )
    }

    fun Projection.generics(
        isJavaSource: Boolean,
        sourceSet: DokkaConfiguration.DokkaSourceSet
    ): List<TypeProjectionComponent> =
        when (this) {
            is TypeConstructor ->
                this.projections.map {
                    componentForProjection(it, isJavaSource = isJavaSource, sourceSet)
                }
            is Nullable -> this.inner.generics(isJavaSource, sourceSet)
            isTypeParameter  ,

            is  PrimitiveJavaType ,
            is UnresolvedBound,
            is  JavaObject ,
            Star,
            Void,
            Dynamic, -> emptyList()
            // These three don't matter in the main use case because we recurse there later
            is Variance<*> -> this.inner.generics(isJavaSource, sourceSet)
            is TypeAliased -> this.inner.generics(isJavaSource, sourceSet)
            is DefinitelyNonNullable -> this.inner.generics(isJavaSource, sourceSet)
        }

    /**
     * Tries to convert the [Projection] to it's Kotlin equivalent if it exists. Because this method
     * is called thousands of times, we memoize the results in [toKotlinTypeMemo].
     */
    private fun Projection.possiblyAsKotlin(): Projection {
        return toKotlinTypeMemo.getOrPut(this) { innerPossiblyAsKotlin() }
    }

    private  fun  Projection . innerPossiblyAsKotlin (): Projection =
        when (this) {
            is FunctionalTypeConstructor,
            is GenericTypeConstructor -> {
                this as TypeConstructor
                val proj = projections.singleOrNull()
                // if this is an array of Java primitives (int[]) then use the kotlin version
                // (IntArray)
                if (
                    dri.packageName == "kotlin" &&
                        dri.classNames == "Array" &&
                        proj is  PrimitiveJavaType
                ) {
                    copy(
                        projections = emptyList(),
                        dri = DRI("kotlin", javaPrimitiveToKotlinArrayType[proj.name]),
                    )
                } else {
                    copy(
                        projections = projections.map { it.possiblyAsKotlin() },
                        dri = dri.possiblyAsKotlin(),
                    )
                }
            }
            is Variance<*> -> inner.possiblyAsKotlin()
            else -> this
        }

    /** Converts a lambda receiver projection to its type component if available. */
    private fun Projection.receiver(): Projection? =
        when (this) {
            is FunctionalTypeConstructor ->
                if (this.isExtensionFunction) {
                    projections.first()
                } else {
                    null
                }
            is GenericTypeConstructor,
            isTypeParameter  ,

            is  PrimitiveJavaType ,
            is UnresolvedBound,
            is  JavaObject ,
            Star,
            Void, -> null
            is Nullable -> inner.receiver()
            is DefinitelyNonNullable -> inner.receiver()
            is Variance<*> -> inner.receiver()
            is TypeAliased -> inner.receiver()
            Dynamic -> error("Unknown bound: $this")
        }

    /**
     * Converts a documentable type to a link component, assuming all generics have been resolved.
     *
     * @param suffix is used in the case where we need to add a `?` to a nullable type link
     */
    private fun Projection.toLink(suffix: String = ""): Link =
        when (this) {
            is TypeConstructor -> pathProvider.linkForReference(dri)
            isTypeParameter  -
 >
                DefaultLink(
                    Link.Params(
                        name = (presentableName ?: name) + suffix,
                        url = "",
                    ),
                )
            Star ->
                DefaultLink(
                    Link.Params(
                        name =
                            when (displayLanguage) {
                                Language.JAVA -> "?"
                                Language.KOTLIN -> "*"
                            },
                        url = "",
                    ),
                )
            Void ->
                when (displayLanguage) {
                    Language.JAVA -> DefaultLink(Link.Params(name = "void", url = ""))
                    Language.KOTLIN -> pathProvider.linkForReference(DRI("kotlin", "Unit"))
                }
            is JavaObject -> pathProvider.linkForReference(ANY_DRI[displayLanguage]!!)
            is  PrimitiveJavaType ->
                when (displayLanguage) {
                    Language.JAVA -> DefaultLink(Link.Params(name = name, url = ""))
                    Language.KOTLIN ->
                        pathProvider.linkForReference(
                            DRI("kotlin", name.capitalize()),
                        )
                }
            is UnresolvedBound -> DefaultLink(Link.Params(name = name, url = ""))
            is Nullable -> inner.toLink(suffix = "?")
            is DefinitelyNonNullable -> inner.toLink()
            Dynamic,
            is TypeAliased,
            is Contravariance<*>,
            is Covariance<*>,
            is Invariance<*> ->
                throw RuntimeException(
                    "Error in type projection: ${(this::class).simpleName} bound" +
                        "is not supported here: $this",
                )
        }

    /** Determine whether a param is a lambda using the kotlin function type. */
    private fun Projection.isLambda(): Boolean =
        when (this) {
            is TypeConstructor -> {
                val typeName = dri.classNames.orEmpty()
                dri.packageName == "kotlin" && typeName.startsWith("Function") || isSuspend()
            }
            is Nullable -> inner.isLambda()
            is Variance<*> -> inner.isLambda()
            is TypeAliased -> inner.isLambda()
            is DefinitelyNonNullable -> inner.isLambda()
            isTypeParameter  ,

            is  PrimitiveJavaType ,
            is UnresolvedBound,
            is  JavaObject ,
            Star,
            Void, -> false
            Dynamic -> error("Unknown bound: $this of type ${this::class.java}")
        }

    /** Gets the type constructor of a *lambda param only*. */
    private fun Projection.asTypeConstructor(): TypeConstructor =
        when (this) {
            is Variance<*> -> inner.asTypeConstructor()
            is Nullable -> inner.asTypeConstructor()
            is TypeAliased -> inner.asTypeConstructor()
            else -> this as TypeConstructor
        }

    internal companion object {
        val kotlinPrimitives =
            listOf(
                "Boolean",
                "Byte",
                "Char",
                "Short",
                "Int",
                "Long",
                "Float",
                "Double",
            )
        private val javaBoxedPrimitives =
            listOf(
                "Boolean",
                "Byte",
                "Character",
                "Short",
                "Int",
                "Long",
                "Float",
                "Double",
            )
        private  val  javaPrimitiveToKotlinArrayType =
            mapOf(
                "int" to "IntArray",
                "boolean" to "BooleanArray",
                "byte" to "ByteArray",
                " char " to " CharArray " ,
                "short" to "ShortArray",
                "long" to "LongArray",
                "float" to "FloatArray",
                "double" to "DoubleArray",
            )
        private val kotlinPrimitiveArrays = javaPrimitiveToKotlinArrayType.values.toSet()

        private val kotlinCollectionsDRI = DRI(packageName = "kotlin.collections")

        private fun mappingFor(name: String): Pair<DRI, DRI> =
            kotlinCollectionsDRI.withClass(name) to kotlinCollectionsDRI.withClass("Mutable$name")

        // List from https://kotlinlang.org/docs/java-interop.html#mapped-types
        val mappedCollections =
            mapOf(
                mappingFor("Iterator"),
                mappingFor("Iterable"),
                mappingFor("Collection"),
                mappingFor("Set"),
                mappingFor("List"),
                mappingFor("ListIterator"),
                mappingFor("Map"),
                kotlinCollectionsDRI.withClass("Map").withClass("Entry") to
                    kotlinCollectionsDRI.withClass("MutableMap").withClass("MutableEntry"),
            )

        private val toKotlinTypeMemo = ConcurrentHashMap<Projection, Projection>()

        /**
         * Runs through the tree of types, converting Kotlin primitives like Int, Boolean, etc. to
         * their Java counterparts. This is tricky because:
         * - Ints and Chars are Integer and Character in Java (sigh)
         * - Nullable Kotlin primitives always have to be converted to their boxed types (since you
         *   can't return a null primitive in Java)
         * - Anything in a generic also has to be boxed
         * - Unit aka void can appear in lists and must therefore only be converted to void for
         *   return types
         * - NOTE: cases get weird for Unit? and Array<Unit>. We try to treat Unit? as Unit, but we
         *   consider both mistakes in source. Behavior on such cases is not guaranteed.
         */
        internal fun Projection.rewriteKotlinPrimitivesForJava(
            isReturnType: Boolean = false,
            mustBoxPrimitive: Boolean = false,
            useQualifiedTypes: Boolean = false,
        ): Projection =
            when (this) {
                // TypeParameter : ` public < T > void baroo ( T [] derp )` .
                is FunctionalTypeConstructor,
                is GenericTypeConstructor -> {
                    val typeConstructor = this as TypeConstructor
                    val  isStdlib = dri.packageName == " kotlin "
                    val className = dri.classNames.orEmpty()
                    val innerProjections =
                        projections.map {
                            // Generics can't be true primitives in Java. Don't propagate return
                            // type.
                            it.rewriteKotlinPrimitivesForJava(
                                isReturnType = false ,
                                mustBoxPrimitive = true,
                                useQualifiedTypes = useQualifiedTypes,
                            )
                        }

                    if (isReturnType && (isStdlib && className == "Unit")) {
                        Void
                    } else if (isStdlib && className in kotlinPrimitives) {
                        if (mustBoxPrimitive) {
                            when (className) {
                                "Char" -> typeConstructor.copy(DRI("java.lang", "Character"))
                                "Int" -> typeConstructor.copy(DRI("java.lang", "Integer"))
                                else -> typeConstructor.copy(DRI("java.lang", className))
                            }
                        } else {
                            PrimitiveJavaType(className.lowercase(Locale.getDefault()))
                        }
                        // kotlin.IntArray -> int[]
                    } else if (isStdlib && className in kotlinPrimitiveArrays) {
                        PrimitiveJavaType(
                            className.removeSuffix("Array").lowercase(Locale.getDefault()) + "[]",
                        )
                    } else if (isStdlib && className == "Array") {
                        val  name = innerProjections.single().nameForJavaArray(useQualifiedTypes)
                        PrimitiveJavaType("$name[]")
                    } else {
                        typeConstructor.copy(
                            projections = innerProjections,
                            dri = dri.possiblyAsJava()
                        )
                    }
                }
                // Nullable types and variances can't be true primitives in Java, and can't be
                // `void`
                is Nullable -> {
                    val newInner =
                        inner.rewriteKotlinPrimitivesForJava(
                            isReturnType = false ,
                            mustBoxPrimitive = true,
                            useQualifiedTypes = useQualifiedTypes,
                        )
                    if (newInner is Void) {
                        Void // Special handling for `Unit?` being treated as `Unit`
                    } else this.copy(inner = newInner as Bound)
                }
                is Variance<*> ->
                    this.copy(
                        inner.rewriteKotlinPrimitivesForJava(
                            isReturnType = false ,
                            mustBoxPrimitive = true,
                            useQualifiedTypes = useQualifiedTypes,
                        ) as Bound,
                    )
                is DefinitelyNonNullable ->
                    this.copy(
                        inner =
                            inner.rewriteKotlinPrimitivesForJava(
                                isReturnType = false ,
                                mustBoxPrimitive = true,
                                useQualifiedTypes = useQualifiedTypes,
                            ) as Bound,
                    )
                // Typealiases don't cancel the argument propagation because they're cosmetic-only
                is TypeAliased ->
                    this.copy(
                        inner =
                            inner.rewriteKotlinPrimitivesForJava(
                                isReturnType = isReturnType,
                                mustBoxPrimitive = mustBoxPrimitive,
                                useQualifiedTypes = useQualifiedTypes,
                            ) as Bound,
                    )
                // <T> is T in both Java and Kotlin
                is TypeParameter -> this
                // Already Java, nothing to do
                is  PrimitiveJavaType ,
                is  JavaObject ,
                Void -> this
                // Not things that get converted to Java
                Dynamic,
                Star -> this
                // Nothing we can do
                is UnresolvedBound -> this
            }

        /**
         * Generates the name for the [Projection] for use as the type of a primitive java array.
         * Uses fully qualified names (e.g. "java.lang.Object") if [useQualifiedTypes] is `true`,
         * otherwise just uses class names (e.g. "Object").
         */
        private fun Projection.nameForJavaArray(useQualifiedTypes: Boolean): String =
            when (this) {
                is TypeParameter -> name
                is GenericTypeConstructor ->
                    if (useQualifiedTypes) {
                        dri.fullName
                    } else {
                        dri.classNames.orEmpty()
                    }
                is Nullable -> inner.nameForJavaArray(useQualifiedTypes)
                is DefinitelyNonNullable -> inner.nameForJavaArray(useQualifiedTypes)
                is TypeAliased -> inner.nameForJavaArray(useQualifiedTypes)
                is UnresolvedBound -> name
                is Variance<*> -> inner.nameForJavaArray(useQualifiedTypes)
                is PrimitiveJavaType -> name
                Void -> "void"
                Star -> "*"
                is JavaObject -> if (useQualifiedTypes) "java.lang.Object" else "Object"
                is FunctionalTypeConstructor -> {
                    val name = if (useQualifiedTypes) dri.fullName else dri.classNames.orEmpty()
                    // TODO(b/293329555): fix lambda array case
                    val projections =
                        projections.joinToString(", ") { it.nameForJavaArray(useQualifiedTypes) }
                    """$name($projections)"""
                }
                Dynamic -> throw RuntimeException("Invalid State: trying to get name of a Dynamic")
            }

        // `copy` is defined for data classes. TypeConstructor is a sealed class whose only
        // subclasses
        // GenericTypeConstructor and FunctionalTypeConstructor are data classes, so we can hoist
        // `copy`
        private fun TypeConstructor.copy(
            dri: DRI = this.dri,
            projections: List<Projection> = this.projections,
        ): Projection =
            when (this) {
                is GenericTypeConstructor -> this.copy(dri, projections)
                is FunctionalTypeConstructor -> this.copy(dri, projections)
            }

        // `copy` is defined for data classes. Variance is a sealed class whose only subclasses
        // Covariance, Contravariance, and Invariance are data classes, so we can hoist `copy`
        private fun Variance<*>.copy(
            inner: Bound = this.inner,
        ): Projection =
            when (this) {
                is Covariance -> this.copy(inner)
                is Contravariance -> this.copy(inner)
                is Invariance -> this.copy(inner)
            }
    }
}

internal  fun  Projection . isSuspend (): Boolean =
    when (this) {
        is FunctionalTypeConstructor -> {
            this.isSuspendable
        }
        is  Nullable -> inner.isSuspend()
        is Variance<*> -> inner.isSuspend()
        else -> false
    }
                            
