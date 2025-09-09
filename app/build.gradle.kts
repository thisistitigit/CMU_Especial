plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.gms)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.dokka)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}
tasks.dokkaHtml.configure {
    // Onde o HTML é gerado dentro de app/
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

    dokkaSourceSets.configureEach {
        moduleName.set("ReviewApp")
        includes.from("RELATORIO.md")

        // Preferências úteis
        jdkVersion.set(17)
        skipDeprecated.set(false)
        reportUndocumented.set(false)

        // (Opcional) esconder pacotes internos da doc
        perPackageOption {
            matchingRegex.set("com\\.example\\.reviewapp\\.di(.*)")
            suppress.set(true)
        }
    }
}


android {
    namespace = "com.example.reviewapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.reviewapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas".toString())
            }
        }

        manifestPlaceholders += mapOf(
            "MAPS_API_KEY" to (project.findProperty("MAPS_API_KEY") ?: "")
        )
        // >>> Estes três geram MESMO campos no BuildConfig <<<
        buildConfigField(
            "String",
            "PLACES_API_KEY",
            "\"${providers.gradleProperty("PLACES_API_KEY").getOrElse("")}\""
        )
        buildConfigField(
            "String",
            "GEOAPIFY_KEY",
            "\"${providers.gradleProperty("GEOAPIFY_KEY").getOrElse("")}\""
        )
        // Opcional: também o MAPS se quiseres ler em código
        buildConfigField(
            "String",
            "MAPS_API_KEY",
            "\"${providers.gradleProperty("MAPS_API_KEY").getOrElse("")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.hilt.common)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    coreLibraryDesugaring(libs.core.desugar)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)

    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.places)
    implementation(libs.play.services.auth)

    implementation(libs.androidx.media3.common)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.core)
    implementation(libs.moshi.kotlin)
    kapt(libs.moshi.kotlin.codegen)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.material)
    implementation(libs.maps.compose)
    implementation(libs.retrofit.gson)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("androidx.core:core-splashscreen:1.0.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

}

