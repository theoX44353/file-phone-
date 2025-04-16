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

import com.google.devsite.ClasslikeSummaryList
import com.google.devsite.ConstructorSummaryList
import com.google.devsite.FunctionSummaryList
import com.google.devsite.LinkDescriptionSummaryList
import com.google.devsite.PropertySummaryList
import com.google.devsite.TypeSummaryItem
import com.google.devsite.className
import com.google.devsite.components.Link
import com.google.devsite.components.impl.DefaultClassHierarchy
import com.google.devsite.components.impl.DefaultClasslike
import com.google.devsite.components.impl.DefaultClasslikeDescription
import com.google.devsite.components.impl.DefaultClasslikeSignature
import com.google.devsite.components.impl.DefaultClasslikeSummary
import com.google.devsite.components.impl.DefaultDevsitePage
import com.google.devsite.components.impl.DefaultDevsitePlatformSelector
import com.google.devsite.components.impl.DefaultInheritedSymbols
import com.google.devsite.components.impl.DefaultReferenceObject
import com.google.devsite.components.impl.DefaultRelatedSymbols
import com.google.devsite.components.impl.DefaultSummaryList
import com.google.devsite.components.impl.DefaultTableRowSummaryItem
import com.google.devsite.components.impl.DefaultTableTitle
import com.google.devsite.components.impl.emptyInheritedSymbolsList
import com.google.devsite.components.impl.emptySummaryList
import com.google.devsite.components.pages.Classlike
import com.google.devsite.components.pages.Classlike.TitledList
import com.google.devsite.components.pages.DevsitePage
import com.google.devsite.components.symbols.ClasslikeDescription
import com.google.devsite.components.symbols.ClasslikeSignature
import com.google.devsite.components.symbols.ClasslikeSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.ReferenceObject
import com.google.devsite.components.symbols.SymbolDetail
import com.google.devsite.components.symbols.SymbolSignature
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeProjectionComponent
import com.google.devsite.components.table.ClassHierarchy
import com.google.devsite.components.table.InheritedSymbolsList
import com.google.devsite.components.table.RelatedSymbols
import com.google.devsite.components.table.SummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableRowSummaryItem
import com.google.devsite.components.table.TableTitle
import com.google.devsite.hasBeenHidden
import com.google.devsite.renderer.Language
import com.google.devsite.renderer.impl.ClassGraph
import com.google.devsite.renderer.impl.DocumentablesHolder
import com.google.devsite.renderer.impl.paths.FilePathProvider
import com.google.devsite.renderer.not
import com.google.devsite.strictSingleOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.model.ActualTypealias
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Bound
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExtraModifiers
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.model.KotlinModifier
import org.jetbrains.dokka.model.WithConstructors
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.WithSupertypes
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.model.toAdditionalModifiers

