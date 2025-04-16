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

package com.google.devsite.renderer.impl

import com.google.devsite.hasBeenHidden
import com.google.devsite.renderer.converters.getExpectOrCommonSourceSet
import com.google.devsite.renderer.converters.gettersAndSetters
import java.util.TreeSet
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.JavaClassKindTypes
import org.jetbrains.dokka.model.KotlinClassKindTypes
import org.jetbrains.dokka.model.WithSupertypes

internal typealias ClassGraph = Map<DRI, ClassNode>

internal typealias DocumentablesGraph = Map<DRI, Documentable>

/**
 * Generate of graph of type dependencies.
 *
 * This returns the type inheritance of the given [classlikes] as a graph. That is, each DRI is
 * associated with itself and its complete list of subclasses and parents.
 */
internal fun computeClassGraph(
    classlikes: List<DClasslike>,
    externalDocumentableProvider: ExternalDocumentableProvider? = null,
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>? = null,
): ClassGraph {
    fun MutableMap<DRI, DClasslike?>.getOrExternal(key: DRI) =
        getOrPut(key) {
            sourceSets?.firstNotNullOfOrNull { sourceSet ->
                externalDocumentableProvider?.getClasslike(key, sourceSet)
            }
        }
    val drisToClasslikes = classlikes.associateBy<DClasslike?, DRI> { it!!.dri }.toMutableMap()
    val classGraph: Map<DRI, MutableClassNode> =
        classlikes.associate { classlike -> classlike.dri to MutableClassNode(classlike) }

    for (classlike in classlikes) {
        recursivelyUpdateClasslikeSupertypesTree(
            classlike,
            classGraph,
            drisToClasslikes::getOrExternal,
        )
    }

    // TODO(b/168956053): remove drisToClasslikes.getValue(it) once dokka has cheap hashCode impl
    return classGraph.mapValues { (_, level) ->
        ClassNode(
            self = level.self,
            allSubClasses = level.allSubClasses.mapNotNull { drisToClasslikes.getOrExternal(it) },
            directSubClasses =
                level.directSubClasses.mapNotNull { drisToClasslikes.getOrExternal(it) },
            indirectSubClasses =
                level.indirectSubClasses.mapNotNull { drisToClasslikes.getOrExternal(it) },
            directSuperClasses =
                level.directSuperClasses.mapNotNull { drisToClasslikes.getOrExternal(it) },
            superClasses = level.superClasses.mapNotNull { drisToClasslikes.getOrExternal(it) },
            interfaces = level.interfaces.mapNotNull { drisToClasslikes.getOrExternal(it) },
            directInterfaces =
                level.directInterfaces.mapNotNull { drisToClasslikes.getOrExternal(it) },
        )
    }
}

/** Generates a map that allows looking up each Documentable by its DRI */
internal fun computeDocumentablesGraph(classGraph: ClassGraph): DocumentablesGraph {
    // helper function for adding a Documentable to a graph
    fun addToDocumentablesGraph(graph: MutableMap<DRI, Documentable>, documentable: Documentable) {
        if (!graph.containsKey(documentable.dri)) {
            graph[documentable.dri] = documentable
            // Include generated property accessors
            val allChildren =
                documentable.children +
                    documentable.children.filterIsInstance<DProperty>().gettersAndSetters()
            for (child in allChildren) {
                addToDocumentablesGraph(graph, child)
            }
        }
    }

    // add each class to the graph, and recurse
    val result = mutableMapOf<DRI, Documentable>()
    for (documentable in classGraph.values) {
        addToDocumentablesGraph(result, documentable.self)
    }
    return result
}

/**
 * Updates the [classGraph] by traversing the supertype tree using [driToClasslike]. [initial] will
 * not change, so it can be added to every parent's subclasses. The recursion occurs on [current].
 *
 * We must recursively traverse the hierarchy graph bottom up because Dokka only provides direct
 * parents as a DRI. We're assuming this will be performant because the JVM doesn't support multiple
 * inheritance, therefore yielding objects that tend to fan out top down, conversely fanning in
 * bottom up (what this method does).
 *
 * @param current the current classlike who's supertypes we will be traversing
 * @param classGraph the mutable type relation graph to be updated. [highestVisibleSubtype] will be
 *   added to the set of direct subclasses for each of [current]'s supertypes. Similarly, [initial]
 *   will be added to the set of all subclasses for each of [current]'s supertypes. If
 *   [highestVisibleSubtype] and [initial] aren't the same (i.e. two edges away from each other: A
 *   -> B -> C), we add [initial] to the set of indirect subclasses for each of [current]'s
 *   supertypes. Lastly, we add the type hierarchy path of [current] to the parents of [initial],
 *   ordered top-down.
 * @param driToClasslike reverse lookup map to get supertypes from DRIs
 * @param initial constant classlike, storing the starting [current]. This classlike should be one
 *   that appears in the docs (has an entry in [classGraph]).
 * @param highestVisibleSubtype the highest-up classlike in the inheritance chain from [current] to
 *   [initial] which isn't hidden from the docs
 */
