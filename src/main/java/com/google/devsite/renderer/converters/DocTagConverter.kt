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

import com.google.devsite.DocsSummaryList
import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.className
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultAnnotatedLink
import com.google.devsite.components.impl.DefaultDescriptionComponent
import com.google.devsite.components.impl.DefaultKmpTableRowSummaryItem
import com.google.devsite.components.impl.DefaultLink
import com.google.devsite.components.impl.DefaultParameterComponent
import com.google.devsite.components.impl.DefaultPlatformComponent
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableRowSummaryItem
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.DefaultTypeProjectionComponent
import com.google.devsite.components.impl.DefaultUnlink
import com.google.devsite.components.impl.UndocumentedSymbolDescriptionComponent
import com.google.devsite.components.symbols.AnnotatedLink
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.table.KmpTableRowSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableRowSummaryItem
import com.google.devsite.components.table.TableTitle
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.strictSingleOrNull
import java.io.File
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.transformers.documentables.isDeprecated
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Callable
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.DefinitelyNonNullable
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Dynamic
import org.jetbrains.dokka.model.JavaObject
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.PrimitiveJavaType
import org.jetbrains.dokka.model.Projection
import org.jetbrains.dokka.model.Star
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.TypeParameter
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.Variance
import org.jetbrains.dokka.model.Void
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.doc.Author
import org.jetbrains.dokka.model.doc.Constructor
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.NamedTagWrapper
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Pre
import org.jetbrains.dokka.model.doc.Property
import org.jetbrains.dokka.model.doc.Receiver
import org.jetbrains.dokka.model.doc.Return
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.doc.Throws
import org.jetbrains.dokka.model.doc.Version

