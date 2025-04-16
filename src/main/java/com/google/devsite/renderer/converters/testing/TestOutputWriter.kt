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

package com.google.devsite.testing

import java.util.Collections
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

class TestOutputWriterPlugin(failOnOverwrite: Boolean = false) : DokkaPlugin() {
    val writer = TestOutputWriter(failOnOverwrite)

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement

    val testWriter by extending {
        (dokkaBase.outputWriter with writer override dokkaBase.fileWriter)
    }
}

class TestOutputWriter(private val failOnOverwrite: Boolean = true) : OutputWriter {
    val contents: Map<String, String>
        get() = _contents

    private val _contents = Collections.synchronizedMap(mutableMapOf<String, String>())

    override suspend fun write(path: String, text: String, ext: String) {
        val fullPath = "$path$ext"
        _contents.putIfAbsent(fullPath, text)?.also {
            if (failOnOverwrite) throw AssertionError("File $fullPath is being overwritten.")
        }
    }

    override suspend fun writeResources(pathFrom: String, pathTo: String) =
        write(pathTo, "*** content of $pathFrom ***", "")
}