private fun recursivelyUpdateClasslikeSupertypesTree(
    current: DClasslike,
    classGraph: Map<DRI, MutableClassNode>,
    driToClasslike: (DRI) -> DClasslike?,
    initial: DClasslike = current,
    highestVisibleSubtype: DClasslike = current,
) {
    if (current !is WithSupertypes || current.supertypes.isEmpty()) return

    // TODO(KMP): this currently only constructs the `common`/`expect` tree. b/253454963
    val supertypes = current.supertypes[current.getExpectOrCommonSourceSet()]!!
    for ((type, kind) in supertypes) {
        classGraph[type.dri]?.let { (_, all, direct, indirect) ->
            all.add(initial.dri)
            direct.add(highestVisibleSubtype.dri)
            if (highestVisibleSubtype !== initial) indirect.add(initial.dri)
        }

        // If a classlike cannot be found in this package, the map will fall back to trying to look
        // it up using the externalDocumentablesProvider, and if we can't find it there either it
        // will be null.
        val supertype = driToClasslike(type.dri)
        if (supertype != null) {
            // Hidden classes should not be included in the class graph.
            // Only public and protected classes should be included in the class graph.
            val visibility = supertype.visibility[supertype.getExpectOrCommonSourceSet()]?.name
            val hidden =
                hasBeenHidden(type.dri) || (visibility != "public" && visibility != "protected")

            val newHighestVisible = if (hidden) highestVisibleSubtype else supertype
            recursivelyUpdateClasslikeSupertypesTree(
                supertype,
                classGraph,
                driToClasslike,
                initial,
                newHighestVisible,
            )

            // Only add this supertype to leaf's node if this is visible in docs.
            if (hidden) continue

            if (kind == JavaClassKindTypes.CLASS || kind == KotlinClassKindTypes.CLASS) {
                val leafValue = classGraph.getValue(initial.dri)
                leafValue.superClasses.add(supertype.dri)
                if (initial == highestVisibleSubtype) {
                    leafValue.directSuperClasses.add(supertype.dri)
                }
            }

            if (kind == JavaClassKindTypes.INTERFACE || kind == KotlinClassKindTypes.INTERFACE) {
                val leafValue = classGraph.getValue(initial.dri)
                leafValue.interfaces.add(supertype.dri)
                if (initial == highestVisibleSubtype) {
                    leafValue.directInterfaces.add(supertype.dri)
                }
            }
        }
    }
}

internal data class ClassNode(
    val self: DClasslike,
    val allSubClasses: List<DClasslike>,
    val directSubClasses: List<DClasslike>,
    val indirectSubClasses: List<DClasslike>,
    val directSuperClasses: List<DClasslike>,
    val superClasses: List<DClasslike>,
    val interfaces: List<DClasslike>,
    val directInterfaces: List<DClasslike>,
)

// TODO(b/168956053): Use DClasslike directly once dokka has cheap hashCode impl
private data class MutableClassNode(
    val self: DClasslike,
    val allSubClasses: MutableSet<DRI> = TreeSet(classComparator),
    val directSubClasses: MutableSet<DRI> = TreeSet(classComparator),
    val indirectSubClasses: MutableSet<DRI> = TreeSet(classComparator),
    val directSuperClasses: MutableSet<DRI> = LinkedHashSet(),
    val superClasses: MutableSet<DRI> = LinkedHashSet(),
    val interfaces: MutableSet<DRI> = LinkedHashSet(),
    val directInterfaces: MutableSet<DRI> = LinkedHashSet(),
) {
    private companion object {
        /**
         * Sort by the class name, but also compare by the package name since this determines
         * equality.
         */
        val classComparator = compareBy<DRI> { it.classNames + it.packageName }
    }
}
