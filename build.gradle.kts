// build.gradle.kts (raiz do projeto)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.hilt.android) apply false
}

