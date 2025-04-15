/*
 * Copyright (C) 2020 The Android Open Source Project
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

// This project confirms that Dokka can be invoked as a .jar from the command line

val runnerJar = configurations.create("runnerJar") {
    dependencies.add(
        project.dependencies.project(
            mapOf(
                "path" to ":",
                "configuration" to "shadow"
            )
        )
    )
}

class GlobalDocsLink(
    @get:Input
    val url: String,
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    val packageListUrl: RegularFile
)

@CacheableTask
abstract class DackkaRunner : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations
    @get:Classpath
    abstract val dackkaClasspath: ConfigurableFileCollection
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val sourceDirectories: ConfigurableFileCollection
    @get:[InputDirectory PathSensitive(PathSensitivity.RELATIVE)]
    abstract val samplesDirectory: DirectoryProperty
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val include: RegularFileProperty
    @get:Nested
    abstract val globalDocsLinks: ListProperty<GlobalDocsLink>

    @get:OutputFile
    abstract val config: RegularFileProperty
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        generateConfig()
        outputDirectory.get().asFile.deleteRecursively()

        // Track all warning lines to write to a file sorted after dackka finishes
        val loggedLines = mutableListOf<String>()

        execOperations.javaexec {
            classpath = dackkaClasspath
            args = listOf(
                config.get().asFile.absolutePath,
                "-loggingLevel", "WARN"
            )
            logging.addStandardOutputListener {
                if (it.isNotBlank()) {
                    loggedLines.add(
                        it.toString().replace(
                            projectDirectory.get().asFile.absolutePath,
                            "\$SRC_DIR"
                        )
                    )
                }
            }
        }

        if (loggedLines.isNotEmpty()) {
            val loggingFile = File(outputDirectory.get().asFile, "logging.txt")
            loggingFile.writeText(loggedLines.sorted().joinToString("\n"))
        }
    }

    private fun generateConfig() {
        val configContents = """
            {
              "outputDir": "${outputDirectory.get().asFile.absolutePath}",
              "offlineMode": "true",
              "sourceSets": [
                {
                  "moduleDisplayName": "Sample",
                  "externalDocumentationLinks": [
${globalDocsLinks.get().joinToString(separator = ",\n") {
"""                        { "url": "${it.url}", "packageListUrl": "file://${it.packageListUrl.asFile.absolutePath}" }"""
}}
                  ],
                  "sourceSetID": {
                    "scopeId": "sample",
                    "sourceSetName": "main"
                  },
                  "sourceRoots": [${sourceDirectories.joinToString(separator = ", ") { "\"${it.absolutePath}\"" }}],
                  "samples": ["${samplesDirectory.get().asFile.absolutePath}"],
                  "include": ["${include.get().asFile.absolutePath}"],
                  "documentedVisibilities": ["PUBLIC", "PROTECTED"]
                }
              ],
                "pluginsConfiguration": [
                  {
                    "fqPluginName": "com.google.devsite.DevsitePlugin",
                    "serializationFormat": "JSON",
                    "values": "{ \"projectPath\": \"androidx\", \"excludedPackages\": [ \".*excluded.*\" ], \"javaDocsPath\": \"\", \"kotlinDocsPath\": \"kotlin\", \"annotationsNotToDisplay\": [ \"java.lang.Override\", \"kotlin.ParameterName\" ] }"
                  }
                ]
            }
        """.trimIndent()
        config.get().asFile.writeText(configContents)
    }
}

val runTask = tasks.register<DackkaRunner>("run") {
    description = "Ensures that dackka can be invoked as a .jar from the command line"
    dackkaClasspath.from(runnerJar)
    sourceDirectories.from(
        layout.projectDirectory.dir("src/testData/androidx/paging/source"),
        layout.projectDirectory.dir("src/testData/androidx/paging/excluded"),
    )
    samplesDirectory.set(
        layout.projectDirectory.dir("src/testData/androidx/paging/samples")
    )
    include.set(layout.projectDirectory.file("src/testData/androidx/paging/metadata.md"))
    globalDocsLinks.addAll(
        GlobalDocsLink(
            url = "https://kotlinlang.org/api/latest/jvm/stdlib/",
            rootProject.layout.projectDirectory.file("testData/package-lists/kotlin/package-list")
        ),
        GlobalDocsLink(
            url = "https://developer.android.com/reference",
            rootProject.layout.projectDirectory.file("testData/package-lists/android/package-list")
        ),
        GlobalDocsLink(
            url = "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
            rootProject.layout.projectDirectory.file("testData/package-lists/coroutines/package-list")
        ),
        GlobalDocsLink(
            url = "https://guava.dev/releases/18.0/api/docs/package-list",
            rootProject.layout.projectDirectory.file("testData/package-lists/guava/package-list")
        )
    )

    outputDirectory.set(project.layout.buildDirectory.dir("docs"))
    config.set(project.layout.buildDirectory.file("resources/config.json"))
    projectDirectory.set(project.layout.projectDirectory)
}

@CacheableTask
abstract class VerifyRun : DefaultTask() {
    @get:[InputDirectory PathSensitive(PathSensitivity.RELATIVE)]
    abstract val directoryToValidate: DirectoryProperty
    @TaskAction
    fun verify() {
        listOf(
            "reference/androidx/index.html",
            "reference/androidx/classes.html",
            "reference/androidx/packages.html"
        ).map {
            File(directoryToValidate.get().asFile, it)
        }.forEach {
            if (!it.exists()) {
                throw GradleException("Failed to create $it")
            }
        }
    }
}

tasks.register<VerifyRun>("verifyRun") {
    directoryToValidate.set(runTask.flatMap { it.outputDirectory })
    outputs.file(File(temporaryDir, "fake-output-for-up-to-date-checks"))
}

tasks.register("test") {
    dependsOn("verifyRun")
}
