@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mannodermaus.android.junit5)
    alias(libs.plugins.mikepenz.aboutlibraries)
}

android {
    namespace = "id.daw.florisboarddaw"
    compileSdk = 34

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }

    defaultConfig {
        applicationId = "id.daw.florisboarddaw"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    bundle {
        language {
            // We disable language split because FlorisBoard does not use
            // runtime Google Play Service APIs and thus cannot dynamically
            // request to download the language resources for a specific locale.
            enableSplit = false
        }
    }

    buildTypes {
        named("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug-"

            isDebuggable = true
            isJniDebuggable = false

            ndk {
                // For running FlorisBoard on the emulator
                // abiFilters += listOf("x86", "x86_64")
            }

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_debug")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_debug_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_debug_foreground")
            resValue("string", "floris_app_name", "FlorisBoard Debug")
        }

        create("beta") {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-alpha04"

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isMinifyEnabled = true
            isShrinkResources = true

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_beta")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_beta_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_beta_foreground")
            resValue("string", "floris_app_name", "FlorisBoard Beta")
        }

        named("release") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isMinifyEnabled = true
            isShrinkResources = true

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_stable")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_stable_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_stable_foreground")
            resValue("string", "floris_app_name", "@string/app_name")
        }

        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")

            ndk {
                // For running FlorisBoard on the emulator
                abiFilters += listOf("x86", "x86_64")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.autofill)
    implementation(libs.androidx.collection.ktx)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.emoji2.views)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.cache4k)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mikepenz.aboutlibraries.core)
    implementation(libs.mikepenz.aboutlibraries.compose)
    implementation(libs.patrickgold.compose.tooltip)
    implementation(libs.patrickgold.jetpref.datastore.model)
    implementation(libs.patrickgold.jetpref.datastore.ui)
    implementation(libs.patrickgold.jetpref.material.ui)

    testImplementation(libs.equalsverifier)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.roboelectric)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner.junit5)

    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}