/*

 * Copyright 2021 The Android Open Source Project
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

import java.io.File
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet
import  org . jetbrains . dokka . model . doc . Pre
import org.jetbrains.dokka.model.doc.Text

internal var failOnMissingSamples = true
internal  var  isRunningInTyreTests = false
// TODO(KMP) we currently have no plan to provide KMP samples b/181224204, so we assume `common`
/*
/**
 * This invokes the EnvironmentAndFacade object to turn a DRI
 * (like "dokkatest.sampleAnnotation.samples.FunctionContainingClassSample") into a PSIElement
 * which is part of the abstract syntax tree representation of kotlin code.
*/
internal fun fqNameToPsiElement(
    resolutionFacade: DokkaResolutionFacade,
    functionName: String,
): PsiElement? {
    val packageName = functionName.takeWhile { it != '.' }
    val descriptor = resolutionFacade.resolveSession.getPackageFragment(FqName(packageName))
        ?: throw RuntimeException("Cannot find descriptor for package $packageName")
    val symbol = resolveKDocLink(
        BindingContext.EMPTY,
        resolutionFacade,
        descriptor,
        null,
        functionName.split("."),
    ).firstOrNull()
        ?: throw RuntimeException("Unresolved function $functionName in @sample")
    return DescriptorToSourceUtils.descriptorToDeclaration(symbol)
}

/**
 * This takes a PSIElement and returns the source code it is associated with. For example, if
 * the PSIElement represents a function, it returns the source code of the function.
*/
internal fun processBody(psiElement: PsiElement): String {
    val text = processSampleBody(psiElement).trim { it == '\n' || it == '\r' }.trimEnd()
    val lines = text.split("\n")
    val indent = lines.filter(String::isNotBlank).minOfOrNull {
        it.takeWhile(Char::isWhitespace).count()
    } ?: 0
    return lines.joinToString("\n") { it.drop(indent) }
}

// Auxiliary function for processBody
private fun processSampleBody(psiElement: PsiElement): String = when (psiElement) {
    is KtDeclarationWithBody -> {
        when (val bodyExpression = psiElement.bodyExpression) {
            is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
            else -> bodyExpression!!.text
        }
    }
    else -> psiElement.text
}

/**
 * We know these imports should not be included in the sampled code, even though they are
 * referenced.
*/
private val importsToIgnore = listOf("androidx.annotation.Sampled")
    .map { ImportPath.fromString(it) }

// Based on old dokka's implementation, hide all non-androidx import statements
private val androidxPackage = Name.identifier("androidx")
*/
/**
* We know these imports should not be included in the sampled code, even though they are
* referenced.
*/
private val importsToIgnore = listOf("androidx.annotation.Sampled")

// Based on old dokka's implementation, hide all non-androidx import statements
private const val androidxPackage = "androidx."

internal fun processImports(sample: SampleSnippet): String {
    val importList = sample.imports
    val filteredImports =
        importList.filter { importString ->
            val importName = importString.trim()
            // Hide all non-androidx imports
            if (!importName.startsWith(androidxPackage)) return@filter false
            // Hide all explicitly ignored imports (like androidx.annotations.Sampled)
            if (importsToIgnore.any { importName.startsWith(it) }) return@filter false
            // Hide empty lines
            if (importName.trim().isEmpty()) return@filter false

            // Return whether any of the code in the sample uses this import
            return@filter importName.substringAfterLast(".") in sample.body
        }
    // Don't spam blank lines if there are no imports (post-filtering)
    if (filteredImports.isEmpty()) {
        return ""
    }
    // The first blank line doesn't appear in rendered html, just makes raws look nicer
    return "\n" + filteredImports.joinToString(separator = "\n") { "import $it" } + "\n\n"
}

/*
/**
 * This takes a PSIElement and returns the list of import statements it requires.
 * For example, the list of import statements associated with all elements in a function's
 * source code.
 * Certain import statements are guaranteed to be removed, for example the import of the
 * `@Sampled` annotation itself.
 * If the file contains multiple such functions, and therefore import statements which are not
 * associated with that function's source code, those import statements are not included.
*/
internal fun processImports(psiElement: PsiElement): String {
    val psiFile = psiElement.containingFile

    val sampleExpressionCalls = mutableSetOf<String>()
    (psiElement as KtDeclarationWithBody).bodyExpression!!.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
            sampleExpressionCalls.addIfNotNull(expression.calleeExpression?.text)
            super.visitCallExpression(expression)
        }
    })
    if (psiFile is KtFile) {
        val filteredImports = psiFile.importList?.imports?.filter { element ->
            val fqImportName = element.importPath?.fqName ?: return@filter false
            // Hide all non-androidx imports
            if (!fqImportName.startsWith(androidxPackage)) return@filter false
            // Hide all explicitly ignored imports (like androidx.annotations.Sampled)
            if (element.importPath in importsToIgnore) return@filter false
            // Hide non-import statements that somehow sneak in here? From old dokka.
            if (element !is KtImportDirective) return@filter false
            // Hide empty lines
            if (element.text.trim().isEmpty()) return@filter false

            // Return whether any of the code in the sample uses this import
            return@filter sampleExpressionCalls.any { call ->
                call == fqImportName.shortName().identifier
            }
        }.orEmpty()
        // Don't spam blank lines if there are no imports (post-filtering)
        if (filteredImports.isEmpty()) { return "" }
        // The first blank line doesn't appear in rendered html, just makes raws look nicer
        return "\n" + filteredImports.joinToString(separator = "\n") { it.text } + "\n\n"
    } else {
        throw RuntimeException("${psiFile::class} is not a supported sample file type")
    }
}

