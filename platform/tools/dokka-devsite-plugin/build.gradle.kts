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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

defaultTasks = mutableListOf("test", "jar", "shadowJar", "ktCheck", "publish", "zipTestResults")

group = "com.google.devsite"
version = "1.6.3" // This is appended to archiveBaseName in the ShadowJar task.

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    id("application")
    id("maven-publish")
}

application {
    mainClass.set("org.jetbrains.dokka.MainKt")
}

dependencies {
    implementation(libs.dokka.base)
    implementation(libs.dokka.core)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.module.kotlin)
    compileOnly(libs.dokka.analysis.api)
    runtimeOnly(libs.dokka.analysis.descriptors)

    implementation("org.jsoup:jsoup:1.16.2") // Remove when upstream updates their version

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.9.1")
    implementation(libs.kotlin.reflect)
    implementation(libs.coroutines.core)

    testImplementation(libs.dokka.base.test.utils)
    testImplementation(libs.kotlin.test)
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation(libs.dokka.test.api)

    implementation(libs.dokka.cli) // Used in CLI integration test
}

val shadowJar = tasks.withType<ShadowJar> {
    archiveBaseName.set("dackka")
    isZip64 = true
    destinationDirectory.set(getDistributionDirectory())
}

// Do not publish shadow jar to maven
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
    skip()
}

val kmpIntegrationSourceDirs = listOf("simple-kmp", "datastore-kmp")
// compose requires compose compiler plugin, fragment is fragmentary (missing internal dependencies)
val unCompilableIntegrationTestSourceDirs = listOf("collections-ktx", "compose", "fragment")
val (kmpSourceDirs, javaSourceDirs) = File("testData").listFiles()!!
    .mapNotNull { testDir -> testDir.listFiles()?.singleOrNull { "source" in it.name } }
    .filterNot { unCompilableIntegrationTestSourceDirs.any { badName -> badName in it.path  } }
    .partition { kmpIntegrationSourceDirs.any { badName -> badName in it.path  } }
val javaTestDataSS: SourceSet by sourceSets.creating {
    javaSourceDirs.forEach { java.srcDir(it) }
}
val kmpTestDataSSs: List<SourceSet> = kmpSourceDirs.flatMap {
    it.listFiles()!!.map { sourceSetDir ->
        val ss = sourceSets.maybeCreate(sourceSetDir.name)
        //ss.java.srcDir(sourceSetDir) // TODO(make KMP testData compile)
        ss
    }
}

val testDataImpl = project.configurations.getByName(javaTestDataSS.implementationConfigurationName)
val testDataAars by project.configurations.creating
val testDataParent by project.configurations.creating
testDataParent.isCanBeResolved = false
fun Configuration.setResolveSources(isKmp: Boolean = false) {
    isTransitive = false
    isCanBeConsumed = false
    attributes {
        attribute(
            Usage.USAGE_ATTRIBUTE,
            project.objects.named(if (isKmp) "androidx-multiplatform-docs" else Usage.JAVA_RUNTIME)
        )
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            project.objects.named(Category.DOCUMENTATION)
        )
        attribute(
            DocsType.DOCS_TYPE_ATTRIBUTE,
            project.objects.named(DocsType.SOURCES)
        )
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            project.objects.named(LibraryElements.JAR)
        )
    }
}
testDataParent.setResolveSources()
val testDataSources by project.configurations.creating
testDataSources.extendsFrom(testDataParent)
testDataSources.setResolveSources()
val testDataSourcesKmp by project.configurations.creating
testDataSourcesKmp.extendsFrom(testDataParent)
testDataSourcesKmp.setResolveSources(isKmp = true)

