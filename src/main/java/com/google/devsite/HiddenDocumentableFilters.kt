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

package com.google.devsite

import com.google.devsite.renderer.converters.allAnnotations
import com.google.devsite.renderer.converters.asString
import com.google.devsite.renderer.converters.deprecatedDri
import com.google.devsite.renderer.converters.explodedChildren
import com.google.devsite.renderer.converters.fullName
import com.google.devsite.renderer.converters.getExpectOrCommonSourceSet
import com.google.devsite.renderer.converters.isFromJava
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Visibility
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

/**
 * These filters remove items from the docs when they:
 * - have @hide in a comment
 * - have @removed in a comment
 * - are deprecated with [DeprecationLevel.HIDDEN]
 * - are annotated with an annotation from [DevsiteConfiguration.hidingAnnotations]
 *
 * There are two filters, one runs before the dokka merge step and one runs after. The pre-merge
 * filter removes hidden documentables and maintains a set of names of hidden packages. The
 * post-merge filter removes packages with their names in that set.
 *
 * This is because Java and Kotlin files are split into different DPackages until the merge step.
 * Packages use @RestrictTo or @hide in package-info.java file if everything in the package should
 * be excluded from the docs. The pre-merge filter only hides the Java files in a package, as the
 * Kotlin files are in a different DPackage at that point. The post-merge filter then hides the
 * Kotlin sources.
 *
 * Previously the pre-merge filter didn't remove hidden packages, leaving it until the post-merge
 * filter when the Java and Kotlin sources were in one package. However, a package which has
 * package-info.java as its only Java source may be filtered out by the empty packages filter before
 * the merge step (but after this filter), so it would never be merged with the Kotlin sources, and
 * the Kotlin sources would not be hidden by the post-merge filter.
 *
 * The post-merge filter also hides all subpackages of hidden packages.
 */

/**
 * Pre-merge transformer: filter hidden documentables, adding the names of all hidden packages to
 * [hiddenPackages]
 */
class PreMergeHiddenDocumentableFilter(
    dokkaContext: DokkaContext,
    private val hidingAnnotations: List<String>,
) : SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        // Upstream bug 2603 means private-backing-public-getter shows up as a public field.
        // This is a real problem if the public getter is `@hide`, because it means something is
        // newly exposed that shouldn't be. Filter for that case specifically.
        if (d is DProperty && d.isFromJava() && d.getter?.let { shouldBeSuppressed(it) } == true) {
            addToHiddenSet(d)
            return true
        }
        if (!d.isHidden(hidingAnnotations)) return false
        if (d is DPackage) {
            d.dri.packageName?.let { hiddenPackages.add(it) }
        }
        addToHiddenSet(d)
        return true
    }
}

/** Post-merge transformer: filter packages based on [packageShouldBeHidden] */
class PostMergePackageDocumentableFilter : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule {
        val filteredPackages =
            original.packages.filter {
                val hide = packageShouldBeHidden(it.packageName)
                if (hide) addToHiddenSet(it)
                !hide
            }
        return original.copy(packages = filteredPackages)
    }
}

class PreMergePrivateAnnotationRecorder : PreMergeDocumentableTransformer {
    /**
     * Does not modify the modules, but adds all annotations with visibilities not in the configured
     * documented visibilities to the hidden set.
     */
    override fun invoke(modules: List<DModule>): List<DModule> {
        modules.forEach { module ->
            module.packages.forEach { dPackage -> checkAllClasslikes(dPackage.classlikes) }
        }
        return modules
    }

    private fun checkAllClasslikes(classlikes: List<DClasslike>) {
        classlikes.forEach { classlike ->
            if (classlike is DAnnotation) {
                val sourceSet = classlike.getExpectOrCommonSourceSet()
                if (
                    classlike.visibility[sourceSet]?.isDocumented(
                        sourceSet.documentedVisibilities
                    ) == false
                ) {
                    addToHiddenSet(classlike)
                }
            }
            checkAllClasslikes(classlike.classlikes)
        }
    }
}

/**
 * Returns whether the [Visibility] corresponds to one of the documented visibilities in the set of
 * [DokkaConfiguration.Visibility]s.
 */
private fun Visibility.isDocumented(
    documentedVisibilities: Set<DokkaConfiguration.Visibility>,
): Boolean =
    // Java package visibility has an empty string name as a [Visibility] but not as a
    // [DokkaConfiguration.Visibility]. All other visibilities match between the definitions.
    DokkaConfiguration.Visibility.fromString(name.ifEmpty { "package" }) in documentedVisibilities

private fun Documentable.isHiddenWithAnnotation(hidingAnnotations: List<String>): Boolean =
    allAnnotations().any {
        hidingAnnotations.contains(it.dri.fullName) ||
            (it.dri == deprecatedDri && "DeprecationLevel.HIDDEN" in it.params["level"].asString())
    }

private fun Documentable.isHidden(hidingAnnotations: List<String>): Boolean =
    isHiddenWithAnnotation(hidingAnnotations) ||
        this.hasHideJavadocTag() ||
        this.hasRemovedJavadocTag() ||
        // Mirror Metalava's behavior for properties annotated with `@get:<hiding annotation>`
        (this as? DProperty)?.getter?.isHiddenWithAnnotation(hidingAnnotations) == true

private fun Documentable.hasHideJavadocTag(): Boolean =
    this.documentation.any { (_, docs) ->
        docs.dfs { it is CustomTagWrapper && it.name.trim() == "hide" } != null
    }

private fun Documentable.hasRemovedJavadocTag(): Boolean =
    this.documentation.any { (_, docs) ->
        docs.dfs { it is CustomTagWrapper && it.name.trim() == "removed" } != null
    }

fun hasBeenHidden(dri: DRI): Boolean {
    return hiddenDocumentables.contains(dri)
}

private fun addToHiddenSet(d: Documentable) {
    (d.explodedChildren + d).forEach { hiddenDocumentables.add(it.dri) }
}

/**
 * A package should be hidden if its name is in [hiddenPackages] or if it is a subpackage of a
 * package in [hiddenPackages].
 */
private fun packageShouldBeHidden(name: String) =
    // The "." in the startsWith condition is important, without it `packageAbc` would be a
    // subpackage of `packageA`
    hiddenPackages.any { it == name || name.startsWith("$it.") }

private val hiddenDocumentables = mutableSetOf<DRI>()
private val hiddenPackages = mutableSetOf<String>()
