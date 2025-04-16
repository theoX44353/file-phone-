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

import com.google.devsite.components.DescriptionComponent
import com.google.devsite.components.Link
import com.google.devsite.components.symbols.ClasslikeSummary
import com.google.devsite.components.symbols.FunctionSignature
import com.google.devsite.components.symbols.ParameterComponent
import com.google.devsite.components.symbols.PropertySignature
import com.google.devsite.components.symbols.SymbolSummary
import com.google.devsite.components.symbols.TypeSummary
import com.google.devsite.components.table.KmpTableRowSummaryItem
import com.google.devsite.components.table.SummaryList
import com.google.devsite.components.table.TableRowSummaryItem
import java.util.Locale
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.TagWrapper

/** Enables calling `!nullableBool ?: false` rather than a built-in less readable alternative. */
internal operator fun Boolean?.not() = this?.let { !it }

internal fun String.startsWithAnyOf(prefixes: List<String>) = prefixes.any { this.startsWith(it) }

internal fun String.capitalize() =
    this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

/** The same as joinToString but doesn't print prefix or postfix if the list is empty. */
internal fun <T> Collection<T>.joinMaybePrefix(
    prefix: String = "",
    postfix: String = "",
    separator: CharSequence = ", ",
    transform: ((T) -> CharSequence)? = null,
) =
    if (this.isEmpty()) {
        ""
    } else {
        joinToString(
            prefix = prefix,
            postfix = postfix,
            separator = separator,
            transform = transform
        )
    }

/** Performs an outer/tensor product. tensorOf((a,b), (c,d)) is ((a,c), (a,d), (b,c), (b,d)) */
fun <T> tensorOf(vararg lists: Iterable<T>): List<List<T>> =
    lists.fold(listOf(listOf())) { accumulated, nextDimension ->
        accumulated.flatMap { aSlice -> nextDimension.map { element -> aSlice + element } }
    }

/** Version of tensorOf that uses arrays. Because Kotlin's slices and list/array dance are silly. */
inline fun <reified T> tensorOf(vararg arrays: Array<T>): Array<Array<T>> =
    tensorOf(*(arrays.map { it.asIterable() }.toTypedArray()))
        .map { it.toTypedArray() }
        .toTypedArray()

/** Like singleOrNull, but requires that only one element be present if any. */
internal fun <T> List<T>.strictSingleOrNull() = if (isEmpty()) null else single()

internal fun <T> Collection<T>.containsAny(others: Collection<T>) =
    this.toSet().intersect(others.toSet()).isNotEmpty()

internal typealias TypeSummaryItem<T> = TableRowSummaryItem<TypeSummary, SymbolSummary<T>>

internal typealias KmpTypeSummaryItem<T> = KmpTableRowSummaryItem<TypeSummary, SymbolSummary<T>>

internal typealias PropertySummaryList = SummaryList<TypeSummaryItem<PropertySignature>>

internal typealias FunctionSummaryList = SummaryList<TypeSummaryItem<FunctionSignature>>

internal typealias ConstructorSummaryList =
    SummaryList<TableRowSummaryItem<Nothing?, SymbolSummary<FunctionSignature>>>

internal typealias ClasslikeSummaryList =
    SummaryList<TableRowSummaryItem<Nothing?, ClasslikeSummary>>

internal typealias WithDescriptionList<T> =
    SummaryList<TableRowSummaryItem<T, DescriptionComponent>>

internal typealias LinkDescriptionSummaryList = WithDescriptionList<Link>

internal typealias DocsSummaryList = WithDescriptionList<ParameterComponent>

internal val Documentable.className
    get() = (this::class).simpleName
internal val DocTag.className
    get() = (this::class).simpleName
internal val TagWrapper.className
    get() = (this::class).simpleName
