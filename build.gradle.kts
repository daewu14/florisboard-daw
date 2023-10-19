// Top-level build file where you can add configuration options common to all sub-projects/modules.
@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.agp.application) apply false
    alias(libs.plugins.agp.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.mannodermaus.android.junit5) apply false
    alias(libs.plugins.mikepenz.aboutlibraries) apply false
}