/** Converts documentable class-likes into the classlike component. */
internal abstract class ClasslikeDocumentableConverter(
    private val displayLanguage: Language,
    protected val classlike: DClasslike,
    private val pathProvider: FilePathProvider,
    protected val docsHolder: DocumentablesHolder,
    protected val functionConverter: FunctionDocumentableConverter,
    protected val propertyConverter: PropertyDocumentableConverter,
    protected val enumConverter: EnumValueDocumentableConverter, // TODO(KMP b/256172699)
    protected val javadocConverter: DocTagConverter, // TODO(KMP b/254490320)
    protected val paramConverter: ParameterDocumentableConverter,
    private val annotationConverter: AnnotationDocumentableConverter,
    private val metadataConverter: MetadataConverter,
) {
    protected abstract val header: DefaultDevsitePlatformSelector?
    protected abstract val functionToSummaryConverter:
        (DFunction, ModifierHints) -> TypeSummaryItem<FunctionSignature>?
    protected abstract val functionToDetailConverter:
        (DFunction, ModifierHints) -> SymbolDetail<FunctionSignature>?
    protected abstract val propertyToSummaryConverter:
        (DProperty, ModifierHints) -> TypeSummaryItem<PropertySignature>?
    protected abstract val propertyToDetailConverter:
        (DProperty, ModifierHints) -> SymbolDetail<PropertySignature>?
    protected abstract val constructorToSummaryConverter:
        (DFunction) -> TableRowSummaryItem<Nothing?, SymbolSummary<FunctionSignature>>?
    protected abstract val constructorToDetailConverter:
        (DFunction, ModifierHints) -> SymbolDetail<FunctionSignature>?

    /** @return the classlike component */
    suspend fun classlike(): DevsitePage<Classlike> = coroutineScope {
        val (unsortedCompanionFunctions, unsortedCompanionProperties) =
            classlike.companionFunctionsAndProperties()
        val companionFunctions = unsortedCompanionFunctions.sortedWith(functionSignatureComparator)
        val companionProperties =
            unsortedCompanionProperties.sortedWith(simpleDocumentableComparator)

        val (initialFunctions, initialProperties) = classlike.nonInheritedTypes()
        val inheritedAll = classlike.inheritedTypes(classlike.supertypesForDisplayLanguage())

        val declaredFunctions =
            computeDeclaredFunctions(initialFunctions, companionFunctions)
                .sortedWith(functionSignatureComparator)
        val declaredProperties =
            computeDeclaredProperties(initialProperties, companionProperties)
                .sortedWith(simpleDocumentableComparator)

        val enumValues =
            (classlike as? DEnum)?.entries.orEmpty().sortedWith(simpleDocumentableComparator)

        val allConstructors =
            (classlike as? WithConstructors)
                ?.constructors
                .orEmpty()
                .sortedWith(functionSignatureComparator)
        val enumValuesSummary = async { enumValuesToSummary(enumValuesTitle(), enumValues) }
        val nestedTypesSummary = async {
            nestedTypesToSummary(
                // These are filtered for not-shown classlikes when accessed
                docsHolder.nestedClasslikesFor(classlike),
                docsHolder.classGraph(),
            )
        }
        val constantsSummary = async {
            propertiesToSummary(
                constantsTitle(),
                declaredProperties.constants(),
            )
        }
        val publicPropertiesSummary = async {
            propertiesToSummary(
                publicPropertiesTitle(displayLanguage),
                declaredProperties.filter(::isPublicNonConst),
            )
        }
        val protectedPropertiesSummary = async {
            propertiesToSummary(
                protectedPropertiesTitle(displayLanguage),
                declaredProperties.filter(::isProtectedNonConst),
            )
        }
        val publicConstructorsSummary = async {
            constructorsToSummary(
                publicConstructorsTitle(),
                allConstructors.filter(::isPublic),
            )
        }
        val protectedConstructorsSummary = async {
            constructorsToSummary(
                protectedConstructorsTitle(),
                allConstructors.filter(::isProtected),
            )
        }
        val publicFunctionsSummary = async {
            functionsToSummary(
                publicMethodsTitle(displayLanguage),
                declaredFunctions.filter(::isPublic),
            )
        }
        val protectedFunctionsSummary = async {
            functionsToSummary(
                protectedMethodsTitle(displayLanguage),
                declaredFunctions.filter(::isProtected),
            )
        }
        val publicCompanionFunctionsSummary = async {
            emptyIfJava()
                ?: functionsToSummary(
                    publicCompanionFunctionsTitle(),
                    companionFunctions.filter(::isPublic),
                )
        }
        val protectedCompanionFunctionsSummary = async {
            emptyIfJava()
                ?: functionsToSummary(
                    protectedCompanionFunctionsTitle(),
                    companionFunctions.filter(::isProtected),
                )
        }
        val publicCompanionPropertiesSummary = async {
            emptyIfJava()
                ?: propertiesToSummary(
                    publicCompanionPropertiesTitle(),
                    companionProperties.filter(::isPublicNonConst),
                )
        }
        val protectedCompanionPropertiesSummary = async {
            emptyIfJava()
                ?: propertiesToSummary(
                    protectedCompanionPropertiesTitle(),
                    companionProperties.filter(::isProtectedNonConst),
                )
        }

        val enumDetails =
            (classlike as? DEnum)?.let { async { enumValuesToDetail(it, enumValues) } }
        val constantsDetails = async { propertiesToDetail(declaredProperties.constants()) }
        val publicPropertiesDetails = async {
            propertiesToDetail(declaredProperties.filter(::isPublicNonConst))
        }
        val protectedPropertiesDetails = async {
            propertiesToDetail(declaredProperties.filter(::isProtectedNonConst))
        }
        val publicConstructorsDetails = async {
            constructorsToDetail(allConstructors.filter(::isPublic))
        }
        val protectedConstructorsDetails = async {
            constructorsToDetail(allConstructors.filter(::isProtected))
        }
        val publicFunctionsDetails = async {
            functionsToDetail(declaredFunctions.filter(::isPublic))
        }
        val protectedFunctionsDetails = async {
            functionsToDetail(declaredFunctions.filter(::isProtected))
        }
        val publicCompanionFunctionsDetail = async {
            emptyListIfJava() ?: functionsToDetail(companionFunctions.filter(::isPublic))
        }
        val protectedCompanionFunctionsDetail = async {
            emptyListIfJava() ?: functionsToDetail(companionFunctions.filter(::isProtected))
        }
        val publicCompanionPropertiesDetail = async {
            emptyListIfJava() ?: propertiesToDetail(companionProperties.filter(::isPublicNonConst))
        }
        val protectedCompanionPropertiesDetail = async {
            emptyListIfJava()
                ?: propertiesToDetail(companionProperties.filter(::isProtectedNonConst))
        }

        val inheritedTypes = async { computeInheritedSymbols(inheritedAll) }
        val metadataComponent = async { metadataConverter.getMetadataForClasslike(classlike) }

        // Note: getters and setters of extension properties are already included in
        // classExtensionFunctions from DocumentablesHolder.extensionFunctionMap
        var extensionFunctions =
            docsHolder
                .extensionFunctionsFor(classlike)
                // Sort by the class the extension function came from first, so they will be grouped
                // together in a logical way
                .sortedWith(
                    compareBy<DFunction> { nameForSyntheticClass(it) }
                        .then(functionSignatureComparator),
                )
                // Convert DRIs to this class so link from summary to detail will stay on class page
                .map { it.withDRIOfClass(classlike) }
        if (displayLanguage == Language.JAVA) {
            extensionFunctions = extensionFunctions.filterNot { it.isSuspendFunction() }
        }
        val extensionFunctionsSummary = async {
            functionsToSummary(extensionFunctionsTitle(), extensionFunctions)
        }
        val extensionFunctionsDetail = async { functionsToDetail(extensionFunctions) }

        val extensionProperties =
            docsHolder
                .extensionPropertiesFor(classlike)
                // Sort by the class the extension property came from first, so they will be grouped
                // together in a logical way
                .sortedWith(
                    compareBy<DProperty> { nameForSyntheticClass(it) }
                        .then(simpleDocumentableComparator),
                )
                // Convert DRIs to this class so link from summary to detail will stay on class page
                .map { it.withDRIOfClass(classlike) }
        val extensionPropertiesSummary = async {
            propertiesToSummary(extensionPropertiesTitle(), extensionProperties)
        }
        val extensionPropertiesDetail = async { propertiesToDetail(extensionProperties) }

        val (inheritedFunctions, inheritedConstants, inheritedProperties) = inheritedTypes.await()

        // Java-only (synthetic) classlikes' members are in the package summary in Kotlin
        val isJavaOnlyClasslike = docsHolder.isFromSyntheticClass(classlike.dri)
        // Kotlin-only (non-JVM-target) members can't be used from a JVM target at all
        val isKotlinOnlyNonJVMClasslike =
            this@ClasslikeDocumentableConverter is KmpClasslikeConverter &&
                classlike.getExpectOrCommonSourceSet().analysisPlatform !in
                    listOf(org.jetbrains.dokka.Platform.common, org.jetbrains.dokka.Platform.jvm)
        // Some packages (e.g. compose) are explicitly set to be only displayed in one language
        val isNotDisplayedForOtherLanguage =
            docsHolder.excludedPackages[displayLanguage.not()]!!.any {
                it.matches(classlike.packageName())
            }
        // Some companion objects are 'boring' (everything is hoisted) in only one language. They
        // switch to their parent class.
        val isBoringInOnlyTheOtherDisplayLanguage =
            (classlike is DObject) &&
                !docsHolder.interestingness(classlike).interestingIn(displayLanguage.not())

        val pathForSwitcher =
            when {
                (isNotDisplayedForOtherLanguage || isKotlinOnlyNonJVMClasslike) -> null
                isJavaOnlyClasslike || isBoringInOnlyTheOtherDisplayLanguage ->
                    pathProvider.forReference(classlike.dri.parent).url
                else -> pathProvider.forReference(classlike.dri).url
            }

        DefaultDevsitePage(
            DevsitePage.Params(
                displayLanguage,
                pathForSwitcher = pathForSwitcher?.removePrefix(pathProvider.rootPath + "/"),
                bookPath = pathProvider.book,
                title = classlike.name(),
                content =
                    DefaultClasslike(
                        Classlike.Params(
                            description = getClasslikeDescription(),
                            displayLanguage = displayLanguage,
                            nestedTypesSummary = nestedTypesSummary.await(),
                            enumValuesSummary = enumValuesSummary.await(),
                            enumValuesDetails =
                                TitledList(
                                    enumValuesTitle(),
                                    enumDetails?.await() ?: emptyList(),
                                ),
                            constantsSummary = constantsSummary.await(),
                            constantsDetails =
                                TitledList(
                                    constantsTitle(),
                                    constantsDetails.await(),
                                ),
                            publicCompanionFunctionsSummary =
                                publicCompanionFunctionsSummary.await(),
                            publicCompanionFunctionsDetails =
                                TitledList(
                                    publicCompanionFunctionsTitle(),
                                    publicCompanionFunctionsDetail.await(),
                                ),
                            protectedCompanionFunctionsSummary =
                                protectedCompanionFunctionsSummary.await(),
                            protectedCompanionFunctionsDetails =
                                TitledList(
                                    protectedCompanionFunctionsTitle(),
                                    protectedCompanionFunctionsDetail.await(),
                                ),
                            publicCompanionPropertiesSummary =
                                publicCompanionPropertiesSummary.await(),
                            publicCompanionPropertiesDetails =
                                TitledList(
                                    publicCompanionPropertiesTitle(),
                                    publicCompanionPropertiesDetail.await(),
                                ),
                            protectedCompanionPropertiesSummary =
                                protectedCompanionPropertiesSummary.await(),
                            protectedCompanionPropertiesDetails =
                                TitledList(
                                    protectedCompanionPropertiesTitle(),
                                    protectedCompanionPropertiesDetail.await(),
                                ),
                            publicConstructorsSummary = publicConstructorsSummary.await(),
                            publicConstructorsDetails =
                                TitledList(
                                    publicConstructorsTitle(),
                                    publicConstructorsDetails.await(),
                                ),
                            protectedConstructorsSummary = protectedConstructorsSummary.await(),
                            protectedConstructorsDetails =
                                TitledList(
                                    protectedConstructorsTitle(),
                                    protectedConstructorsDetails.await(),
                                ),
                            publicFunctionsSummary = publicFunctionsSummary.await(),
                            publicFunctionsDetails =
                                TitledList(
                            publicMethodsTitle(displayLanguage),
                                    publicFunctionsDetails.await(),
                                ),
                            protectedFunctionsSummary = protectedFunctionsSummary.await(),
                            protectedFunctionsDetails =
                                TitledList(
                                    protectedMethodsTitle(displayLanguage),
                                    protectedFunctionsDetails.await(),
                                ),
                            publicPropertiesSummary = publicPropertiesSummary.await(),
                            publicPropertiesDetails =
                                TitledList(
                                    publicPropertiesTitle(displayLanguage),
                                    publicPropertiesDetails.await(),
                                ),
                            protectedPropertiesSummary = protectedPropertiesSummary.await(),
                            protectedPropertiesDetails =
                                TitledList(
                                    protectedPropertiesTitle(displayLanguage),
                                    protectedPropertiesDetails.await(),
                                ),
                            extensionFunctionsSummary = extensionFunctionsSummary.await(),
                            extensionFunctionsDetails =
                                TitledList(
                                    extensionFunctionsTitle(),
                                    extensionFunctionsDetail.await(),
                                ),
                            extensionPropertiesSummary = extensionPropertiesSummary.await(),
                            extensionPropertiesDetails =
                                TitledList(
                                    extensionPropertiesTitle(),
                                    extensionPropertiesDetail.await(),
                                ),
                            inheritedFunctions = inheritedFunctions ?: emptyInheritedSymbolsList(),
                            inheritedConstants = inheritedConstants ?: emptyInheritedSymbolsList(),
                            inheritedProperties =
                                inheritedProperties ?: emptyInheritedSymbolsList(),
                        ),
                    ),
                metadataComponent = metadataComponent.await(),
                includedHeadTagPath = pathProvider.includedHeadTagsPath,
                referenceObject =
                    DefaultReferenceObject(
                        ReferenceObject.Params(
                            name = classlike.dri.classNames.orEmpty(),
                            path = classlike.dri.packageName.orEmpty(),
                            // Aggregate all functions and properties. The anchor is used to enable
                            // devsite search to link directly to the item.
                            properties =
                                buildList {
                                        addAll(declaredFunctions)
                                        addAll(declaredProperties)
                                        addAll(companionFunctions)
                                        addAll(companionProperties)
                                        addAll(extensionFunctions)
                                        addAll(extensionProperties)
                                    }
                                    .mapNotNull { it.dri.callable?.anchor() ?: it.name },
                            language = displayLanguage,
                        ),
                    ),
            ),
        )
    }

    /** This is intended to be used as (some thing) = emptyIfJava ?: actuallyComputeItIfKotlin() */
    private fun <T : SummaryItem> emptyIfJava() =
        if (displayLanguage == Language.JAVA) emptySummaryList<T>() else null

    private fun <T> emptyListIfJava() =
        if (displayLanguage == Language.JAVA) emptyList<T>() else null

    protected open suspend fun getClasslikeDescription(): ClasslikeDescription = coroutineScope {
        val signature = async { computeSignature(classlike, docsHolder.classGraph()) }
        val hierarchy = async { computeHierarchy() }
        val relatedSymbols = async { findRelatedSymbols() }
        DefaultClasslikeDescription(
            ClasslikeDescription.Params(
                header = header,
                primarySignature = signature.await(),
                hierarchy = hierarchy.await(),
                relatedSymbols = relatedSymbols.await(),
                descriptionDocs = javadocConverter.metadata(classlike),
            ),
        )
    }

    private fun nestedTypesToSummary(
        nestedClasslikes: List<DClasslike>,
        classGraph: ClassGraph
    ): ClasslikeSummaryList {
        val components =
            nestedClasslikes.map { nestedClasslike ->
                errorContextInjector(nestedClasslike) {
                    DefaultTableRowSummaryItem(
                        TableRowSummaryItem.Params(
                            title = null,
                            description =
                                DefaultClasslikeSummary(
                                    ClasslikeSummary.Params(
                                        signature = computeSignature(nestedClasslike, classGraph),
                                        description =
                                            javadocConverter.summaryDescription(nestedClasslike),
                                    ),
                                )
                                    as ClasslikeSummary,
                        ),
                    )
                }
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header =
                    DefaultTableTitle(
                        TableTitle.Params(
                            title = nestedTypesTitle(),
                            big = true,
                        ),
                    ),
                items = components,
            ),
        )
    }

    private fun functionsToSummary(
        name: String? = null,
        functions: List<DFunction>
    ): FunctionSummaryList {
        val components =
            functions.mapNotNull {
                val modifierHints =
                    ModifierHints(
                        displayLanguage = displayLanguage,
                        type = DFunction::class.java,
                        containingType = classlike::class.java,
                        isFromJava = classlike.isFromJava(),
                        isSummary = true,
                        injectStatic = it.isJavaStaticMethod(),
                        inCompanion = classlike.isCompanion(),
                    )
                errorContextInjector(it) { functionToSummaryConverter(it, modifierHints) }
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header =
                    name?.let {
                        DefaultTableTitle(
                            TableTitle.Params(
                                title = name,
                                big = true,
                            ),
                        )
                    },
                items = components,
            ),
        )
    }

    private fun constructorsToSummary(
        name: String,
        constructors: List<DFunction>
    ): ConstructorSummaryList {
        val components =
            constructors.mapNotNull {
                errorContextInjector(it) { constructorToSummaryConverter(it) }
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header =
                    DefaultTableTitle(
                        TableTitle.Params(
                            title = name,
                            big = true,
                        ),
                    ),
                items = components,
            ),
        )
    }

    private fun functionsToDetail(
        functions: List<DFunction>,
    ): List<SymbolDetail<FunctionSignature>> {
        return functions.mapNotNull {
            val modifierHints =
                ModifierHints(
                    displayLanguage = displayLanguage,
                    isSummary = false,
                    type = DFunction::class.java,
                    containingType = classlike::class.java,
                    isFromJava = classlike.isFromJava(),
                    injectStatic = it.isJavaStaticMethod(),
                    inCompanion = classlike.isCompanion(),
                )
            errorContextInjector(it) { functionToDetailConverter(it, modifierHints) }
        }
    }

    private fun constructorsToDetail(
        functions: List<DFunction>
    ): List<SymbolDetail<FunctionSignature>> {
        val modifierHints =
            ModifierHints(
                displayLanguage = displayLanguage,
                type = DFunction::class.java,
                containingType = classlike::class.java,
                isFromJava = classlike.isFromJava(),
                isSummary = false,
                isConstructor = true,
            )
        return functions.mapNotNull {
            errorContextInjector(it) { constructorToDetailConverter(it, modifierHints) }
        }
    }

    private fun enumValuesToSummary(
        title: String,
        enumVals: List<DEnumEntry>
    ): LinkDescriptionSummaryList {
        val components = enumVals.map { errorContextInjector(it) { enumConverter.summary(it) } }
        return DefaultSummaryList(
            SummaryList.Params(
                header =
                    DefaultTableTitle(
                        TableTitle.Params(
                            title = title,
                            big = true,
                        ),
                    ),
                items = components,
            ),
        )
    }

    private fun propertiesToSummary(
        name: String? = null,
        properties: List<DProperty>
    ): PropertySummaryList {
        val components =
            properties.mapNotNull {
                val modifierHints =
                    ModifierHints(
                        displayLanguage = displayLanguage,
                        type = DProperty::class.java,
                        containingType = classlike::class.java,
                        isFromJava = classlike.isFromJava(),
                        isSummary = true,
                        injectStatic = it.isStaticAnnotated(),
                    )
                errorContextInjector(it) { propertyToSummaryConverter(it, modifierHints) }
            }

        val title =
            name?.let {
                DefaultTableTitle(
                    TableTitle.Params(
                        title = it,
                        big = true,
                    ),
                )
            }

        return DefaultSummaryList(
            SummaryList.Params(
                header = title,
                items = components,
            ),
        )
    }

    private fun propertiesToDetail(
        properties: List<DProperty>,
    ): List<SymbolDetail<PropertySignature>> {
        return properties.mapNotNull {
            val modifierHints =
                ModifierHints(
                    displayLanguage = displayLanguage,
                    type = DProperty::class.java,
                    containingType = classlike::class.java,
                    isFromJava = classlike.isFromJava(),
                    isSummary = false,
                    injectStatic = it.isStaticAnnotated(),
                )
            errorContextInjector(it) { propertyToDetailConverter(it, modifierHints) }
        }
    }

    private fun enumValuesToDetail(
        dEnum: DEnum,
        enumValues: List<DEnumEntry>
    ): List<SymbolDetail<PropertySignature>> {
        val modifierHints =
            ModifierHints(
                displayLanguage,
                type = DEnumEntry::class.java,
                containingType = classlike::class.java,
                isFromJava = classlike.isFromJava(),
                isSummary = false,
            )
        return enumValues.map {
            errorContextInjector(it) { enumConverter.detail(dEnum, it, modifierHints) }
        }
    }

    /**
     * Creates a list of properties which are declared on the classlike.
     *
     * For Kotlin docs, this is the list of [initialProperties] and constant [companionProperties].
     *
     * For Java, the list is modified based on @Jvm annotations, and some properties are not
     * included because they are converted to getters and setters. Some [companionProperties] are
     * hoisted to the containing classlike (or, if the classlike is a companion, no properties are
     * declared on it as they are hoisted or converted to getters/setters).
     */
    private fun computeDeclaredProperties(
        initialProperties: List<DProperty>,
        companionProperties: List<DProperty>,
    ): List<DProperty> {
        if (displayLanguage == Language.KOTLIN) {
            return initialProperties + companionProperties.constants()
        }

        // Java documentation needs to respect @jvm* annotations
        val properties =
            initialProperties.filterOutJvmSynthetic().filter {
                it.isPropertyInJava() ||
                    !it.hasAnAccessor() // Sometimes isFromJava and thus isPropertyInJava are
                // incorrect.
            }

        // Some symbols are moved from the companion object type to the enclosing class in java
        // Objects that are not top-level
        return if (classlike.isCompanion()) {
            // Either the property is hoisted to the containing classlike, or it shows up as getters
            // and setters on the companion (or both), so a companion always has no properties.
            emptyList()
        } else if (classlike is DObject) {
            val (static, nonStatic) = properties.partition { it.objectPropertyHoistedInJava() }
            nonStatic +
                objectInstanceProperty +
                // Inject the @JvmStatic annotation to properties that need it
                static.map { it.addJvmStatic() }
        } else {
            // Classlikes that are not (top-level) objects
            properties +
                // It is technically incorrect to put @JvmStatic on a property, but we use this
                // to remember that we should later inject the `static` modifier to this
                companionProperties
                    .filter { it.objectPropertyHoistedInJava() }
                    .map { it.addJvmStatic() }
        }
    }

    /**
     * Creates a list of functions which are declared on the classlike.
     *
     * For Kotlin docs, this is just the list of [initialFunctions].
     *
     * For Java, the list is modified based on @Jvm annotations. Some [companionFunctions] are
     * hoisted to the containing classlike. The [initialFunctions] list should include generated
     * property accessors.
     */
    private fun computeDeclaredFunctions(
        initialFunctions: List<DFunction>,
        companionFunctions: List<DFunction>,
    ): List<DFunction> {
        if (displayLanguage == Language.KOTLIN) return initialFunctions

        // Static companion functions are hoisted to the containing class
        return (initialFunctions + companionFunctions.filter { it.isJavaStaticMethod() })
            // Java documentation needs to respect @jvm* annotations
            .filterOutJvmSynthetic()
            .map { it.withJvmName() }
            .map { it.convertReceiverForJava() }
    }

    private val objectInstanceProperty: DProperty by lazy {
        DProperty(
            dri =
                classlike.dri.copy(
                    callable = Callable(name = "INSTANCE", params = emptyList()),
                ),
            name = "INSTANCE",
            documentation = emptyMap(),
            expectPresentInSet = classlike.expectPresentInSet,
            sources = classlike.sources,
            visibility = classlike.visibility,
            type = GenericTypeConstructor(dri = classlike.dri, projections = emptyList()),
            receiver = null,
            setter = null,
            getter = null,
            modifier = emptyMap(),
            sourceSets = classlike.sourceSets,
            generics = emptyList(),
            isExpectActual = false,
            extra =
                PropertyContainer.withAll(
                    classlike.sourceSets.map {
                        mapOf(
                                it to setOf(ExtraModifiers.JavaOnlyModifiers.Static),
                            )
                            .toAdditionalModifiers()
                    } +
                        classlike.sourceSets.map {
                            Annotations(
                                mapOf(
                                    it to
                                        listOf(
                                            Annotations.Annotation(
                                                dri =
                                                    DRI(
                                                        packageName = "kotlin.jvm",
                                                        classNames = "JvmField",
                                                    ),
                                                params = emptyMap(),
                                            ),
                                        ),
                                ),
                            )
                        },
                ),
        )
    }

    private fun DClasslike.isCompanion() = this is DObject && docsHolder.isCompanion(this)

    // To allow consolidating identical ClasslikeSignatures generated from different sourceSets,
    // we want all identical ClasslikeSignatures to be ==. Because we can't rely on == for upstream
    // data classes like DClasslike, we need a workaround. We abstract out the inputs to this fun.
    private data class SourceSetDependentSignatureInputs(
        val modifiers: Modifiers,
        val annotations: List<Annotations.Annotation>,
        val typeAliasEquals: TypeProjectionComponent? = null,
    )

    private data class SourceSetIndependentSignatureInputs(
        val generics: List<DTypeParameter>,
        val name: String,
        val dri: DRI,
        val type: String,
        val isFromJava: Boolean,
    )

    private val SIGNATURE_INSTANCES =
        ConcurrentHashMap<
            Pair<SourceSetDependentSignatureInputs, SourceSetIndependentSignatureInputs>,
            ClasslikeSignature,
        >()

    protected open fun computeSignature(
        classlike: DClasslike,
        classGraph: ClassGraph,
        sourceSet: DokkaSourceSet = classlike.getExpectOrCommonSourceSet(),
        typealiasEquals: Bound? = null,
    ): ClasslikeSignature {
        val sourceSetDependentInput = computeSourceSetDependentSignatureInputs(classlike, sourceSet)
        val sourceSetIndepInput = computeSourceSetIndependentSignatureInputs(classlike)

        return SIGNATURE_INSTANCES.getOrPut(
            sourceSetDependentInput to sourceSetIndepInput,
        ) {
            // TODO(KMP ClassGraph b/253454963. Move these into sourceSetDependentInput.)
            val (extends, implements) =
                when (sourceSetIndepInput.type) {
                    "class",
                    "interface",
                    "enum",
                    "object" ->
                        (classGraph.getValue(sourceSetIndepInput.dri).directSuperClasses.map {
                            pathProvider.linkForReference(it.dri)
                        } to
                            classGraph.getValue(sourceSetIndepInput.dri).directInterfaces.map {
                                pathProvider.linkForReference(it.dri)  
                            })
                    else -> emptyList<Link>() to emptyList()
                }
            DefaultClasslikeSignature(
                ClasslikeSignature.Params(
                    displayLanguage = displayLanguage,
                    name =
                        pathProvider.linkForReference(
                            sourceSetIndepInput.dri,
                            sourceSetIndepInput.name
                        ),
                    type =
                        if (sourceSetDependentInput.typeAliasEquals != null) {
                            "typealias"
                        } else sourceSetIndepInput.type,
                    modifiers = sourceSetDependentInput.modifiers,
                    implements = implements,
                    extends = extends,
                    typeParameters =
                        sourceSetIndepInput.generics.map {
                            errorContextInjector(it) {
                                paramConverter.componentForTypeParameter(
                                    it,
                                    sourceSetIndepInput.isFromJava,
                                )
                            }
                        },
                    annotationComponents =
                        annotationConverter.annotationComponents(
                            annotations = sourceSetDependentInput.annotations,
                            nullability =
                                Nullability.DONT_CARE, // Classlike definitions aren't null
                        ),
                    typeAliasEquals = sourceSetDependentInput.typeAliasEquals,
                ),
            )
        }
    }

    private fun computeSourceSetDependentSignatureInputs(
        classlike: DClasslike,
        sourceSet: DokkaSourceSet,
    ): SourceSetDependentSignatureInputs {
        val typeAliasEquals =
            (classlike as? WithExtraProperties<*>)
                ?.extra
                ?.allOfType<ActualTypealias>()
                ?.strictSingleOrNull()
                ?.typeAlias
                ?.underlyingType
                ?.get(sourceSet)
        val modifiers =
            classlike.modifiers(sourceSet) +
                (typeAliasEquals?.let { listOf("actual") } ?: emptyList())
        return SourceSetDependentSignatureInputs(
            modifiers.modifiersFor(
                ModifierHints(
                    displayLanguage,
                    type = classlike::class.java,
                    containingType = null,
                    injectStatic = classlike is DObject && displayLanguage == Language.JAVA,
                    isFromJava = classlike.isFromJava(),
                    isSummary = false,
                ),
            ),
            classlike.annotations(sourceSet),
            typeAliasEquals?.let { paramConverter.componentForProjection(it, false, sourceSet) },
        )
    }

    private fun computeSourceSetIndependentSignatureInputs(
        classlike: DClasslike,
    ) =
        SourceSetIndependentSignatureInputs(
            dri = classlike.dri,
            name = classlike.name(),
            type = classlike.stringForType(displayLanguage),
            generics = classlike.generics(),
            isFromJava = classlike.isFromJava(),
        )

    /** Walks up this class' type hierarchy and returns the hierarchy component. */
    protected suspend fun computeHierarchy(): ClassHierarchy {
        if (classlike !is WithSupertypes) {
            return DefaultClassHierarchy(ClassHierarchy.Params(parents = emptyList()))
        }

        val parents = docsHolder.classGraph().getValue(classlike.dri).superClasses
        if (parents.isEmpty()) {
            // Don't show the hierarchy if this class only extends Any/Object
            return DefaultClassHierarchy(ClassHierarchy.Params(parents = emptyList()))
        }

        val classHierarchyRootDri =
            when (displayLanguage) {
                Language.JAVA -> DRI(packageName = "java.lang", classNames = "Object")
                Language.KOTLIN -> DRI(packageName = "kotlin", classNames = "Any")
            }
        val classHierarchyRootLink =
            pathProvider.linkForReference(classHierarchyRootDri, classHierarchyRootDri.fullName)
        val thisLink = pathProvider.linkForReference(classlike.dri, classlike.dri.fullName)

        val parentLinks =
            parents.map { classlike ->
                pathProvider.linkForReference(classlike.dri, classlike.dri.fullName)
            }
        val allLinks = listOf(classHierarchyRootLink) + parentLinks + listOf(thisLink)
        return DefaultClassHierarchy(ClassHierarchy.Params(allLinks))
    }

    /** Creates a list of InheritedSymbols from a list of DFunctions */
    private fun computeInheritedSymbols(
        symbolList: List<Documentable>,
    ): Triple<
        InheritedSymbolsList<FunctionSignature>?,
        InheritedSymbolsList<PropertySignature>?,
        InheritedSymbolsList<PropertySignature>?,
    > {
        val symbols =
            when (displayLanguage) {
                Language.JAVA -> symbolList.filterOutJvmSynthetic()
                Language.KOTLIN -> symbolList
            }

        val functions =
            symbols.filterIsInstance<DFunction>().sortedWith(functionSignatureComparator)
        val functionsRenamed =
            when (displayLanguage) {
                Language.JAVA -> functions.map { it.withJvmName() }
                Language.KOTLIN -> functions
            }
        val functionsSummary =
            functionsRenamed
                .takeIf { it.isNotEmpty() }
                ?.createInheritedCategory(title = inheritedMethodsTitle(displayLanguage)) {
                    functionsToSummary(functions = it)
                }

        val (consts, properties) =
            symbols
                .filterIsInstance<DProperty>()
                .sortedWith(simpleDocumentableComparator)
                .partition { it.isConstant() }

        val constsSummary =
            consts
                .takeIf { it.isNotEmpty() }
                ?.createInheritedCategory(title = inheritedConstantsTitle()) {
                    propertiesToSummary(properties = it)
                }

        val propertiesSummary =
            properties
                .takeIf { it.isNotEmpty() }
                ?.createInheritedCategory(title = inheritedPropertiesTitle(displayLanguage)) {
                    propertiesToSummary(properties = it)
                }

        return Triple(functionsSummary, constsSummary, propertiesSummary)
    }

    private fun <T, U : SymbolSignature> List<T>.createInheritedCategory(
        title: String,
        summaryGen: (List<T>) -> SummaryList<TypeSummaryItem<U>>,
    ): InheritedSymbolsList<U> where T : Documentable, T : WithExtraProperties<T> {
        fun createInheritedSymbolsList(
            parentDri: DRI,
            symbolList: List<T>
        ): Pair<Link, SummaryList<TypeSummaryItem<U>>> {
            val link = pathProvider.linkForReference(parentDri, parentDri.fullName)
            val summary = summaryGen(symbolList)
            return link to summary
        }

        // val category = groupBy { it.driInheritedFrom() ?: it.dri.parent }
        // TODO: this is sorting classes--what if you have the same-named class in two sourceSets?
        // Addressing this will likely require fixing b/247079868
        val category =
            groupBy { it.dri.parent }
                .toSortedMap(compareBy { it.classNames + " " + it.fullName })
                .entries
                .associate { (k, v) -> createInheritedSymbolsList(k, v) }

        return DefaultInheritedSymbols(
            InheritedSymbolsList.Params(
                header =
                    DefaultTableTitle(
                        TableTitle.Params(title, big = true),
                    ),
                inheritedSymbolSummaries = category,
            ),
        )
    }

    /**
     * WARNING: does not work properly The dri from which this documentable is inherited, or null.
     *
     * `extra[InheritedMember].inheritedFrom` does not actually contain where inherited members are
     * inherited from
     */
    /*
    private fun <T> T.driInheritedFrom(): DRI?
        where T : Documentable, T : WithExtraProperties<T> =
        extra[InheritedMember]?.inheritedFrom?.values?.toSet()?.singleOrNull() */

    /** Finds the direct and indirect subclasses for this classlike, returning their component. */
    // We know our subclasses will always be DClasslikes
    protected suspend fun findRelatedSymbols(): RelatedSymbols {
        val classNode = docsHolder.classGraph().getValue(classlike.dri)
        val directSubclasses = classNode.directSubClasses
        val indirectSubclasses = classNode.indirectSubClasses

        return DefaultRelatedSymbols(
            RelatedSymbols.Params(
                // TODO(KMP; b/254490320)
                directSubclasses = linksForClasslikes(directSubclasses),
                directSummary = javadocConverter.docsToSummaryDefault(directSubclasses),
                indirectSubclasses = linksForClasslikes(indirectSubclasses),
                indirectSummary = javadocConverter.docsToSummaryDefault(indirectSubclasses),
            ),
        )
    }

    /** Converts the classlikes to link components for use in the related symbols component. */
    private fun linksForClasslikes(docs: List<DClasslike>): List<Link> {
        return docs.map { pathProvider.linkForReference(it.dri) }
    }

    /** Returns lists of functions and properties directly owned by this class-like. */
    private suspend fun DClasslike.nonInheritedTypes(): Pair<List<DFunction>, List<DProperty>> {
        val allFunctions =
            if (displayLanguage == Language.KOTLIN) {
                this.functions
            } else this.functions + this.properties.gettersAndSetters()

        val supertypes = this.supertypesForDisplayLanguage()

        return Pair(
            allFunctions.nonInheritedTypes(supertypes, this),
            this.properties.nonInheritedTypes(supertypes, this),
        )
    }

    /**
     * Returns the list of declared symbols. That is, symbols directly owned by the provided
     * class-like and not found through the inheritance hierarchy.
     *
     * Class and package comparison isn't applicable for synthetic classes.
     */
    private inline fun <reified T : Documentable> List<T>.nonInheritedTypes(
        supertypes: Set<DRI>,
        forClass: DClasslike,
    ): List<T> {
        if (forClass.isSynthetic) {
            return this
        }

        return filter { symbol ->
                // Remove all symbols inherited from visible supertypes
                !symbol.isInherited(supertypes) &&
                    !symbol.dri.isFromBaseClass() &&
                    // If hidden parent symbols shouldn't be included, remove any symbols defined in
                    // classes marked as hidden
                    (docsHolder.includeHiddenParentSymbols || !hasBeenHidden(symbol.dri.ofClass()))
            }
            .map { symbol -> symbol.withDRIOfClass(forClass) }
    }

    /** Returns just the class part of a DRI. */
    private fun DRI.ofClass(): DRI = DRI(packageName, classNames)

    /**
     * If the DProperty or DFunction does not already have a DRI with the given class, makes a copy
     * of it with a new DRI. Defined over Documentables because both DProperty and Function have a
     * `copy` method defined because they are data classes, but there isn't a way to specify the
     * Documentable must be a data class.
     */
    private inline fun <reified T : Documentable> T.withDRIOfClass(forClass: DClasslike): T {
        return if (
            forClass.packageName() == this.dri.packageName && forClass.name() == this.dri.classNames
        ) {
            this
        } else {
            val dri =
                this.dri.copy(
                    packageName = forClass.packageName(),
                    classNames = forClass.name(),
                )
            when (this) {
                is DFunction -> this.copy(dri) as T
                is DProperty -> this.copy(dri) as T
                else -> throw RuntimeException()
            }
        }
    }

    /**
     * Gather superclasses and interfaces for this class, converting mapped types when applicable.
     */
    private suspend fun DClasslike.supertypesForDisplayLanguage(): Set<DRI> {
        val classNode = docsHolder.classGraph()[this.dri] ?: return emptySet()
        return (classNode.superClasses + classNode.interfaces)
            .map { it.dri.possiblyConvertMappedType(displayLanguage) }
            .toSet()
    }

    private suspend fun DClasslike.companionFunctionsAndProperties():
        Pair<List<DFunction>, List<DProperty>> {
        return this.companion()?.nonInheritedTypes() ?: return Pair(emptyList(), emptyList())
    }

    /**
     * Returns the list of inherited symbols, not from Any or Object If class is synthetic there
     * should be no inherited methods
     */
    private fun DClasslike.inheritedTypes(supertypes: Set<DRI>): List<Documentable> {
        if (this.isSynthetic) {
            return emptyList()
        }
        val allSymbols =
            when (displayLanguage) {
                Language.KOTLIN -> this.children
                Language.JAVA ->
                    (this.children + this.properties.gettersAndSetters()).filterNot {
                        it is DProperty && it.hasAnAccessor() && !it.isPropertyInJava()
                    }
            }
        return allSymbols.filter { it.isInherited(supertypes) && !it.dri.isFromBaseClass() }
    }

    private fun <T : Documentable> T.isInherited(supertypes: Set<DRI>): Boolean {
        // Convert mapped type for the function to make sure the DRI lines up with supertypes.
        val classDRI =
            DRI(dri.packageName, dri.classNames).possiblyConvertMappedType(displayLanguage)
        return supertypes.contains(classDRI)
    }

    private fun createDefaultConstructorFor(classlike: DClasslike) =
        DFunction(
            dri = classlike.dri.copy(callable = Callable(classlike.name!!, null, emptyList())),
            name = classlike.name!!,
            isConstructor = true,
            parameters = emptyList(),
            documentation = emptyMap(),
            expectPresentInSet = null,
            sources = emptyMap(),
            visibility = classlike.visibility,
            type = GenericTypeConstructor(dri = classlike.dri, projections = emptyList()),
            generics = emptyList(),
            receiver = null,
            modifier = classlike.visibility.keys.associateWith { KotlinModifier.Final },
            sourceSets = classlike.sourceSets,
            isExpectActual = false,
        )

    protected open fun <I : Documentable, O> errorContextInjector(
        documentable: I,
        sourceSet: DokkaSourceSet = documentable.getExpectOrCommonSourceSet(),
        toDo: (I) -> O,
    ): O {
        try {
            return toDo(documentable)
        } catch (e: Exception) {
            var message = "Error when handling ${documentable.className} ${documentable.name} "
            if (classlike != documentable) message += "in ${classlike.className} ${classlike.name}"
            if (documentable is WithSources) {
                message += " at " + documentable.getErrorLocation(sourceSet)
            }
            throw RuntimeException(message, e)
        }
    }
}

