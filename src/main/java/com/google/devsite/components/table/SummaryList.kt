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

import com.google.devsite.components.ContextFreeComponent

/** Builds a table view. */
internal interface SummaryList<T : SummaryItem> : ContextFreeComponent, List<T> {
    val data: Params<T>

    /** @return true if there is summary content to render, false otherwise */
    fun hasContent(): Boolean

    data class Params<T>(
        val header: TableTitle? = null,
        var items: List<T>,
    )

    fun title(): String? = data.header?.data?.title

    override val size: Int
        get() = data.items.size

    override fun contains(element: T) = data.items.contains(element)

    override fun containsAll(elements: Collection<T>) = data.items.containsAll(elements)

    override fun get(index: Int) = data.items.get(index)

    override fun indexOf(element: T) = data.items.indexOf(element)

    override fun isEmpty() = data.items.isEmpty()

    override fun iterator() = data.items.iterator()

    override fun lastIndexOf(element: T) = data.items.lastIndexOf(element)

    override fun listIterator() = data.items.listIterator()

    override fun listIterator(index: Int) = data.items.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = data.items.subList(fromIndex, toIndex)

    /** WARNING: these functions have side effects */
    operator fun plus(other: List<T>): SummaryList<T> {
        data.items = data.items + other
        return this
    }

    operator fun minus(other: Collection<T>): SummaryList<T> {
        data.items = data.items - other.toSet()
        return this
    }
}
