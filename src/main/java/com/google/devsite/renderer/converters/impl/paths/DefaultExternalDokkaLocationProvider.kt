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

package com.google.devsite.renderer.impl.paths

import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet

class DefaultExternalDokkaLocationProvider(
    private val dokkaLocationProvider: DokkaLocationProvider,
) : ExternalDokkaLocationProvider {
    private val memoizer = ConcurrentHashMap<DRI, String>()

    /** ConcurrentHashMap cannot have nullable type parameters for some reason */
    private fun String.nullifier(): String? = if (this == "null") null else this

    @JvmName("is private") private fun String?.deNullifier(): String = this ?: "null"

    override fun resolve(dri: DRI): String? =
        memoizer
            .getOrPut(dri) {
                dokkaLocationProvider.resolve(dri, emptySet<DisplaySourceSet>()).deNullifier()
            }
            .nullifier()
}
