// :core:domain - Modulo JVM puro.
// Define contratos (interfaces + modelos). No depende de Android para que sea
// facilmente testeable y reusable desde cualquier feature.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
