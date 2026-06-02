plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.watnapp.buddhawajana"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.watnapp.buddhawajana"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
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
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── New foundation modules ─────────────────────────────────────────────
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":feature:books"))
    implementation(project(":core:player"))
    implementation(project(":feature:audio"))
    implementation(libs.kotlinx.serialization.json)

    // ── Compose BOM + UI ──────────────────────────────────────────────────
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // ── AndroidX ─────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)

    // ── Koin DI ───────────────────────────────────────────────────────────
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.navigation)

    // ── Coil ─────────────────────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── Legacy deps (kept for existing screens until Task 11) ─────────────
    // Compose runtime extras
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.runtime.rxjava2)
    implementation(libs.compose.material)

    // Room (legacy app-level DB still used by legacy repositories)
    implementation(libs.room.runtime)
    implementation(libs.room.rxjava2)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Appcompat / Material / ConstraintLayout (legacy activities)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // Retrofit + RxJava2 (legacy API services)
    implementation(libs.retrofit)
    implementation(libs.retrofit.rxjava2)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.moshi)

    // RxJava2
    implementation(libs.rxjava2)
    implementation(libs.rxandroid)

    // WorkManager + RxJava2 bridge
    implementation(libs.work.runtime)
    implementation(libs.work.runtime.ktx)
    implementation(libs.work.rxjava2)
    implementation(libs.work.gcm)

    // Misc legacy UI libs
    implementation(libs.kprogresshud)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.compose.html)
    implementation(libs.pdf.viewer)
    implementation(libs.androidx.window)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Moshi (legacy moshi variant used in old retrofit setup)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging)

    // ── Test ──────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
}
