// build.gradle.kts (raiz do projeto)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.dokka) apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false

}

