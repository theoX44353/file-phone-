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

package com.google.devsite.components.table

import com.google.devsite.TypeSummaryItem
import com.google.devsite.components.ContextFreeComponent
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.SymbolSignature

/** Represents the inherited symbols in an expandable summary. */
internal interface InheritedSymbolsList<T : SymbolSignature> :
    ContextFreeComponent, List<TypeSummaryItem<T>> {

    val data: Params<T>

    data class Params<T : SymbolSignature>(
        val header: TableTitle,
        val inheritedSymbolSummaries: Map<Link, SummaryList<TypeSummaryItem<T>>>,
    )

    fun hasContent() = data.inheritedSymbolSummaries.isNotEmpty()

    val items
        get() = data.inheritedSymbolSummaries.flatMap { it.value }

    override val size
        get() = items.size

    override fun contains(element: TypeSummaryItem<T>) = items.contains(element)

    override fun containsAll(elements: Collection<TypeSummaryItem<T>>) = items.containsAll(elements)

    override fun get(index: Int) = items.get(index)

    override fun indexOf(element: TypeSummaryItem<T>) = items.indexOf(element)

    override fun isEmpty() = items.isEmpty()

    override fun iterator() = items.iterator()

    override fun lastIndexOf(element: TypeSummaryItem<T>) = items.lastIndexOf(element)

    override fun listIterator() = items.listIterator()

    override fun listIterator(index: Int) = items.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = items.subList(fromIndex, toIndex)
    }
