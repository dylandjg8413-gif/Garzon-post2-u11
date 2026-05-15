// Top-level build file - declara los plugins disponibles para todos los modulos.
// Las versiones se centralizan en gradle/libs.versions.toml (Version Catalog).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
}
