plugins {
    id("buddhawajana.android.compose")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.watnapp.buddhawajana.feature.books" }
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
}
