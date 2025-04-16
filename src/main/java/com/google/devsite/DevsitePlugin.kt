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

package com.google.devsite

import com.google.devsite.renderer.DocumentablesWrapper
import com.google.devsite.renderer.MultiLanguageRenderer
import com.google.devsite.transformers.DocTagsForCheckedExceptionsTransformer
import com.google.devsite.transformers.PropagatedAnnotationsTransformer
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.querySingle

class DevsitePlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }
    internal val analysisPlugin by lazy { plugin<KotlinAnalysisPlugin>() }

    /**
     * "All of Dokka's plugin API is in preview and it can be changed in a backwards-incompatible
     * manner with a best-effort migration. By opting in, you (we) acknowledge the risks of relying
     * on preview API."
     */
    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement

    val translator by extending {
        CoreExtensions.documentableToPageTranslator providing
            {
                DocumentablesWrapper()
            } override
            dokkaBase.documentableToPageTranslator
    }

    val renderer by extending {
        CoreExtensions.renderer providing
            {
                MultiLanguageRenderer(
                    it,
                    dokkaBase.querySingle { outputWriter },
                    getDevsiteConfiguration(it),
                    analysisPlugin,
                )
            } override
            dokkaBase.htmlRenderer
    }

    val docTagsForCheckedExceptions by extending {
        CoreExtensions.documentableTransformer with DocTagsForCheckedExceptionsTransformer()
    }

    val propagateAnnotations by extending {
        CoreExtensions.documentableTransformer providing
            {
                PropagatedAnnotationsTransformer(
                    getDevsiteConfiguration(it).propagatingAnnotations,
                )
            }
    }

    val privateAnnotationFilter by extending {
        dokkaBase.preMergeDocumentableTransformer with
            PreMergePrivateAnnotationRecorder() order
            {
                before(dokkaBase.documentableVisibilityFilter)
            }
    }

    val preMergeHiddenFilter by extending {
        dokkaBase.preMergeDocumentableTransformer providing
            {
                PreMergeHiddenDocumentableFilter(it, getDevsiteConfiguration(it).hidingAnnotations)
            } order
            {
                before(dokkaBase.emptyPackagesFilter)
            }
    }

    val hiddenPackageFilter by extending {
        CoreExtensions.documentableTransformer with PostMergePackageDocumentableFilter()
    }
}

internal fun getDevsiteConfiguration(dokkaContext: DokkaContext): DevsiteConfiguration {
    return checkNotNull(configuration<DevsitePlugin, DevsiteConfiguration>(dokkaContext)) {
        "Missing Dackka plugin configuration. See go/dackka#generating-docs for more detail."
    }
}