/**
 * Whether the [Documentable] as a child of a companion object would be documented on the containing
 * classlike of the companion for the given [displayLanguage].
 *
 * All companion properties and functions appear on the class page for Kotlin but not all for Java.
 */
internal fun Documentable.isHoistedFromCompanion(displayLanguage: Language): Boolean =
    when (displayLanguage) {
        Language.KOTLIN -> this is DProperty || this is DFunction
        Language.JAVA ->
            (this is DFunction && this.isJavaStaticMethod()) ||
                (this is DProperty && this.objectPropertyHoistedInJava())
    }

// an `actual` cannot narrow visibility, but can widen it TODO(b/262710702)
private fun isPublic(element: Documentable) =
    "public" in element.modifiers(element.getExpectOrCommonSourceSet())

private fun isProtected(element: Documentable) =
    "protected" in element.modifiers(element.getExpectOrCommonSourceSet())

/** Filter out constants, because those have a separate display sections from properties. */
private fun isPublicNonConst(prop: DProperty) = isPublic(prop) && !prop.isConstant()

private fun isProtectedNonConst(prop: DProperty) = isProtected(prop) && !prop.isConstant()

/**
 * Returns true if function is a suspend function itself, or takes a suspend function as a
 * parameter.
 */
private fun DFunction.isSuspendFunction() =
    type.isSuspend() || parameters.any { it.type.isSuspend() }

