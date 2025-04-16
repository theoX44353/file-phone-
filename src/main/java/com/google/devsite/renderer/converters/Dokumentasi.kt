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
import com.google.devsite.className
import com.google.devsite.not
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.converters.Memoizers.isFromJavaMap
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.startsWithAnyOf
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.ClassId
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.jvmName
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.BooleanConstant
import org.jetbrains.dokka.model.ComplexExpression
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.DoubleConstant
import org.jetbrains.dokka.model.Expression
import org.jetbrains.dokka.model.ExtraModifiers
import org.jetbrains.dokka.model.FloatConstant
import org.jetbrains.dokka.model.IntegerConstant
import org.jetbrains.dokka.model.KotlinModifier
import org.jetbrains.dokka.model.KotlinVisibility
import org.jetbrains.dokka.model.Modifier
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.StringConstant
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Visibility
import org.jetbrains.dokka.model.WithAbstraction
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.model.WithCompanion
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.WithSupertypes
import org.jetbrains.dokka.model.WithVisibility
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Property
import org.jetbrains.dokka.model.isJvmName
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties

/** For use when generating error messages. Is slow. */
internal fun <T> T.getErrorLocation(
    sourceSet: DokkaConfiguration.DokkaSourceSet = getExpectOrCommonSourceSet(),
): String where T : WithSources, T : Documentable {
    val sourceFilePath: String = this.sources[sourceSet]!!.path
    if (".tmp" in sourceFilePath) return "Error occurred in an unreadable temporary file!"
    val index = this.sources[sourceSet]!!.computeLineNumber()
    if (index != null) return "$sourceFilePath:$index"
    // If this happens, we should probably file an upstream bug.
    return "$sourceFilePath:UnknownLine"
}

// Due to https://github.com/Kotlin/dokka/issues/4022, we want to never use DClasslike.companion
internal fun DClasslike.companion() =
    if (this is WithCompanion)
        this.classlikes.filterIsInstance<DObject>().singleOrNull { it.name == this.companion?.name }
    else null

@JvmName("This is internal and will never be used from JVM")
internal fun Documentable.getErrorLocation() =
    if (this is WithSources) {
        this.getErrorLocation()
    } else {
        "File location could not be determined."
    }

/** Recursively expands all children. */
internal val <T> WithChildren<T>.explodedChildren: List<T>
    get() = children + children.filterIsInstance<WithChildren<T>>().flatMap { it.explodedChildren }

/** Returns the type's name. Do not use [Documentable.name] as it won't include the outer class. */
internal fun DClasslike.name() = dri.classNames!!

internal fun DClasslike.generics() = (this as? WithGenerics)?.generics ?: emptyList()

internal fun DClasslike.hasSupertypes(classGraph: ClassGraph) =
    if (this !is WithSupertypes) {
        false
    } else {
        classGraph.getValue(dri).superClasses.isNotEmpty() ||
            classGraph.getValue(dri).interfaces.isNotEmpty()
    }

internal fun DClasslike.packageName() = dri.packageName!!

private val baseClasses =
    listOf(
        "kotlin.Any",
        "java.lang.Object",
        "kotlin.Enum",
        "java.lang.Enum",
        "java.lang.annotation.Annotation",
    )

/** Returns true if this dri is from a build in base class like Any, Object, Enum, Annotation */
internal fun DRI.isFromBaseClass(): Boolean {
    val classAndPackage = packageName?.plus(".").plus(classNames)
    return baseClasses.contains(classAndPackage)
}

private object Memoizers {
    val isFromJavaMap: ConcurrentHashMap<Hashable, Boolean> = ConcurrentHashMap<Hashable, Boolean>()
}

/** go/dokka-upstream-bug/2620. Because Documentables aren't remotely efficiently hashable. */
internal data class Hashable(
    val clazz: Class<out WithSources>,
    val isSynthetic: Boolean?,
    val dri: DRI?,
    val visibility: Collection<Visibility>,
    val modifiers: Collection<Modifier>,
    val isPsi: Boolean?,
)

