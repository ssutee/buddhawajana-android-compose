plugins { id("buddhawajana.android.compose") }
android { namespace = "com.watnapp.buddhawajana.core.designsystem" }
dependencies {
    implementation(libs.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
}