/** Extracts the handwritten documentation from documentables into the correct components. */
internal class DocTagConverter(
    private val displayLanguage: Language,
    private val pathProvider: FilePathProvider,
    private val docsHolder: DocumentablesHolder,
    private val paramConverter: ParameterDocumentableConverter,
    private val annotationConverter: AnnotationDocumentableConverter,
) {
    // We currently assume all sample are in the common sourceSet. TODO KMP samples b/181224204
    // private val analysisMap = runBlocking { docsHolder.analysisMap() }

    /**
     * @param documentable the documentable we are getting the documentation of
     * @param deprecationAnnotation the @Deprecated annotation. Is a parameter because for e.g.
     *   getters, the annotation will be propagated manually from some higher element, so we can't
     *   just use documentable.deprecationAnnotation() in such a case.
     * @return the in-source hand-written element documentation
     */
    fun summaryDescription(
        documentable: Documentable,
        deprecationAnnotation: Annotations.Annotation? = documentable.deprecationAnnotation(),
    ): DescriptionComponent {
        return deprecationComponent(documentable, summary = true, deprecationAnnotation)
            ?: documentable.getDescription(summary = true)
    }

    /** metadata() can either take a WithSources Documentable, OR an isFromJava boolean */
    fun <T> metadata(
        documentable: T,
        returnType: TypeProjectionComponent? = null,
        paramNames: List<String> = emptyList(),
        deprecationAnnotation: Annotations.Annotation? = documentable.deprecationAnnotation(),
    ) where T : Documentable, T : WithSources =
        metadataImpl(documentable, returnType, paramNames, deprecationAnnotation)

    fun metadata(
        documentable: Documentable,
        returnType: TypeProjectionComponent? = null,
        paramNames: List<String> = emptyList(),
        deprecationAnnotation: Annotations.Annotation? = documentable.deprecationAnnotation(),
        isFromJava: Boolean,
    ) = metadataImpl(documentable, returnType, paramNames, deprecationAnnotation, isFromJava)

    /**
     * Returns a breakdown of the different metadata as a deprecation warning, description, and then
     * separate summaries. Examples include the list of parameters, return type, see also, throws,
     * etc.
     */
    private fun metadataImpl(
        documentable: Documentable,
        returnType: TypeProjectionComponent? = null,
        paramNames: List<String> = emptyList(),
        deprecationAnnotation: Annotations.Annotation? = documentable.deprecationAnnotation(),
        isFromJavaParam: Boolean? = null,
    ): List<ContextFreeComponent> {
        val isFromJava =
            if (isFromJavaParam != null) {
                isFromJavaParam
            } else {
                assert(documentable is WithSources)
                (documentable as WithSources).isFromJava()
            }
        val description = documentable.getDescription(summary = false)
        val deprecation = deprecationComponent(documentable, summary = false, deprecationAnnotation)
        val receiverParam = documentable.find<Receiver>()?.let { Param(it.root, "receiver") }
        val generics = if (documentable is WithGenerics) documentable.generics else emptyList()
        // Filter out tags which should instead be put into the Description, which is handled in the
        // getDescription method.
        val metadataTags =
            (listOfNotNull(receiverParam) + documentable.tags()).filter {
                !it.belongsInDescriptionOf(documentable)
            }
        val tagsByType = metadataTags.sortedWith(tagOrder(paramNames)).groupBy { it.javaClass }
        val tables =
            tagsByType.mapNotNull { (_, rawTags) ->
                var tags = handleUpstreamTagDuplication(documentable, rawTags, generics)
                if (tags.isEmpty()) return@mapNotNull null
                val firstTag = tags.first()
                if (documentable is DFunction && firstTag is Param) {
                    val propertyClass = Property::class.java as Class<*>
                    tags = tags + tagsByType[propertyClass].orEmpty()
                }
                // We know all the elements in `tags` will be of the same type, so we pick an
                // arbitrary
                // one to do the switching and then cast the list to its type.
                @kotlin.Suppress("UNCHECKED_CAST")
                try {
                    when (firstTag) {
                        is Param ->
                            params(
                                tags as List<NamedTagWrapper>,
                                generics,
                                documentable,
                                isFromJava,
                                documentable.getExpectOrCommonSourceSet(),
                            )
                        is Return -> returnType(tags as List<Return>, checkNotNull(returnType))
                        is Throws -> throws(tags as List<Throws>, documentable)
                        is See -> see(tags as List<See>, documentable)
                        is Sample -> null // Samples are handled in the description
                        is Property ->
                            throw RuntimeException("Should have been consumed in description!")
                        is CustomTagWrapper ->
                            docsHolder
                                .printWarningFor(
                                    "unrecognized javadoc tag @",
                                    documentable,
                                    brokenDocTag = firstTag,
                                )
                                .let { null }
                        is Since ->
                            docsHolder
                                .printWarningFor(
                                    "unsupported javadoc tag @",
                                    documentable,
                                    brokenDocTag = firstTag,
                                    additionalContext =
                                        ". Instead, autogenerate per go/dackka#api-since.",
                                )
                                .let { null }
                        is Constructor -> null // TODO("b/179999964: constructor")
                        is Description,
                        is Deprecated -> null // Documented separately in getDescription
                        is Receiver -> null // gets merged with @params
                        is Suppress ->
                            throw RuntimeException(
                                "Reaching the documentation generation step on a suppressed member should" +
                                    "be impossible! If you see this, file a bug on dackka. $documentable",
                            )
                        // These aren't tags we believe it is necessary to support
                        is Version,
                        is Author -> null
                    }
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Exception thrown while handling ${firstTag.className} tags $tags.",
                        e,
                    )
                }
            }

        return listOfNotNull(deprecation, description, *tables.toTypedArray())
    }

    // Turn both "E" and "<E>" to "E"
    private fun ungenerify(name: String): String {
        if (name.startsWith('<') && name.endsWith('>')) return name.drop(1).dropLast(1)
        return name
    }

    private fun TagWrapper.name() = ungenerify((this as NamedTagWrapper).name)

    private fun List<TagWrapper>.names() = this.map { it.name() }

    /* Tags, in particular for property parameters, are propagated multiple times in upstream.
     * For example, @param t t_doc class Foo(val t) has Parameter(t, t_doc) duplicated many times.
     * On the class itself, on each property parameter of that class, and on the constructor.
     * This function filters out inappropriately propagated docs, and enforces that documentation
     * applies only to existing properties and parameters.
     */
    private fun handleUpstreamTagDuplication(
        documentable: Documentable,
        tags: List<TagWrapper>,
        generics: List<DTypeParameter>,
    ): List<TagWrapper> {
        if (tags.first() is Param) {
            // This one is very strange, and I haven't been able to reproduce it in unit tests
            // A generic called "ToValue" is instead registered as being named "V"
            // TODO: Fix
            if (tags.names() == listOf("ToValue", "function") && generics.single().name == "V") {
                return tags.filter { (it as Param).name == "function" }
            }

            // Handle parameter properties, e.g. class AClass<Gen>(val propParam)
            when (documentable) {
                // doc is DClasslike. DClasslike's only valid @params are type params
                is DClasslike -> {
                    val genericNames = generics.map { it.name }
                    val (forGenerics, otherAtParams) = tags.partition { it.name() in genericNames }
                    if (otherAtParams.isEmpty()) return forGenerics
                    // Enforce that the propagated documentation makes sense somewhere. Specifically
                    // documentation primarily aimed at a constructor may wind up on the DClass
                    // if the parameter being documented is a primary constructor property parameter
                    val constructorParamNames =
                        (documentable as? WithConstructors)
                            ?.constructors
                            ?.map { constructor -> constructor.parameters.map { it.name!! } }
                            ?.flatten()
                            .orEmpty()
                    val otherNames =
                        (documentable.properties.map { it.name } + constructorParamNames)
                    val (validTags, badTags) = otherAtParams.partition { it.name() in otherNames }
                    badTags.forEach {
                        docsHolder.printWarningFor(
                            "Unable to find reference @",
                            documentable,
                            brokenDocTag = it,
                            additionalContext =
                                ". Are you trying to refer to something not visible to users?",
                        )
                    }
                    // Use only docs for type parameters in the parameter documentation table
                    return forGenerics
                }
                // doc is Property. It is possible that a parameter property is documented on the
                // class as @param. That doc is used as though it were @property. Handled there.
                // Properties can also have @param documentation for type parameters
                is DProperty -> {
                    val genericNames = generics.map { it.name }
                    val (forGenericsOrThis, badTags) =
                        tags.partition {
                            it.name() in genericNames || it.name() == documentable.name
                        }
                    badTags.forEach {
                        docsHolder.printWarningFor(
                            "Unable to find reference @",
                            documentable,
                            brokenDocTag = it,
                            additionalContext =
                                ". Are you trying to refer to something not visible to users?",
                        )
                    }
                    return forGenericsOrThis
                }
                is DFunction -> {}
                else -> throw RuntimeException("Can't apply @param to a ${documentable.className}")
            }
        } else if (tags.first() is Property) {
            // A DClasslike with @property applying to property parameters may have Parameter tags
            // In such a case, none of these tags should become docs *on the DClasslike itself*
            return when (documentable) {
                is DClasslike -> {
                    val propertyNames = documentable.properties.map { it.name }
                    tags
                        .filter { it.name() !in propertyNames }
                        .forEach {
                            docsHolder.printWarningFor(
                                "Unable to find reference @",
                                documentable,
                                brokenDocTag = it,
                            )
                        }
                    emptyList()
                }
                is DParameter,
                is DProperty -> tags
                else ->
                    throw RuntimeException("Can't apply @property to a ${documentable.className}")
            }
        }
        return tags
    }

    private fun params(
        tags: List<NamedTagWrapper>,
        dGenerics: List<DTypeParameter>,
        documentable: Documentable,
        isFromJava: Boolean,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
    ): DocsSummaryList {
        val tagged = tags.map { it.name() }.toSet()

        // @param can refer to parameters, lambda parameters, type parameters, or receivers.
        val allOptions = mutableMapOf<String, ParameterComponent>()
        if (documentable is DFunction) {
            allOptions.putAll(
                documentable.parameters.mapNotNull {
                    val name = it.name!!
                    if (!tagged.contains(name)) {
                        // Synthetic receiver params don't need @param documentation
                        if (name == "receiver") return@mapNotNull null
                        // This warning is primarily for blocking newly-added code
                        if (documentable.isDeprecated()) return@mapNotNull null
                        docsHolder.printWarningFor(
                            "Missing @param tag for parameter `$name`",
                            documentable,
                        )
                    }
                    name to
                        paramConverter.componentForParameter(
                            param = it,
                            isSummary = false,
                            isFromJava = isFromJava,
                            parent = documentable,
                        )
                },
            )
            allOptions.putAll(
                recursivelyGetLambdaParamNames(documentable.parameters.map { it.type }).map {
                    (it.presentableName ?: "") to
                        paramConverter.componentForLambdaParameter(it, isFromJava, sourceSet)
                },
            )
        }
        allOptions.putAll(
            dGenerics.map { it.name to paramConverter.componentForTypeParameter(it, isFromJava) },
        )
        if (documentable is Callable && documentable.receiver != null) {
            allOptions[documentable.receiver!!.name ?: "receiver"] =
                paramConverter.componentForParameter(
                    param = documentable.receiver!!,
                    isSummary = false,
                    isFromJava = isFromJava,
                    parent = documentable,
                )
        }
        val params =
            tags.map { tag ->
                if (allOptions[tag.name()] == null) {
                    throw RuntimeException(
                        "Unable to find what is referred to by \"@param " +
                            "${tag.name()}\" in ${documentable.className} " +
                            "${documentable.name}, with contents: ${tag.text()}",
                    )
                }
                val title = allOptions[tag.name()]!!
                DefaultTableRowSummaryItem(
                    TableRowSummaryItem.Params(
                        title = title,
                        description = description(tag),
                    ),
                )
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("Parameters")),
                items = params,
            ),
        )
    }

    /** For example, "a" and "b" in `fun foo((a: (b: String) -> int)) -> Unit)` */
    private fun recursivelyGetLambdaParamNames(
        argumentTypes: List<Projection>,
    ): List<TypeConstructor> {
        val result = mutableListOf<TypeConstructor>()
        for (argumentType in argumentTypes) {
            when (argumentType) {
                is TypeConstructor -> {
                    if (argumentType.presentableName != null) result += argumentType
                    result += recursivelyGetLambdaParamNames(argumentType.projections)
                }
                is Variance<*> -> {
                    result += recursivelyGetLambdaParamNames(listOf(argumentType.inner))
                }
                is Nullable -> {
                    result += recursivelyGetLambdaParamNames(listOf(argumentType.inner))
                }
                is DefinitelyNonNullable -> {
                    result += recursivelyGetLambdaParamNames(listOf(argumentType.inner))
                }
                is TypeParameter -> {
                    /* Type parameters can't be lambdas, and are fully squashed to strings. */
                }
                is TypeAliased -> { // No clear way to decide which
                    result +=
                        recursivelyGetLambdaParamNames(
                            setOf(argumentType.inner, argumentType.typeAlias).toList(),
                        )
                }
                is PrimitiveJavaType,
                is JavaObject,
                Void,
                Dynamic,
                Star -> {
                    /* Do nothing */
                }
                is UnresolvedBound -> {
                    /* Nothing we can do. We warn elsewhere for this case. */
                }
            }
        }
        return result
    }

    private fun returnType(
        tags: List<Return>,
        returnType: TypeProjectionComponent
    ): SummaryList<TableRowSummaryItem<TypeProjectionComponent, DescriptionComponent>> {
        val params =
            tags.map { tag ->
                DefaultTableRowSummaryItem(
                    TableRowSummaryItem.Params(
                        title = returnType,
                        description = description(tag),
                    ),
                )
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("Returns")),
                items = params,
            ),
        )
    }

    private fun throws(tags: List<Throws>, parent: Documentable): DocsSummaryList {
        val params =
            tags.map { tag ->
                DefaultTableRowSummaryItem(
                    TableRowSummaryItem.Params(
                        title = throwsToParameterComponent(tag, parent),
                        description = description(tag),
                    ),
                )
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("Throws")),
                items = params,
            ),
        )
    }

    private fun String.firstWord() = substring(0, indexOfFirst { it == ' ' })

    private fun throwsToParameterComponent(
        throws: Throws,
        parent: Documentable,
    ): ParameterComponent {
        val name = throws.name
        var dri: DRI? = throws.exceptionAddress
        if (throws.name in listOf("a", "an")) {
            throw RuntimeException(
                "Do not use '${throws.name}' before the exception type in an @throws statement. " +
                    "This is against jdoc spec. Your exception is not being linked and looks bad",
            )
        } else if ("{@link" in name) {
            throw RuntimeException(
                "Do not {@link the exception type in an @throws statement. @throws state" +
                    "ments are automatically linked. Manually java-linking them is against jdoc s" +
                    "pec, and breaks linking behavior causing them to actually *not* be linked.",
            )
        } else if (throws.exceptionAddress == null) {
            docsHolder.printWarningFor(
                "Link does not resolve for @",
                parent,
                brokenDocTag = throws,
                additionalContext =
                    ". Is it from a package that the containing file does not " +
                        "import? Are docs inherited by an un-documented override function, but the " +
                        "exception class is not in scope in the inheriting class? The general fix for" +
                        " these is to fully qualify the exception name, e.g. " +
                        "`@throws java.io.IOException under some conditions`.",
            )
            dri = null
        }
        val link =
            if (dri == null) {
                DefaultLink(Link.Params(name, url = ""))
            } else pathProvider.linkForReference(throws.exceptionAddress!!, name)

        return DefaultParameterComponent(
            ParameterComponent.Params(
                name = "",
                type =
                    DefaultTypeProjectionComponent(
                        TypeProjectionComponent.Params(
                            type = link,
                            nullability = Nullability.DONT_CARE,
                            displayLanguage = displayLanguage,
                        ),
                    ),
                displayLanguage = displayLanguage,
            ),
        )
    }

    private fun see(tags: List<See>, parent: Documentable): LinkDescriptionSummaryList {
        val params =
            tags.map { tag ->
                DefaultTableRowSummaryItem(
                    TableRowSummaryItem.Params(
                        title = tag.toLink(parent),
                        description = description(tag),
                    ),
                )
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header = DefaultTableTitle(TableTitle.Params("See also")),
                items = params,
            ),
        )
    }

    /**
     * Gets a Description for the Documentable, or returns UndocumentedSymbolDescription() Has
     * special handling to inject @property documentation as a description, if it exists Also
     * applies to @param documentation that should become a description, i.e. property params
     */
    private fun Documentable.getDescription(summary: Boolean): DescriptionComponent {
        val components = mutableListOf<DocTag>()
        tags().forEach {
            when (it) {
                is Sample -> {
                    val dri = it.name
                    // val sourceSet = this.getExpectOrCommonSourceSet()
                    /* val analysisEnvironment = analysisMap[sourceSet]!!
                    val sample = try {
                        analysisEnvironment
                            .resolveSample(sourceSet, dri)!!
                    } catch (e: NullPointerException) {
                        throw RuntimeException("Unable to resolve sample $dri!")
                    }*/
                    // TODO(KMP) we currently have no plan to provide KMP samples b/181224204
                    // As such, we currently assume that all samples are in common
                    val sample =
                        docsHolder.sampleAnalysisEnvironment.value.resolveSample(
                            docsHolder.commonSourceSet,
                            dri
                        ) ?: throw RuntimeException("Unable to resolve sample $dri")
                    val imports = processImports(sample)

                    components.add(
                        Pre(
                            params = mapOf("class" to "prettyprint lang-kotlin"),
                            children = listOf(Text(imports + sample.body)),
                        ),
                    )
                    components.addAll(it.children)
                }
                is Description,
                is NamedTagWrapper -> {
                    if (!it.belongsInDescriptionOf(this)) return@forEach
                    it.children.forEach { child ->
                        try {
                            recursivelyConsiderPsAndTextsForJavaSamples(
                                child,
                                components,
                                this.getExpectOrCommonSourceSet().samples,
                            )
                        } catch (e: Exception) {
                            throw RuntimeException(
                                "Error when resolving samples when processing $name",
                                e,
                            )
                        }
                    }
                }
                is Author,
                is Version -> {} // These are not supported
                is Return,
                is Receiver,
                is Constructor -> {} // These become tables in metadata()
                is Deprecated,
                is Suppress,
                is Since -> {} // These are handled elsewhere
            }
        }
        if (components.isEmpty()) return UndocumentedSymbolDescriptionComponent
        return description(components, summary, null)
    }

    /**
     * Tags with the same name as the documentable should generally go in the description component,
     * but sometimes the tag refers to something else with the same name, like a parameter that has
     * the same name as its function.
     */
    private fun TagWrapper.belongsInDescriptionOf(documentable: Documentable): Boolean {
        if (this is Description) return true
        if (this !is NamedTagWrapper) return false
        return when (this) {
            is Param,
            is Property ->
                (documentable is DParameter || documentable is DProperty) &&
                    (this.name == documentable.name)
            is Throws,
            is See -> false
            else -> true
        }
    }

    /** annotation-sampled and SampledAnnotationDetector */
    private fun DocTag.explicitlyBanLookingForSamples() =
        "Functions referenced with @sample are annotated with @Sampled" in text() ||
            "Denotes that the annotated function is considered a sample function" in text() ||
            "that functions referred to from KDoc with a @sample tag are annotated" in text()

    private fun recursivelyConsiderPsAndTextsForJavaSamples(
        root: DocTag,
        components: MutableList<DocTag>,
        samples: Set<File>,
    ) {
        if ("@sample" !in root.text() || root.explicitlyBanLookingForSamples()) {
            components.add(root)
            return
        }
        when (root) {
            is Text -> {
                val parts = root.body.split("{", "}")
                for (part in parts) {
                    if ("@sample" !in part) {
                        if (part.isNotBlank()) components.add(Text(part.trim()))
                    } else components.add(convertTextToJavadocSample(Text(part.trim()), samples))
                }
            }
            is P -> {
                for (child in root.children) {
                    recursivelyConsiderPsAndTextsForJavaSamples(child, components, samples)
                }
            }
            // Having non-text components on the same line as a samples is not supported
            else -> throw RuntimeException("considered invalid type ${root.className} for sample")
        }
    }

    private fun WithChildren<DocTag>.text(): String {
        return if (this is Text) this.body else children.joinToString(" ") { it.text() }
    }

    private fun description(
        soleComponent: TagWrapper,
        summary: Boolean = false,
        deprecation: String? = null,
    ): DescriptionComponent {
        return description(soleComponent.children, summary, deprecation)
    }

    private fun description(
        components: List<DocTag> = emptyList(),
        summary: Boolean = false,
        deprecation: String? = null,
    ): DescriptionComponent {
        return DefaultDescriptionComponent(
            DescriptionComponent.Params(
                pathProvider,
                components,
                summary,
                deprecation,
                docsHolder,
            ),
        )
    }

    /** Returns the component for a deprecation. */
    private fun deprecationComponent(
        documentable: Documentable,
        summary: Boolean,
        deprecationAnnotation: Annotations.Annotation?,
    ): DescriptionComponent? {
        val deprecation = findDeprecation(documentable, deprecationAnnotation) ?: return null
        return description(deprecation.children, summary, documentable.deprecationText())
    }

    /**
     * Finds either the javadoc @deprecated tag or the Kotlin @Deprecated annotation [TagWrapper].
     */
    private fun findDeprecation(
        documentable: Documentable,
        deprecationAnnotation: Annotations.Annotation?,
    ): Deprecated? {
        val javadocDeprecation = documentable.find<Deprecated>()
        if (javadocDeprecation != null) {
            // Prefer javadoc deprecation messages since they allow formatting
            return javadocDeprecation
        }

        val annotationDeprecationMessage =
            (deprecationAnnotation?.params?.get("message") as? StringValue)?.value ?: return null
        // Dokka makes message="foo" show up as "\"foo\"" since you typically want to show quotes
        // when rendering an annotation. Remove those outer quotes.
        val message = annotationDeprecationMessage.removeSurrounding("\"")

        return Deprecated(P(children = listOf(Text(message))))
    }

    private fun Documentable.deprecationText() =
        "This ${this.stringForType(displayLanguage)} is deprecated."

    /** Retrieves the doc tags of type [T]. */
    private inline fun <reified T> Documentable.find() =
        tags().filterIsInstance<T>().strictSingleOrNull()

    /**
     * @return the doc tags (aka human-written javadoc or kdoc) associated with this documentable
     */
    private fun Documentable.tags() =
        documentation[getExpectOrCommonSourceSet()]?.children ?: emptyList()

    private fun tagOrder(paramNames: List<String>) =
        compareBy<TagWrapper> { tag ->
                when (tag) {
                    is Deprecated -> 0
                    is Description -> 1
                    is Return -> 2
                    is Constructor -> 3
                    is Property -> 4
                    is Receiver -> 5
                    is Param -> 6
                    is Throws -> 7
                    is See -> 8
                    is Sample -> 9
                    is Since -> 10
                    is Version -> 11
                    is Author -> 12
                    is Suppress -> 13
                    is CustomTagWrapper -> 14
                }
            }
            .thenBy { tag ->
                when (tag) {
                    is Param -> paramNames.indexOf(tag.name)
                    else -> -1
                }
            }

    /**
     * Extract the see tag's reference into a link.
     *
     * This one is painful. An address is only sometimes there, other times there's a docs link
     * nested somewhere in the tree, and as a last resort the name is always present with whatever a
     * developer writes which could either be a fully qualified reference or just the URL fragment.
     */
    private fun See.toLink(parent: Documentable): Link {
        val address = address
        if (address != null) {
            return pathProvider.linkForReference(address)
        }
        val docsLink = root.explodedChildren.filterIsInstance<DocumentationLink>().singleOrNull()
        if (docsLink != null) {
            return pathProvider.linkForReference(docsLink.dri)
        }

        // TODO(b/167437580): figure out how to reliably parse links
        if (name == "") {
            @kotlin.Suppress("UNCHECKED_CAST")
            val aHrefFormatted =
                root.children.singleOrNull()?.children as? List<Text>
                    ?: throw RuntimeException("Could not understand link: $this")
            if (aHrefFormatted.size == 3)
                return DefaultLink(
                    Link.Params(
                        name = aHrefFormatted[1].body,
                        url = aHrefFormatted[0].body.removePrefix("<a href=").removeSuffix(">"),
                        externalLink = true
                    )
                )
            throw RuntimeException("Could not understand link: $this")
        }
        if (name.startsWith("<a href=")) {
            val (url, linkName) = name.removePrefix("<a href=").removeSuffix("</a>").split(">")
            return DefaultLink(Link.Params(name = linkName, url = url, externalLink = true))
        }
        val segments = name.split("#")
        return if (segments.size == 1) {
            val (packageName, typeName) = typeToPackageNameAndType(segments.single())
            if (typeName.isEmpty()) {
                var additionalContext = ""
                // Maybe the link is `package.Class.aFunction` instead of `package.Class#aFunction`?
                val last = name.substringAfterLast(".")
                val rest = name.substringBeforeLast(".")
                if (last.firstOrNull()?.isLowerCase() == true && rest.any { it.isUpperCase() }) {
                    additionalContext = ". Did you mean $rest#$last?"
                    val (packageN, typeN) = typeToPackageNameAndType(rest)
                    val url = pathProvider.forType(packageN, typeN)
                    DefaultLink(Link.Params(typeN, "$url#$last"))
                }
                docsHolder.printWarningFor(
                    "Failed to resolve ",
                    parent,
                    brokenDocTag = this,
                    additionalContext = additionalContext,
                )
                DefaultLink(Link.Params(name, url = ""))
            } else if (packageName.isEmpty()) {
                // This is a same-package type link, though we sadly can't prove it's correct
                pathProvider.linkForReference(DRI("", typeName))
            } else {
                pathProvider.linkForReference(DRI(packageName, typeName))
            }
        } else if (segments.size == 2) {
            val (type, anchor) = segments
            if (type.isEmpty()) {
                // Self link
                DefaultLink(Link.Params(anchor, anchor))
            } else {
                // Assume link with anchor
                val (packageName, typeName) = typeToPackageNameAndType(type)
                val url = pathProvider.forType(packageName, typeName)
                DefaultLink(Link.Params(typeName, "$url#$anchor"))
            }
        } else {
            throw RuntimeException("Could not understand path: $name")
        }
    }

    /** Horrible guess-work to try and extract the package and type names. */
    private fun typeToPackageNameAndType(full: String): Pair<String, String> {
        val parts = full.split(".")

        if (parts.size == 1) return "" to full

        val packageName = parts.takeWhile { it.all(Char::isLowerCase) }.joinToString(".")
        val typeName =
            parts
                .takeLastWhile {
                    if (it.isEmpty()) {
                        throw RuntimeException("empty element in TTPNAT. Full: $full")
                    } else it.first().isUpperCase()
                }
                .joinToString(".")
        return packageName to typeName
    }

    internal fun docsToSummaryDefault(documentables: List<Documentable>) =
        docsToSummary(documentables, false)

    /**
     * Converts a generic List<Documentable> to a SummaryList. Does nothing clever; only converts
     * Documentables to links (by default with annotations)
     */
    private fun docsToSummary(
        documentables: List<Documentable>,
        showAnnotations: Boolean,
    ) =
        DefaultSummaryList(
            SummaryList.Params(
                items = documentables.map { summaryForDocumentable(it, showAnnotations) },
            ),
        )

    /**
     * Converts generic Documentables to TableRowSummaryItems, as simple maybe-annotated links This
     * is used for mini-signatures, e.g. nested types list, subclasses list, package summary
     */
    internal fun summaryForDocumentable(
        documentable: Documentable,
        showAnnotations: Boolean = false,
    ): TableRowSummaryItem<Link, DescriptionComponent> {
        val link =
            if (documentable is DTypeAlias) {
                // typealiases have no pages
                DefaultUnlink(Link.Params(documentable.name, ""))
            } else pathProvider.linkForReference(documentable.dri)
        val maybeAnnotatedLink =
            if (showAnnotations) {
                DefaultAnnotatedLink(
                    AnnotatedLink.Params(
                        annotations =
                            annotationConverter.annotationComponents(
                                documentable.annotations(documentable.getExpectOrCommonSourceSet()),
                                nullability = Nullability.DONT_CARE, // Not useful for these cases
                            ),
                        link = link,
                    ),
                )
            } else {
                link
            }
        return DefaultTableRowSummaryItem(
            TableRowSummaryItem.Params(
                title = maybeAnnotatedLink,
                description =
                    summaryDescription(
                        documentable,
                        documentable.deprecationAnnotation(),
                    ),
            ),
        )
    }

    /**
     * Converts a generic List<Documentable> to a SummaryList. Does nothing clever; only converts
     * Documentables to links (by default with annotations)
     */
    internal fun docsToSummaryKmp(
        documentables: List<Documentable>,
    ) =
        DefaultSummaryList(
            SummaryList.Params(
                items = documentables.map { summaryForDocumentableKmp(it) },
            ),
        )

    /**
     * Converts generic Documentables to TableRowSummaryItems, as simple maybe-annotated links This
     * is used for mini-signatures, e.g. nested types list, subclasses list, package summary
     */
    private fun summaryForDocumentableKmp(
        documentable: Documentable,
    ): TableRowSummaryItem<Link, DescriptionComponent> {
        return DefaultKmpTableRowSummaryItem(
            KmpTableRowSummaryItem.Params(
                title = pathProvider.linkForReference(documentable.dri),
                description = summaryDescription(documentable),
                platforms = DefaultPlatformComponent(documentable.sourceSets),
            ),
        )
    }
}