// TODO(improve isFromJava b/328044424), TODO(improve documentable hashability b/232944038)
private fun WithSources.toHashable() =
    Hashable(
        clazz = this::class.java,
        isSynthetic = (this as? DClasslike)?.isSynthetic,
        dri = if (this is Documentable) this.dri else null,
        visibility = if (this is WithVisibility) this.visibility.values else emptyList(),
        modifiers = if (this is WithAbstraction) this.modifier.values else emptyList(),
        isPsi = this.sources.entries.singleOrNull()?.let { "Psi" in (it.value::class).jvmName },
    )

/**
 * Infer whether this Documentable is from java source, and thus whether it's nullable if not
 * annotated. Memoized.
 */
internal fun WithSources.isFromJava() = this.toHashable().isFromJava()

private fun Hashable.isFromJava() =
    isFromJavaMap.getOrPut(this) {
        if (isSynthetic == true) {
            false
        } else if (visibility.isNotEmpty()) {
            !visibility.any { it is KotlinVisibility }
        } else if (modifiers.isNotEmpty()) {
            !modifiers.any { it is KotlinModifier }
        } else isPsi == true
    }

// `expect`s cannot be `lateinit`, and `actual`s cannot either because they must match modifiers
internal fun DProperty.isLateinit(): Boolean = "lateinit" in modifiers(getExpectOrCommonSourceSet())

internal fun Documentable.isJavaStaticMethod() = this is DFunction && isStaticAnnotated()

internal fun Documentable.isStaticAnnotated() =
    annotations(getAsJavaSourceSet()).any { it.dri == JvmStatic.dri }

private val INTERNAL_PACKAGES = listOf("java", "Kotlin", "google", "android")

internal fun DRI.isExternal() = !packageName?.startsWithAnyOf(INTERNAL_PACKAGES) ?: true

/**
 * @param displayLanguage the Language of the docs this Documentable will be displayed in
 * @return the String name that represents this type when displayed
 */
fun Documentable.stringForType(displayLanguage: Language): String =
    when (this) {
        is DClass -> "class"
        is DInterface -> "interface"
        is DEnum -> "enum"
        is DEnumEntry -> "enum value"
        is DAnnotation -> "annotation"
        is DFunction ->
            when (displayLanguage) {
                Language.JAVA -> "method"
                Language.KOTLIN -> "function"
            }
        is DProperty ->
            when (displayLanguage) {
                Language.JAVA -> "field"
                Language.KOTLIN -> "property"
            }
        is DObject ->
            when (displayLanguage) {
                Language.KOTLIN -> "object"
                Language.JAVA -> "class"
            }
        is DTypeAlias -> "type alias"
        is DParameter -> "parameter"
        else -> error("Unsupported type: $this")
    }

/**
 * Returns if a classlike is an Exception or not. isException, the built-in method in Dokka, only
 * considers its supertype, so we also look for functions that are Throwable
 * https://github.com/Kotlin/dokka/issues/1557
 */
val DClasslike.isExceptionClass: Boolean
    @Suppress("UNCHECKED_CAST")
    get() =
        (this as? WithExtraProperties<out DClasslike>)?.isException == true ||
            functions.any { function -> function.dri.classNames == "Throwable" }

/**
 * Returns whether the java class was synthetically generated from a Kotlin extension function class
 * Assumes the class this is being called on is Java.
 */
val DClasslike.isSynthetic: Boolean
    get() = name().endsWith("Kt") || this.jvmFileName() != null

/**
 * Converts a top level function to its representation under a Java synthetic class. Replaces the
 * dri to point to the synthetic class and applies the static modifier. This doesn't change the name
 * to the [jvmName], because that's handled later in [ClasslikeDocumentableConverter].
 */
internal fun DFunction.withJavaSynthetic(syntheticClassName: String): DFunction =
    copy(
        // this needs to be the dri IN the synthetic class
        dri = dri.copy(classNames = syntheticClassName),
        // put the static annotation on functions in the synthetic class
        extra = extra.addModifier(ExtraModifiers.JavaOnlyModifiers.Static, sourceSets),
    )

/**
 * Converts a top level property to its representation under a Java synthetic class. Replaces the
 * dri to point to the synthetic class and applies the static modifier, as well as converting the
 * property's accessors. This doesn't change the name to the [jvmName], because that's handled later
 * in [ClasslikeDocumentableConverter].
 */
