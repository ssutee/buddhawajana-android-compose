// Extra properties for legacy app/build.gradle (Groovy) that still use ext vars.
// These will be removed when app/build.gradle is converted to Kotlin DSL in Task 10.
extra["compose_version"] = "1.3.3"
extra["retrofit_version"] = "2.9.0"
extra["koin_android_compose_version"] = "3.4.2"
extra["koin_android_version"] = "3.3.3"
extra["room_version"] = "2.4.0"
extra["work_version"] = "2.7.1"

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