/**
 * Constructs EnvironmentAndFacade objects for each sourceSet.
 * These are mysterious upstream objects capable of complex parsing of the Kotlin abstract syntax
 * tree of the files they are generated from. In particular, they can create PSIElements for kotlin
 * code objects given a DRI like dokkatest.sampleAnnotation.samples.FunctionContainingClassSample
 *
 * These are generated for each sourceSet with samples, and returned as a map.
 */
internal fun setUpAnalysis(
    context: DokkaContext,
    sampleAnalysisEnvironmentCreator: SampleAnalysisEnvironmentCreator,
) = context.configuration.sourceSets
    /*.filter { it.samples.isNotEmpty() }*/.associateWith { sourceSet ->
        sampleAnalysisEnvironmentCreator.create()
        /*AnalysisEnvironment(
            DokkaMessageCollector(context.logger),
            sourceSet.analysisPlatform,
        ).run {
            if (analysisPlatform == Platform.jvm) {
                addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
            }
            sourceSet.classpath.forEach(::addClasspath)

            addSources(sourceSet.samples.toList())

            loadLanguageVersionSettings(sourceSet.languageVersion, sourceSet.apiVersion)

            val environment = createCoreEnvironment()
            createResolutionFacade(environment).first
        }*/
    }
*/
/** Resolves a javadoc `{@sample path/to/file.javaOrXml}`. Takes Text, returns <pre><code>. */
internal fun convertTextToJavadocSample(
    block: Text,
    samples: Set<File>,
): Pre {
    val sampleLine =
        block.body
            .trim()
            .removePrefix("{")
            .removeSuffix("}")
            // Upstream inserts "*"s on line breaks within the { }
            .split(" ")
            .filter { it.isNotEmpty() && it != "*" }
    if (sampleLine[0] != "@sample") {
        throw RuntimeException(
            "invalid first line of " +
                "purported sample block: \"${sampleLine[0]}\"; expected to be \"@sample\"",
        )
    }
    val filePath = sampleLine[1]
    val whatSamples = sampleLine[2]
    val sampleFiles = samples.allFiles()
    var resolvedFile = sampleFiles.filter { it.absolutePath.contains(filePath) }
    // When we run tests in dackka, we use androidx source jars from prebuilts, but cannot get
    // samples the same way, so the path of the samples is wrong, and we name-mangle to resolve them
    if (resolvedFile.isEmpty() && isRunningInDackkasTests) {
        resolvedFile = sampleFiles.filter { it.name == filePath.split("/").last() }
    }
    return when (resolvedFile.size) {
        0 ->
            if (failOnMissingSamples) {
                throw RuntimeException(
                    "Unable to find the sample file $filePath in the samples directory " +
                        sampleFiles.map { it.path }.reduce { acc, s -> acc.commonPrefixWith(s) },
                )
            } else {
                Pre(emptyList())
            }
        1 -> {
            // extractCodeBlockFromFile can only work with .java and .xml files
            val fileExtension = resolvedFile.single().path.substringAfterLast(".")
            Pre(
                params = mapOf("class" to "prettyprint lang-$fileExtension"),
                children = listOf(extractCodeBlockFromFile(resolvedFile.single(), whatSamples)),
            )
        }
        else -> throw RuntimeException("Somehow, multiple files with path $filePath were found.")
    }
}

private fun Iterable<File>.allFiles() = this.map { it.allFiles() }.flatten()

private fun File.allFiles(): List<File> =
    if (this.isFile) {
        listOf(this)
    } else listFiles()!!.asIterable().allFiles()

/**
 * Extracts sample code from a file (probably a java or xml file). Takes all lines between
 * BEGIN_INCLUDE(block_to_take) and END_INCLUDE(block_to_take) Reduces all indents to that of the
 * first line
 */
internal fun extractCodeBlockFromFile(sampleFile: File, blockToTake: String): Text {
    var result = "\n"
    var inBlock = false
    var indentSize = -1
    sampleFile.readLines().forEach { line ->
        if ("END_INCLUDE($blockToTake)" in line) return Text(result)
        if (inBlock) {
            if (indentSize == -1) indentSize = indentSize(line)
            result += line.removePrefix(" ".repeat(indentSize)) + "\n"
        }
        if ("BEGIN_INCLUDE($blockToTake)" in line) inBlock = true
    }
    throw RuntimeException("No END_INCLUDE($blockToTake) in ${sampleFile.name} sample!")
}

/** The number of spaces before the first non-space character on this line */
private fun indentSize(it: String) = (it.length - it.trimStart().length).coerceAtLeast(0)
