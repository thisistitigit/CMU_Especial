    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.google.gms.google.services)
        alias(libs.plugins.kotlin.kapt)
        alias(libs.plugins.hilt.android)
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
            vectorDrawables.useSupportLibrary = true

            javaCompileOptions {
                annotationProcessorOptions {
                    arguments += mapOf("room.schemaLocation" to "$projectDir/schemas".toString())
                }
            }
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

        kotlinOptions {
            jvmTarget = "17"
        }

        buildFeatures {
            buildConfig = true
            compose = true


        }
        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.8"
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }

    dependencies {
        // Core
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.credentials)
        implementation(libs.androidx.credentials.play.services.auth)
        implementation(libs.googleid)
        coreLibraryDesugaring(libs.core.desugar)

        // Compose
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation(libs.androidx.navigation.compose)

        // Room
        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
        kapt(libs.androidx.room.compiler)
        implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
        kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

        // Firebase
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.auth)
        implementation(libs.firebase.database)
        implementation(libs.firebase.firestore.ktx)
        implementation(libs.firebase.storage.ktx)

        // Google Services
        implementation(libs.play.services.location)
        implementation(libs.play.services.maps)
        implementation(libs.places)

        // Networking
        implementation(libs.retrofit.core)
        implementation(libs.retrofit.moshi)
        implementation(libs.moshi.core)
        implementation(libs.moshi.kotlin)
        implementation(libs.okhttp.logging)

        // Hilt
        implementation(libs.hilt.android)
        kapt(libs.hilt.compiler)
        implementation(libs.hilt.navigation.compose)

        // WorkManager
        implementation("androidx.work:work-runtime-ktx:2.8.1")

        // Coil
        implementation("io.coil-kt:coil-compose:2.4.0")

        // CameraX
        implementation("androidx.camera:camera-core:1.3.0")
        implementation("androidx.camera:camera-camera2:1.3.0")
        implementation("androidx.camera:camera-lifecycle:1.3.0")
        implementation("androidx.camera:camera-view:1.3.0")
        implementation("com.google.android.material:material:1.12.0")
        implementation("com.squareup.retrofit2:converter-gson:2.2.0")
        // Maps Compose
        implementation("com.google.maps.android:maps-compose:4.1.1")


        implementation("com.google.android.gms:play-services-auth:21.2.0")
        implementation("androidx.compose.material:material-icons-extended")


        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }

    kapt {
        correctErrorTypes = true
    }