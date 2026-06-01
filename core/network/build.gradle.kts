plugins {
    id("buddhawajana.android.library")
}
android { namespace = "com.watnapp.buddhawajana.core.network" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.android)
}