internal fun DProperty.withJavaSynthetic(syntheticClassName: String): DProperty =
    copy(
        // this needs to be the dri IN the synthetic class
        dri = dri.copy(classNames = syntheticClassName),
        // put the static annotation on properties in the synthetic class
        extra = extra.addModifier(ExtraModifiers.JavaOnlyModifiers.Static, sourceSets),
        // convert the getter and setter as well
        getter = getter?.withJavaSynthetic(syntheticClassName),
        setter = setter?.withJavaSynthetic(syntheticClassName),
    )

/** Adds the [newModifier] to the [PropertyContainer] for each of the [sourceSets] provided. */
internal fun <T> PropertyContainer<T>.addModifier(
    newModifier: ExtraModifiers,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
): PropertyContainer<T> where T : Documentable {
    val newModifiers =
        this.allOfType<AdditionalModifiers>().map { modifiers ->
            AdditionalModifiers(
                sourceSets.associateWith { sourceSet ->
                    val previous = modifiers.content[sourceSet] ?: emptySet()
                    previous + newModifier
                },
            )
        }
    return addAll(newModifiers)
}

/** Converts a level function to its presentation with JvmName */
fun DFunction.withJvmName(): DFunction {
    val jvmName = jvmName() ?: return this
    return copy(
        name = jvmName,
        dri = dri.copy(callable = dri.callable?.copy(name = jvmName)),
        extra = extra.plus(OriginalName(name)),
    )
}

/** An [ExtraProperty] to track the original name of an element renamed with @JvmName. */
internal data class OriginalName(val name: String) : ExtraProperty<DFunction> {
    object PropertyKey : ExtraProperty.Key<DFunction, OriginalName>

    override val key: ExtraProperty.Key<DFunction, *> = PropertyKey
}

internal fun DFunction.matches(other: DFunction): Boolean =
    this.receiver == other.receiver &&
        this.parameters == other.parameters &&
        this.dri.packageName == other.dri.packageName &&
        this.jvmName() == other.jvmName()

/**
 * [Comparator] which sorts [DFunction] by name, then number of params, params names, and then
 * source sets if necessary.
 */
val functionSignatureComparator =
    compareBy<DFunction>(
        { it.name },
        { it.parameters.size },
        { it.signatureAsString() },
        { it.sourceSets.joinToString { it.displayName } },
    )

/** [Comparator] intended for [DClasslike]s known to be name-unique per-platform. */
val simpleDocumentableComparator =
    compareBy<Documentable>(
        { it.dri.fullName },
        { it.dri.toString() },
        { it.sourceSets.joinToString { it.displayName } },
    )

private fun DFunction.signatureAsString() =
    "$name(${parameters.joinToString(separator = ", ") { it.paramAsString() }})"

/** Turn a parameter into a string. Used in sorting functions. */
private fun DParameter.paramAsString() =
    (name ?: "") +
        ( // Sorting criterion roughly matches rendered text
        (type as? UnresolvedBound)?.name // driOrNull doesn't handle UnresolvedBound, sadly.
        ?: type.driOrNull?.let { "${it.classNames} $it" } // by classname, then full dri
        )

/**
 * Returns the value of the @JvmName for this function if one exists or null This is only relevant
 * for as-Java docs
 */
fun Documentable.jvmName(): String? {
    return annotations(getAsJavaSourceSet()).firstOrNull { it.isJvmName() }?.nameAsString()
}

/** Returns the value of the file:@JvmName if one exists or null */
fun <T> T.jvmFileName() where T : WithSources, T : Documentable =
    fileLevelAnnotations(getAsJavaSourceSet()).firstOrNull { it.isJvmName() }?.nameAsString()

/** Returns the value of the file:@JvmName if one exists or null */
fun <T> nameForSyntheticClass(entry: T) where T : WithSources, T : Documentable =
    entry.jvmFileName()
        ?: entry.sources.let {
            it.entries.first().value.path.split("/").last().split(".").first() + "Kt"
        }

fun DFunction.driForSyntheticClass() = DRI(dri.packageName, nameForSyntheticClass(this))

