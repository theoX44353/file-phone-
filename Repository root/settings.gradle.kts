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

rootProject.name = "dokka-devsite-plugin"

include(":integration-tests:cli")

pluginManagement {
    repositories {
        maven("../../prebuilts/androidx/external")
    }
}

buildscript {
    repositories {
        maven("../../prebuilts/androidx/external")
    }
    dependencies {
        classpath("com.gradle:develocity-gradle-plugin:3.19.1")
        classpath("com.gradle:common-custom-user-data-gradle-plugin:2.1")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("../../prebuilts/androidx/external")
        maven("../../prebuilts/androidx/internal")
    }
}

apply(plugin = "com.gradle.develocity")
apply(plugin = "com.gradle.common-custom-user-data-gradle-plugin")

val BUILD_NUMBER = System.getenv("BUILD_NUMBER")
develocity {
    server = "https://ge.androidx.dev"

    buildScan {
        capture.fileFingerprints.set(true)
        obfuscation {
            hostname { _ -> "unset" }
            ipAddresses { listOf("0.0.0.0") }
        }
        if (BUILD_NUMBER != null) {
            value("BUILD_NUMBER", BUILD_NUMBER)
            link("ci.android.com build", "https://ci.android.com/builds/branches/aosp-androidx-main/grid?head=$BUILD_NUMBER&tail=$BUILD_NUMBER")
        }

        publishing.onlyIf {
            it.isAuthenticated
        }
    }
}
