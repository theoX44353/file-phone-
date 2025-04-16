
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

import com.google.common.truth.Truth.assertWithMessage
import com.google.devsite.DevsiteConfiguration
import com.google.devsite.defaultValidNullabilityAnnotations
import com.google.devsite.renderer.converters.isRunningInDackkasTests
import java.io.File
import java.net.URL
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.toCompactJsonString
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.Before
import testApi.testRunner.TestDokkaConfigurationBuilder

/**
 * Full integration tests of source to html generation.
 *
 * Html output results can be found in testData/
 */
abstract class IntegrationTestBase :
    BaseAbstractTest(TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))) {
    @Before
    fun setUp() {
        isRunningInDackkasTests = true
    }

    fun TestDokkaConfigurationBuilder.singlePlatformSourceSets(
        sources: List<File>,
        samplesLocations: List<String>,
        includeFiles: List<String> = emptyList(),
        externalLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    ) = sourceSets {
        sourceSet {
            sourceRoots = sources.map { it.absolutePath }
            // TODO: find a workaround to using a fixed classpath file b/243842129
            classpath = classpathFromFile("testData/classpath.txt")
            externalDocumentationLinks = externalLinks
            samples = samplesLocations
            includes = includeFiles
            documentedVisibilities =
                setOf(
                    DokkaConfiguration.Visibility.PUBLIC,
                    DokkaConfiguration.Visibility.PROTECTED,
                )
        }
    }

    open fun TestDokkaConfigurationBuilder.makeSourcesets(
        sources: List<File>,
        samplesLocations: List<String>,
        includeFiles: List<String> = emptyList(),
        externalLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    ) = singlePlatformSourceSets(sources, samplesLocations, includeFiles, externalLinks)

    /** For when a test uses source outside of `./testData/` */
    open fun makeExternalConfiguration(
        sources: List<File>,
        samplesLocations: List<String>,
        includeFiles: List<String> = emptyList(),
        docRootPath: String,
        projectPath: String,
        javaDocsPath: String?,
        kotlinDocsPath: String?,
        includedHeadTagsPathJava: String? = "_shared/_reference-head-tags.html",
        includedHeadTagsPathKotlin: String? = "_shared/_reference-head-tags.html",
        useAndroidxBaseSourceLink: Boolean = false,
        versionMetadataFilesnames: List<String>? = null,
        hidingAnnotations: List<String> = listOf("androidx.annotation.RestrictTo"),
        includeHiddenParentSymbols: Boolean = false,
        validNullabilityAnnotations: List<String> = defaultValidNullabilityAnnotations,
    ): DokkaConfigurationImpl {
        sources.forEach { check(it.isDirectory) { "$it does not exist or is not a directory" } }
        val externalLinks =
            mapOf(
                    "coroutines" to "https://kotlinlang.org/api/kotlinx.coroutines",
                    "android" to "https://developer.android.com/reference",
                    "guava" to "https://guava.dev/releases/18.0/api/docs/package-list",
                    "kotlin" to "https://kotlinlang.org/api/latest/jvm/stdlib/",
                )
                .map {
                    ExternalDocumentationLink(
                        url = URL(it.value),
                        // TODO: improve package-list updateability b/243840381
                        packageListUrl =
                            File("testData")
                                .toPath()
                                .resolve("package-lists/${it.key}/package-list")
                                .toUri()
                                .toURL(),
                    )
                }

        val baseSourceLink =
            if (useAndroidxBaseSourceLink) {
                "https://cs.android.com/search?q=file:%s+class:%s" +
                    "&ss=androidx/platform/frameworks/support"
            } else {
                null
            }

        return dokkaConfiguration {
            makeSourcesets(sources, samplesLocations, includeFiles, externalLinks)
            offlineMode = true
            pluginsConfigurations =
                mutableListOf(
                    PluginConfigurationImpl(
                        fqPluginName = "com.google.devsite.DevsitePlugin",
                        serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                        values =
                            DevsiteConfiguration(
                                    docRootPath = docRootPath,
                                    projectPath = projectPath,
                                    excludedPackages = null,
                                    excludedPackagesForJava = null,
                                    excludedPackagesForKotlin = null,
                                    libraryMetadataFilename = null,
                                    versionMetadataFilenames = versionMetadataFilesnames,
                                    javaDocsPath = javaDocsPath,
                                    kotlinDocsPath = kotlinDocsPath,
                                    includedHeadTagsPathJava = includedHeadTagsPathJava,
                                    includedHeadTagsPathKotlin = includedHeadTagsPathKotlin,
                                    packagePrefixToRemoveInToc = null,
                                    baseSourceLink = baseSourceLink,
                                    baseFunctionSourceLink = null,
                                    basePropertySourceLink = null,
                                    // These lists are based on the AndroidX excluded annotations
                                    annotationsNotToDisplay =
                                        listOf(
                                            "androidx.compose.runtime.Stable",
                                            "androidx.compose.runtime.Immutable",
                                            "androidx.compose.runtime.ReadOnlyComposable",
                                            "androidx.annotation.OptIn",
                                            "kotlin.OptIn",
                                            "androidx.annotation.CheckResult",
                                            "kotlin.ParameterName",
                                            "kotlin.js.JsName",
                                            "java.lang.Override",
                                        ),
                                    annotationsNotToDisplayJava = null,
                                    annotationsNotToDisplayKotlin =
                                        listOf(
                                            "kotlin.ExtensionFunctionType",
                                        ),
                                    hidingAnnotations = hidingAnnotations,
                                    includeHiddenParentSymbols = includeHiddenParentSymbols,
                                    validNullabilityAnnotations = validNullabilityAnnotations,
                                )
                                .toCompactJsonString(),
                    ),
                )
        }
    }

    /** For when a test uses source in `./testData/` */
    private fun makeInternalConfiguration(
        samplesBaseDir: String,
        sourceDir: String,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
        docRootPath: String,
        projectPath: String,
        javaDocsDirectory: String?,
        kotlinDocsDirectory: String?,
        includedHeadTagsPathJava: String?,
        includedHeadTagsPathKotlin: String?,
        useAndroidxBaseSourceLink: Boolean,
        hidingAnnotations: List<String>,
        includeHiddenParentSymbols: Boolean,
    ): DokkaConfigurationImpl {
        val sources = File(sourceDir).absoluteFile
        return makeExternalConfiguration(
            listOf(sources),
            sampleLocations.map { "$samplesBaseDir/$it" },
            includeFiles.map { File(sourceDir, it).absolutePath },
            docRootPath,
            projectPath,
            javaDocsDirectory,
            kotlinDocsDirectory,
            includedHeadTagsPathJava,
            includedHeadTagsPathKotlin,
            useAndroidxBaseSourceLink,
            hidingAnnotations = hidingAnnotations,
            includeHiddenParentSymbols = includeHiddenParentSymbols,
        )
    }

    /** Executes dackka on source from an androidx checkout on the same machine. No validation. */
    fun executionTest(
        testName: String,
        paths: List<String>,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
    ) {
        val configuration =
            makeExternalConfiguration(
                paths.map { File(it).absoluteFile },
                sampleLocations,
                includeFiles,
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = true,
                validNullabilityAnnotations =
                    defaultValidNullabilityAnnotations +
                        "org.checkerframework.checker.nullness.qual.Nullable",
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                dump(writerPlugin.writer.contents, File("build/docs/$testName").absolutePath)
            }
        }
    }

    /**
     * Executes dackka on source from an androidx checkout on the same machine. No validation.
     *
     * @param maxFolders limits the source files run against, in case of performance issues
     */
    fun crawlingExecTest(
        testName: String,
        checkoutRoot: String,
        excludedPaths: MutableList<String> = mutableListOf(),
        maxFolders: Int = 999999,
    ) {
        // We never intend to run dackka on these folders
        excludedPaths +=
            listOf(
                "test", // "test" folders don't contain "main"; stop recursion
                "development", // A folder of scripts
                "buildSrc", // Not published or documented
                "frameworks", // basically a recursive symlink to checkout-root
                "annotation-sampled", // not published and its docs confuse the samples system
            )
        var sourceRoots = mutableListOf<File>()
        val samplesRoots = mutableSetOf<String>() // Due to symlinks, we need to de-dupe Support4
        // We want to limit unnecessary recursion work, and we can check that no main folder is more
        // than 6 folders deep from the checkout root, as folder structure ~ package structure.
        val MAX_DEPTH = 6
        var currentDirs = listOf(File(checkoutRoot))
        var nextDirs = mutableListOf<File>()
        for (i in 0..MAX_DEPTH) {
            currentDirs.forEach { parent ->
                parent
                    .listFiles { child -> child.isDirectory }
                    ?.forEach { child ->
                        when (child.name) {
                            "main" -> sourceRoots += child
                            "samples" -> samplesRoots += child.absolutePath
                            in excludedPaths -> {}
                            else -> nextDirs += child
                        }
                    }
            }
            currentDirs = nextDirs
            nextDirs = mutableListOf()
            if (sourceRoots.size > maxFolders) break
        }
        sourceRoots = sourceRoots.take(maxFolders).toMutableList()

        logger.debug("Number of main folders found: ${sourceRoots.size}")
        logger.debug("Number of samples folders found: ${samplesRoots.size}")

        val configuration =
            makeExternalConfiguration(
                sourceRoots,
                samplesRoots.toList(),
                emptyList(),
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = true,
                validNullabilityAnnotations =
                    defaultValidNullabilityAnnotations +
                        "org.checkerframework.checker.nullness.qual.Nullable",
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                dump(writerPlugin.writer.contents, File("build/docs/$testName").absolutePath)
            }
        }
    }

    /**
     * Runs dackka on sources from a prebuilt; for verifying that there are no errors.
     *
     * Sources are unzipped from prebuilts/androidx/internal/ (grabbed via gradle dependency) into
     * `build/explodedSources/$artifactName-$version-sources/` Samples are kept locally (read from
     * `testData/$testName/samples/`), as they are not published
     */
    fun executePrebuilts(
        testName: String,
        artifactNames: List<String>,
        samples: Boolean = false,
    ) {
        val samplesBaseDir = "testData/$testName/samples"

        val configuration =
            makeExternalConfiguration(
                artifactNames.map { File("build/explodedSources/$it/").absoluteFile },
                if (samples) listOf(samplesBaseDir) else emptyList(),
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = true,
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                dump(writerPlugin.writer.contents, File("build/docs/$testName").absolutePath)
            }
        }
    }

    /**
     * Reads sources and outputs from a directory in `./testData/`, and validates based on them.
     *
     * Sources are located at testData/$path/source outputs are located at testData/$path/docs
     */
    open fun validateDirectory(
        path: String,
        sampleLocations: List<String> = emptyList(),
        includeFiles: List<String> = emptyList(),
        docRootPath: String = "reference",
        projectPath: String? = null,
        javaDocsDirectory: String? = "",
        kotlinDocsDirectory: String? = "kotlin",
        includedHeadTagsPathJava: String? = "_shared/_reference-head-tags.html",
        includedHeadTagsPathKotlin: String? = "_shared/_reference-head-tags.html",
        suffix: String = "source",
        useAndroidxBaseSourceLink: Boolean = false,
        hidingAnnotations: List<String> = listOf("androidx.annotation.RestrictTo"),
        includeHiddenParentSymbols: Boolean = false,
    ) {
        val samplesBaseDir = "testData/$path"
        val outputBaseDir = "testData/$path/docs"
        val sourceDir = "testData/$path/$suffix"
        val loggingDir = "testData/$path/logs"

        val inferredProjectPath =
            projectPath
                ?: File(sourceDir).listFiles().orEmpty().singleOrNull { it.isDirectory }?.name
                ?: "dokkatest"

        val configuration =
            makeInternalConfiguration(
                samplesBaseDir,
                sourceDir,
                sampleLocations,
                includeFiles,
                docRootPath,
                inferredProjectPath,
                javaDocsDirectory,
                kotlinDocsDirectory,
                includedHeadTagsPathJava,
                includedHeadTagsPathKotlin,
                useAndroidxBaseSourceLink,
                hidingAnnotations,
                includeHiddenParentSymbols,
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin.writer.contents, outputBaseDir)
                verifyOutput(logFiles(sourceDir), loggingDir)
            }
        }
    }

    /**
     * Runs dackka on sources from a prebuilt and validates against saved docs in `./testData/`.
     *
     * Sources are unzipped from prebuilts/androidx/internal/ (grabbed via gradle dependency) into
     * `build/explodedSources/$artifactName-$version-sources/` outputs are located at
     * `testData/$testName/docs` Samples are kept locally (read from `testData/$testName/samples/`),
     * as they are not published
     *
     * Use useAndroidxBaseSourceLink=true to include AndroidX source links in the generated page.
     * Multiple artifact names and source links don't work well together, links end up using
     * absolute paths.
     */
    fun validatePrebuilts(
        testName: String,
        artifactNames: List<String>,
        samples: Boolean = false,
        includeFiles: List<String> = emptyList(),
        useAndroidxBaseSourceLink: Boolean = true,
        versionMetadata: Boolean = false,
    ) {
        val outputBaseDir = "testData/$testName/docs"
        val samplesBaseDir = "testData/$testName/samples"
        val loggingDir = "testData/$testName/logs"

        val versionMetadataBaseDir = "testData/$testName/versionMetadata"
        val versionMetadataFiles =
            if (versionMetadata) {
                File(versionMetadataBaseDir).listFiles()?.map { it.absolutePath }
            } else {
                null
            }

        val sourceDir = "build/explodedSources"
        val configuration =
            makeExternalConfiguration(
                artifactNames.map { File("$sourceDir/$it/").absoluteFile },
                if (samples) listOf(samplesBaseDir) else emptyList(),
                includeFiles =
                    includeFiles.map { File("testData/$testName/source", it).absolutePath },
                docRootPath = "reference",
                projectPath = "androidx",
                javaDocsPath = "",
                kotlinDocsPath = "kotlin",
                useAndroidxBaseSourceLink = useAndroidxBaseSourceLink,
                versionMetadataFilesnames = versionMetadataFiles,
            )

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin),
        ) {
            renderingStage = { _: RootPageNode, _: DokkaContext ->
                verifyOutput(writerPlugin.writer.contents, outputBaseDir)
                verifyOutput(logFiles(sourceDir), loggingDir)
            }
        }
    }

    /** Creates a map from a filename for a logging level to the logged messages of that level. */
    private fun logFiles(sourceDir: String): Map<String, String> {
        val absoluteSourcePath = File(sourceDir).absolutePath
        return mapOf(
                // Start with "//" to match the filepaths from the writer plugin contents
                "//warnings.txt" to cleanLogMessages(logger.warnMessages, absoluteSourcePath),
                "//debug.txt" to cleanLogMessages(logger.debugMessages, absoluteSourcePath),
                "//error.txt" to cleanLogMessages(logger.errorMessages, absoluteSourcePath),
            )
            .filter { it.value.isNotEmpty() }
    }

    private fun cleanLogMessages(messages: List<String>, sourceDir: String): String {
        return messages.map { it.replace(sourceDir, "\$SRC_DIR") }.sorted().joinToString("\n")
    }

    /** Confirms that the given file to output map matches the contents of the given directory. */
    private fun verifyOutput(generatedFiles: Map<String, String>, outputPath: String) {
        val outputDirectory = File(outputPath).absolutePath

        val dumpedFile = File("build/docs/$outputPath")
        dump(generatedFiles, dumpedFile.absolutePath)

        for ((fileName, generatedContent) in generatedFiles) {
            val expectedFile = File(outputDirectory, fileName)
            val expectedText =
                if (expectedFile.exists()) {
                    expectedFile.readText()
                } else {
                    ""
                }

            if (expectedText != generatedContent) {
                val message =
                    """
                    |Unexpected output in $fileName.
                    |To update the expected output to match the current output, run this command:
                    |
                    |  rm -rf $outputDirectory && cp -r ${dumpedFile.absolutePath} $outputDirectory
                    |
                    |Difference in outputs:
                """
                        .trimMargin()
                assertWithMessage(message).that(generatedContent).isEqualTo(expectedText)
            }
        }
        val expectedFileList = File(outputPath).recursivelyListFiles()
        // This same "fix" happens automatically when the file is written, so it's needed to match
        val fixedGeneratedPaths = generatedFiles.keys.map { it.replace("//", "/") }
        for (eFile in expectedFileList) {
            assertWithMessage("File ${eFile.path} was expected but not generated!")
                .that(eFile.path.removePrefix(outputPath) in fixedGeneratedPaths)
                .isTrue()
        }
    }

    private fun File.recursivelyListFiles(): List<File> =
        (this.listFiles { it: File -> !it.isDirectory }?.asList() ?: emptyList()) +
            (this.listFiles { it: File -> it.isDirectory }
                ?.flatMap { it: File -> it.recursivelyListFiles() } ?: emptyList())

    /** Exports the file to output map to outputPath. */
    private fun dump(generatedFiles: Map<String, String>, outputPath: String) {
        val outputDirectory = File(outputPath)
        outputDirectory.deleteRecursively()

         for ((fileName, fileContent) in generatedFiles) {
            val expectedFile = File(outputDirectory, fileName)
            expectedFile.parentFile.mkdirs()
            expectedFile.writeText(fileContent)
        }
    }
}

fun classpathFromFile(file: String): List<String> = File(file).bufferedReader().readLines()