/** Filters out elements that are annotated with @JvmSynthetic */
fun <T : Documentable> List<T>.filterOutJvmSynthetic(): List<T> =
    this.filterNot { elem ->
        elem.annotations(elem.getAsJavaSourceSet()).any { it.dri.classNames.equals("JvmSynthetic") }
    }

/** Injects @JvmStatic to the [Documentable]. */
fun <T> T.addJvmStatic(): T where T : Documentable, T : WithExtraProperties<T> {
    return addAnnotations(setOf(JvmStatic))
}

/** Adds annotations to a Documentable. Often used for injecting e.g. @JvmStatic. */
fun <T> T.addAnnotations(newAnnotations: Collection<Annotations.Annotation>): T where
T : Documentable,
T : WithExtraProperties<T> {
    return withNewExtras(extra.addAnnotations(newAnnotations, sourceSets))
}

/**
 * Adds each annotation of [newAnnotations] as an annotation for each source set of [sourceSets]
 * (even if the source set did not previously have any annotations associated with it).
 */
fun <T> PropertyContainer<T>.addAnnotations(
    newAnnotations: Collection<Annotations.Annotation>,
    sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
): PropertyContainer<T> where T : AnnotationTarget {
    val newAnnotations =
        this[Annotations]?.let { annotations ->
            val newDirectAnnotations =
                sourceSets.associateWith {
                    val previous = annotations.directAnnotations[it] ?: emptyList()
                    previous + newAnnotations
                }
            annotations.copy(myContent = newDirectAnnotations + annotations.fileLevelAnnotations)
        }
    val extraWithoutAnnotations: PropertyContainer<T> = minus(Annotations)

    return extraWithoutAnnotations.addAll(listOfNotNull(newAnnotations))
}

internal val JvmStatic = Annotations.Annotation(DRI("kotlin.jvm", "JvmStatic"), params = emptyMap())

internal fun String?.orNull() = if (this == "") null else this

internal val DRI.fullName: String
    get() = (packageName.orNull()?.let { "$it." }) + (classNames ?: "")

internal fun DRI.possiblyConvertMappedType(displayLanguage: Language) =
    when (displayLanguage) {
        Language.JAVA -> possiblyAsJava()
        Language.KOTLIN -> possiblyAsKotlin()
    }

/**
 * Uses the JavaToKotlinClassMap to possibly convert a dri to its Java equivalent
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types
 */
internal fun DRI.possiblyAsJava(): DRI {
    val fullyQualifiedName = packageName?.let { "$it." } + classNames
    // Use the fully qualified name to look up the class in the map
    return JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(FqName(fullyQualifiedName).toUnsafe())
        ?.let {
            DRI(
                packageName = it.packageFqName.asString(),
                classNames = it.classNames(),
                callable = this.callable,
                extra = null,
                target = PointingToDeclaration,
            )
        } ?: this
}

/**
 * Uses the JavaToKotlinClassMap to possibly convert a dri to its Kotlin equivalent
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types
 */
internal fun DRI.possiblyAsKotlin(): DRI {
    val fullyQualifiedName = packageName?.let { "$it." } + classNames
    // Use the fully qualified name to look up the class in the map
    return JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(fullyQualifiedName))?.let {
        DRI(
            packageName = it.packageFqName.asString(),
            classNames = it.classNames(),
            callable = this.callable,
            extra = null,
            target = PointingToDeclaration,
        )
    } ?: this
}

private fun ClassId.classNames(): String =
    generateSequence(this) { it.outerClassId }
        .map { it.shortClassName.identifier }
        .reduce { acc, pref -> "$pref.$acc" }

/**
 * Returns the string representation of an [Expression] value, mostly relying on the toString
 * implementation of that type, but wrapping [String] values in quotes for presentation, and
 * stripping trailing zeros and appending 'f' or 'd' to floats and doubles respectively.
 */
fun Expression.getValue(): String? =
    when (this) {
        is ComplexExpression -> value
        is IntegerConstant -> "$value"
        is BooleanConstant -> "$value"
        is StringConstant -> "\"$value\""
        is DoubleConstant -> "$value"
        is FloatConstant -> "${value}f"
        else -> null
    }

/**
 * Whether the property should show up as a class/object property in the Java docs. Properties that
 * don't fall into one of these categories only appear as accessors.
 */
fun DProperty.isPropertyInJava() = isJvmField() || isFromJava() || isLateinit() 