private fun List<DProperty>.constants() = filter { it.isConstant() }

internal fun nestedTypesTitle() = "Nested types"

internal fun publicConstructorsTitle() = "Public ${constructorsTitle()}"

internal fun protectedConstructorsTitle() = "Protected ${constructorsTitle()}"

internal fun constructorsTitle(): String = "constructors"

internal fun publicMethodsTitle(displayLanguage: Language) =
    "Public ${methodsTitle(displayLanguage)}"

internal fun inheritedMethodsTitle(displayLanguage: Language) =
    "Inherited ${methodsTitle(displayLanguage)}"

internal fun protectedMethodsTitle(displayLanguage: Language) =
    "Protected ${methodsTitle(displayLanguage)}"

internal fun methodsTitle(displayLanguage: Language): String =
    when (displayLanguage) {
        Language.JAVA -> "methods"
        Language.KOTLIN -> "functions"
    }

internal fun publicPropertiesTitle(displayLanguage: Language) =
    "Public ${propertiesTitle(displayLanguage)}"

internal fun inheritedPropertiesTitle(displayLanguage: Language) =
    "Inherited ${propertiesTitle(displayLanguage)}"

internal fun protectedPropertiesTitle(displayLanguage: Language) =
    "Protected ${propertiesTitle(displayLanguage)}"

internal fun propertiesTitle(displayLanguage: Language): String =
    when (displayLanguage) {
        Language.JAVA -> "fields"
        Language.KOTLIN -> "properties"
    }

internal fun constantsTitle() = "Constants"

internal fun inheritedConstantsTitle() = "Inherited ${constantsTitle()}"

internal fun enumValuesTitle() = "Enum Values"

// Extension functions and companions are a Kotlin-only feature and only show up in as-Kotlin
internal fun extensionFunctionsTitle() = "Extension functions"

internal fun extensionPropertiesTitle() = "Extension properties"

internal fun companionFunctionsTitle(): String = "companion ${methodsTitle(Language.KOTLIN)}"

internal fun companionPropertiesTitle(): String = "companion ${propertiesTitle(Language.KOTLIN)}"

internal fun publicCompanionFunctionsTitle(): String = "Public ${companionFunctionsTitle()}"

internal fun protectedCompanionFunctionsTitle(): String = "Protected ${companionFunctionsTitle()}"

internal fun publicCompanionPropertiesTitle(): String = "Public ${companionPropertiesTitle()}"

internal fun protectedCompanionPropertiesTitle(): String = "Protected ${companionPropertiesTitle()}"
