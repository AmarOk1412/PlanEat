/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    alias(libs.plugins.android.application)

    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
    kotlin("plugin.serialization") version "2.2.0-RC"
    id("com.google.devtools.ksp") version "2.3.2"
    id("com.chaquo.python")
}

android {
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    namespace = "com.planeat.planeat"

    defaultConfig {
        applicationId = "com.planeat.planeat"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = 36
        versionCode = 18
        versionName = "1.1.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    signingConfigs {
        // We use a bundled debug keystore, to allow debug builds from CI to be upgradable
        named("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            ndk.debugSymbolLevel = "full"
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    androidResources {
        noCompress += listOf("tflite")
    }

    // Specify tflite file should not be compressed for the app apk
}

dependencies {
    // Translation
    implementation(libs.translate)
    // Classify recipes
    implementation(libs.litert)
    implementation(libs.tensorflow.lite.select.tf.ops)
    implementation(libs.litert.metadata)
    implementation(libs.litert.support.api)
    // FOR RELEASE KEEP THIS
    implementation(libs.androidx.room.common)
    implementation(libs.room.ktx)
    implementation(libs.transport.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android.v1102)
    implementation(libs.error.prone.annotations)
    // Database
    implementation(libs.androidx.room.runtime.v261)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.rules)
    annotationProcessor(libs.room.compiler.v261)
    ksp(libs.room.compiler.v261)
    implementation(libs.kotlinx.serialization.json)
    // Parsing
    api(libs.jsoup)
    // Compose UI
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.compose.material)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose) // navigation.compose.NavHost
    implementation(libs.androidx.compose.materialWindow)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.accompanist.adaptive.v0262beta)
    // Async image
    implementation(libs.coil.compose.v260)

    // Test rules and transitive dependencies:
    androidTestImplementation(libs.androidx.ui.test.junit4)
    // Needed for createComposeRule(), but not for createAndroidComposeRule<YourActivity>():
    debugImplementation(libs.androidx.ui.test.manifest)

    // QrCode
    implementation(libs.compose.qr.code)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.barcode.scanning)


}