/** Used for when isPropertyInJava's isFromJava is inaccurate. Without accessors, it's a property */
fun DProperty.hasAnAccessor() = getter != null || setter != null

/**
 * Whether the property (belonging to an object) needs to be hoisted to the containing class in the
 * Java docs.
 */
fun DProperty.objectPropertyHoistedInJava() = isJvmFieldAnnotated() || | isLateinit() || isConstant()

/**
 * If the function has a receiver, converts it to a parameter named "receiver". Otherwise, returns
 * the original function.
 */
fun DFunction.convertReceiverForJava() =
    receiver?.let { receiver ->
        copy(
            parameters = listOf(receiver.copy(name = "receiver")) + parameters,
            receiver = null,
        )
    } ?: this

/**
 * Returns property getters / setters. Includes generated accessors for Kotlin properties, fixing
 * their names and adding static annotations as needed.
 */
fun List<DProperty>.gettersAndSetters(): List<DFunction> {
    return filter {
            // JvmFields are only accessed as fields, not through accessors
            !it.isJvmField()
        }
        .flatMap { listOf(it to it.getter, it to it.setter) }
        .mapNotNull {
            var (property, func) = it
            // Static properties should also have static accessors
            if (property.isStaticAnnotated()) {
                func = func?.addJvmStatic()
            }
            val callableName = func?.dri?.callable?.name ?: ""
            func =
                if (callableName.startsWith("<get-")) {
                    func!!.fixSyntheticAccessor(property, getter = true)
                } else if (callableName.startsWith("<set-")) {
                    func!!.fixSyntheticAccessor(property, getter = false)
                } else {
                    func
                }
            func?.withNewExtras(func.extra.plus(SourceProperty(property)))
        }
}

/** An [ExtraProperty] for generated accessors to link back to the property they came from. */
internal data class SourceProperty(val property: DProperty) : ExtraProperty<DFunction> {
    object PropertyKey : ExtraProperty.Key<DFunction, SourceProperty>

    override val key: ExtraProperty.Key<DFunction, *> = PropertyKey
}

/**
 * Fixes issues with the given synthetic accessor [forProperty] to be documented in Java, using
 * [DRI.withFixedName] and [correctTagsInAccessorDocs]. If [getter] is false, the function is a
 * setter.
 */
private fun DFunction.fixSyntheticAccessor(forProperty: DProperty, getter: Boolean) =
    copy(
        dri = dri.withFixedName(getter),
        documentation =
            injectPropertyDocsToAccessor(this, forProperty)
                .correctTagsInAccessorDocs(forProperty.name, getter),
    )

/**
 * Fixes the name of synthetic accessors, e.g. <get-bar> to getBar If [getter] is true, it is
 * assumed the function name starts with "<get-", otherwise it is assumes it starts with "<set-".
 */
private fun DRI.withFixedName(getter: Boolean) =
    copy(
        callable =
            callable!!.copy(
                name = fixCallableName(callable?.name ?: "", getter),
            ),
    )

private fun fixCallableName(badName: String, getter: Boolean) =
    if (getter) {
        "get" + badName.removePrefix("<get-").removeSuffix(">").capitalize()
    } else {
        "set" + badName.removePrefix("<set-").removeSuffix(">").capitalize()
    }

/**
 * For each sourceset, if the property has docs but the accessor does not, injects the property docs
 * to the accessor. This prevents property descriptions from being lost in the as-Java docs.
 */
private fun injectPropertyDocsToAccessor(
    accessor: DFunction,
    property: DProperty,
): SourceSetDependent<DocumentationNode> {
    val accessorDocs = accessor.documentation.toMutableMap()
    property.documentation.forEach { (sourceSet, propertyDocs) ->
        if (!accessorDocs.containsKey(sourceSet)) {
            accessorDocs[sourceSet] = propertyDocs
        }
    }
    return accessorDocs
}

