plugins {
    id("buddhawajana.android.compose")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.watnapp.buddhawajana.core.ui" }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.material3.adaptive.navigation.suite)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.material.icons.extended)
}