val lifecycleVersion = "2.6.0"
val collectionsVersion = "1.3.0"
val composeVersion = "1.5.0"
val composeMaterial3Version = "1.2.0"
val benchmarkVersion = "1.2.0"
val appsearchVersion = "1.1.0-alpha05"
val biometricVersion = "1.4.0-alpha02"
val cameraVersion = "1.5.0-alpha02"
val carVersion = "1.4.0"
dependencies {
    testDataImpl("io.reactivex.rxjava3:rxjava:3.0.2")
    testDataImpl("io.reactivex.rxjava2:rxjava:2.2.9")
    testDataImpl("org.robolectric:sandbox:4.12.2")
    testDataImpl(libs.coroutines.core)
    testDataImpl(libs.coroutines.rx2)
    testDataImpl(libs.coroutines.rx3)
    testDataImpl(libs.coroutines.guava)
    testDataImpl("org.robolectric:android-all-instrumented:13-robolectric-9030017-i7")
    testDataImpl("junit:junit:4.13.2")
    testDataImpl("com.google.truth:truth:1.1.3")
    testDataImpl("com.android.tools.build:gradle:8.3.1")
    testDataImpl("org.jspecify:jspecify:1.0.0")

    testDataImpl(fileTree("${layout.buildDirectory.get()}/exploded"))

    testDataAars("androidx.lifecycle:lifecycle-livedata-core:$lifecycleVersion")
    testDataAars("androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")
    testDataAars("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    testDataAars("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    testDataAars("androidx.recyclerview:recyclerview:1.3.0")
    testDataAars("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    testDataAars("androidx.compose.foundation:foundation:1.0.5")
    testDataAars("androidx.activity:activity:1.6.0")
    testDataAars("androidx.paging:paging-common:3.2.0")

    testDataSources("androidx.fragment:fragment:1.6.0")
    // TODO: publish sample source code in a way accessible to dackka (/studio) b/153171116
    // testDataSources("androidx.fragment:fragment-samples:1.6.0-alpha01")
    testDataSources("androidx.lifecycle:lifecycle-common:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-compiler:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata-core:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata-core-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-reactivestreams:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-runtime:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-runtime-testing:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    testDataSources("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
    testDataSources("androidx.activity:activity:1.6.0")
    testDataSources("androidx.activity:activity-ktx:1.6.0")
    testDataSources("androidx.ads:ads-identifier:1.0.0-alpha04")
    testDataSources("androidx.ads:ads-identifier-common:1.0.0-alpha04")
    testDataSources("androidx.ads:ads-identifier-provider:1.0.0-alpha04")
    testDataSourcesKmp("androidx.annotation:annotation:1.6.0")
    testDataSources("androidx.annotation:annotation-experimental:1.3.0")
    //testDataSources("androidx.annotation:annotation-experimental-lint:1.0.0-rc01") // need dep
    testDataSources("androidx.appcompat:appcompat:1.6.0")
    testDataSources("androidx.appcompat:appcompat-resources:1.6.0")
    testDataSources("androidx.appsearch:appsearch:$appsearchVersion")
    testDataSources("androidx.appsearch:appsearch-builtin-types:$appsearchVersion")
    testDataSources("androidx.appsearch:appsearch-compiler:$appsearchVersion")
    testDataSources("androidx.appsearch:appsearch-ktx:$appsearchVersion")
    testDataSources("androidx.appsearch:appsearch-debug-view:$appsearchVersion")
    testDataSources("androidx.appsearch:appsearch-platform-storage:$appsearchVersion")
    testDataSources("androidx.appsearch:appsearch-local-storage:$appsearchVersion")
    testDataSources("androidx.arch.core:core-common:2.2.0")
    testDataSources("androidx.arch.core:core-runtime:2.2.0")
    testDataSources("androidx.arch.core:core-testing:2.2.0")
    testDataSources("androidx.asynclayoutinflater:asynclayoutinflater:1.0.0")
    testDataSources("androidx.autofill:autofill:1.3.0-beta01")
    testDataSources("androidx.benchmark:benchmark:1.0.0-alpha03")
    testDataSources("androidx.benchmark:benchmark-common:$benchmarkVersion")
    testDataImpl(gradleApi())
    testDataSources("androidx.benchmark:benchmark-junit4:$benchmarkVersion")
    testDataSources("androidx.benchmark:benchmark-macro:$benchmarkVersion")
    testDataSources("androidx.benchmark:benchmark-macro-junit4:$benchmarkVersion")
    testDataSources("androidx.biometric:biometric:$biometricVersion")
    testDataSources("androidx.biometric:biometric-ktx:$biometricVersion")
    testDataSources("androidx.browser:browser:1.5.0")
    testDataSources("androidx.camera:camera-camera2:$cameraVersion")
    testDataSources("androidx.camera:camera-camera2-pipe:1.0.0-alpha01")
    testDataSources("androidx.camera:camera-camera2-pipe-testing:1.0.0-alpha01")
    testDataSources("androidx.camera:camera-core:$cameraVersion")
    testDataSources("androidx.camera:camera-extensions:$cameraVersion")
    testDataSources("androidx.camera:camera-lifecycle:$cameraVersion")
    testDataSources("androidx.camera:camera-mlkit-vision:$cameraVersion")
    testDataAars("com.google.mlkit:vision-interfaces:16.0.0")
    testDataSources("androidx.camera:camera-extensions:$cameraVersion")
    testDataSources("androidx.camera:camera-previewview:1.1.0-beta02")
    testDataSources("androidx.camera:camera-video:$cameraVersion")
    testDataSources("androidx.camera:camera-view:$cameraVersion")
    testDataSources("androidx.camera:camera-viewfinder:1.4.0-alpha08")
    testDataSources("androidx.car.app:app:$carVersion")
    testDataSources("androidx.car.app:app-aaos:1.0.0-alpha01")
    testDataSources("androidx.car.app:app-automotive:$carVersion")
    testDataSources("androidx.car.app:app-projected:$carVersion")
    testDataSources("androidx.car.app:app-testing:$carVersion")
    // testDataSources("androidx.car:car:1.0.0-alpha7") // Obsolete artifacts
    // testDataSources("androidx.car:car-cluster:1.0.0-alpha5")
    // testDataSources("androidx.car:car-moderator:1.0.0-alpha1")
    testDataSources("androidx.cardview:cardview:1.0.0")
    // We do not test against androidx.test, because they are not part of the androidx build
    // and also publish source jars with problematic no-write-permission on parts
    testDataAars("androidx.test.uiautomator:uiautomator:2.2.0") // no source jar
    testDataSources("androidx.tracing:tracing:1.2.0")
    testDataSources("androidx.tracing:tracing-ktx:1.2.0")
    testDataSources("androidx.tracing:tracing-perfetto:1.0.0")
    testDataSources("androidx.tracing:tracing-perfetto-binary:1.0.0")
    testDataSources("androidx.tracing:tracing-perfetto-common:1.0.0-alpha16")

    testDataSources("androidx.paging:paging-common:3.2.0")
    testDataSources("androidx.paging:paging-common-ktx:3.2.0")
    testDataSources("androidx.paging:paging-runtime:3.2.0")
    testDataSources("androidx.paging:paging-runtime-ktx:3.2.0")
    testDataSources("androidx.paging:paging-rxjava2:3.2.0")
    testDataSources("androidx.paging:paging-rxjava2-ktx:3.2.0")
    testDataSources("androidx.paging:paging-rxjava3:3.2.0")
    testDataSources("androidx.paging:paging-guava:3.2.0")
    testDataSources("androidx.paging:paging-compose:3.2.0")

    // Compose is KMP, and they publish KMP source jars
    testDataSourcesKmp("androidx.compose.animation:animation:$composeVersion")
    testDataSourcesKmp("androidx.compose.animation:animation-core:$composeVersion")
    testDataSourcesKmp("androidx.compose.animation:animation-graphics:$composeVersion")
    testDataSourcesKmp("androidx.compose.foundation:foundation:$composeVersion")
    testDataSourcesKmp("androidx.compose.foundation:foundation-layout:$composeVersion")
    testDataSourcesKmp("androidx.compose.material3:material3:$composeMaterial3Version")
    testDataSourcesKmp("androidx.compose.material3:material3-window-size-class:$composeMaterial3Version")
    testDataSourcesKmp("androidx.compose.runtime:runtime:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-geometry:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-graphics:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-test:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-test-junit4:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-text:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-tooling:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-unit:$composeVersion")
    testDataSourcesKmp("androidx.compose.ui:ui-util:$composeVersion")

    testDataSourcesKmp("androidx.collection:collection:$collectionsVersion")
    testDataSourcesKmp("androidx.datastore:datastore-core:1.1.0")
}

val explodeAars by tasks.registering(Sync::class) {
    into("${layout.buildDirectory.get()}/exploded")
    from(testDataAars) {
        include("*.jar")
    }

    testDataAars.files.filter { it.extension == "aar" }.forEach { arch ->
        from(zipTree(arch)) {
            include("classes.jar")
            rename { arch.nameWithoutExtension + ".jar" }
        }
    }
}

val explodeSources by tasks.registering {
    (testDataSources.files + testDataSourcesKmp.files).filter {
        it.nameWithoutExtension.endsWith("sources")
    }.forEach { arch ->
        sync {
            val splitName = arch.nameWithoutExtension.split("-")
            val versionInd =
                splitName.indexOfFirst { '.' in it } // index of first block in version num
            val baseName = splitName.subList(0, versionInd).joinToString(separator = "-")
            logger.debug("Unzipping prebuilt for $baseName")
            from(zipTree(arch))
            into("${layout.buildDirectory.get()}/explodedSources/$baseName")
        }
    }
}

val classpathForTests by tasks.registering(ClasspathForTestsTask::class) {
    dependsOn(explodeAars)
    dependsOn(explodeSources)
    classpath = javaTestDataSS.compileClasspath
    kmpTestDataSSs.forEach { classpath += it.compileClasspath }
    location.set(file("testData/classpath.txt"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xmulti-platform")
    }
    dependsOn(explodeAars)
}

val testTask = tasks.named<Test>("test") {
    dependsOn(classpathForTests)
    dependsOn(tasks.withType<KotlinCompile>())

    maxHeapSize = "16g"
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    testLogging.events = hashSetOf(
        TestLogEvent.FAILED,
        TestLogEvent.STANDARD_OUT,
        TestLogEvent.STANDARD_ERROR
    )
    if (isBuildingOnServer()) ignoreFailures = true
}

val zipTask = project.tasks.register<Zip>("zipTestResults") {
    dependsOn(testTask)
    destinationDirectory.set(File(getDistributionDirectory(), "host-test-reports"))
    archiveFileName.set("dackka-tests.zip")
    from(project.file(testTask.flatMap { it.reports.junitXml.outputLocation}))
}

val ktfmtConfiguration: Configuration by configurations.creating
dependencies {
    ktfmtConfiguration("com.facebook:ktfmt:0.49")
}

class KtFilesProvider : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> {
        val process = ProcessBuilder("sh", "-c", "find src -type f -name '*.kt'")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val ktFiles = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return ktFiles.split("\n")
    }
}

val ktCheck by tasks.creating(JavaExec::class) {
    description = "Check Kotlin code style."
    group = "Verification"
    classpath = ktfmtConfiguration
    mainClass.set("com.facebook.ktfmt.cli.Main")

    argumentProviders.add(KtFilesProvider())
    args = listOf("--kotlinlang-style", "--dry-run", "--set-exit-if-changed")
}

val ktFormat by tasks.creating(JavaExec::class) {
    description = "Fix Kotlin code style deviations."
    group = "Formatting"
    classpath = ktfmtConfiguration
    mainClass.set("com.facebook.ktfmt.cli.Main")

    argumentProviders.add(KtFilesProvider())
    args = listOf("--kotlinlang-style")
}

publishing {
    publications {
        create<MavenPublication>(name = "Dackka") {
            from(components["java"])
            pom {
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("The Android Open Source Project")
                    }
                }
                scm {
                    connection.set("scm:git:https://android.googlesource.com/platform/tools/dokka-devsite-plugin/")
                    url.set("https://android.googlesource.com/platform/tools/dokka-devsite-plugin//")
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("file://${getDistributionDirectory().canonicalPath}/repo/repository")
        }
    }
}

/**
 * The build server will copy the contents of the distribution directory and make it available for
 * download.
 */
fun getDistributionDirectory(): File {
    return if (System.getenv("DIST_DIR") != null) {
        File(System.getenv("DIST_DIR"))
    } else {
        File(projectDir, "out/dist").apply { mkdirs() }
    }
}

fun isBuildingOnServer(): Boolean {
    return System.getenv("OUT_DIR") != null && System.getenv("DIST_DIR") != null
}

abstract class ClasspathForTestsTask: DefaultTask() {
    @get:Classpath abstract var classpath: FileCollection
    @get:OutputFile abstract val location: RegularFileProperty

    @TaskAction
    fun run(){
        location.get().asFile.writeText(classpath.joinToString(separator = "\n"))
    }
}