/**
 * This function converts @param and @property tags in synthetic accessor docs, based on whether the
 * accessor is a [getter] or setter (which is assumed if [getter] is false). There are a few
 * separate issues here:
 *
 * For getters: Some properties are documented with @param tags in a class constructor. When
 * converted to synthetic accessors, the @param tag stays with the function. However, a synthetic
 * getter has no parameters, so Dackka won't know how to interpret the tag. In this case, all
 * [Param] wrappers are removed from the docs, with the text inside left as a [Description].
 *
 * For setters: The generated setter for a Kotlin property will have one parameter, named
 * [propertyName]. This is true in Dackka even if the setter was explicitly defined with a different
 * parameter name (b/268236485). To work around this, any [Param] tags for setters are set to have
 * [propertyName] as the name of the parameter.
 *
 * For @property tags, they can be unwrapped for both getters and setters to just descriptions.
 */
private fun SourceSetDependent<DocumentationNode>.correctTagsInAccessorDocs(
    propertyName: String,
    getter: Boolean,
): SourceSetDependent<DocumentationNode> {
    return this.mapValues { entry ->
        DocumentationNode(
            entry.value.children.map {
                when (it) {
                    is Param -> {
                        // Getters should have no params, move text out into a description
                        if (getter) {
                            Description(it.root)
                        } // Setters have one param, named the same as the property (b/268236485)
                        else {
                            Param(it.root, propertyName)
                        }
                    }
                    is Property -> Description(it.root)
                    else -> it
                }
            },
        )
    }
}

private fun DRI.isAtJvmField(): Boolean = packageName == "kotlin.jvm" && classNames == "JvmField"

private fun Annotations.Annotation.isAtJvmField(): Boolean = dri.isAtJvmField()

internal fun DProperty.isJvmFieldAnnotated() =
    annotations(getAsJavaSourceSet()).any { it.isAtJvmField() }

/** Returns whether property is annotated as @JvmField */
fun DProperty.isJvmField(): Boolean {
    return isJvmFieldAnnotated() || isConstant()
}

internal fun List<DFunction>.names() = map { it.name }

@JvmName("internalAndThusKotlinOnly") internal fun List<DParameter>.names() = map { it.name }

@JvmName("internalAndThusKotlinOnlyAlso") internal fun List<DProperty>.names() = map { it.name }

/**
 * Attempts to get the `expect` source set for a Documentable, or wherever else a sourceset-agnostic
 * version of the function is defined. This is because we currently pull e.g. description
 * documentation from this kind of source set, and ignore e.g. `actual`s' documentation.
 *
 * Single-platform functions, regardless of function, will return sourceSets.singleOrNull
 * expect/actual classes and elements will return expectPresentInSet There are fallbacks for some
 * odd cases following that.
 */
internal fun Documentable.getExpectOrCommonSourceSet() =
    sourceSets.singleOrNull()
        ?: expectPresentInSet
        ?: sourceSets.singleOrNull { it.analysisPlatform == org.jetbrains.dokka.Platform.common }
        ?: sourceSets.singleOrNull { it.displayName.equalsPossiblyWithMain("common") }
        // b/254490320. The one case we expect to see this is package descriptions.
        // Below is a weak fallback for libraries with no common sourceSet.
        ?: sourceSets.singleOrNull { it.displayName.equalsIgnoreCase("jvmMain") }
        ?: sourceSets.singleOrNull { it.displayName.equalsIgnoreCase("androidMain") }
        ?: sourceSets.singleOrNull { it.displayName.equalsIgnoreCase("desktopMain") }
        ?: sourceSets.firstOrNull()
        ?: throw RuntimeException("No sourceSets present for ${this.className} $dri")

private fun String.equalsPossiblyWithMain(other: String) =
    this.equalsIgnoreCase(other) || this.equalsIgnoreCase(other + "main")

private fun String.equalsIgnoreCase(other: String) = this.uppercase() == other.uppercase()

/**
 * Used in as-Java docs and when getting JVM-exclusive annotations.
 *
 * Returns null if the documentable is not accessible in a jvm context.
 *
 * Uses common sourceSet as a fallback, because poorly-implemented JVM-exclusive annotations could
 * be there, and because java code can access Kotlin common code.
 */
internal fun Documentable.getAsJavaSourceSet() =
    sourceSets.singleOrNull { it.analysisPlatform == org.jetbrains.dokka.Platform.jvm }
        ?: sourceSets.singleOrNull { it.analysisPlatform == org.jetbrains.dokka.Platform.common }
