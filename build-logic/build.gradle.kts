plugins {
    `kotlin-dsl`
}
dependencies {
    compileOnly(libs.plugins.android.library.toDep())
    compileOnly(libs.plugins.kotlin.android.toDep())
    compileOnly(libs.plugins.kotlin.compose.toDep())
    compileOnly(libs.plugins.kotlin.jvm.toDep())
}
fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "buddhawajana.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "buddhawajana.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "buddhawajana.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
