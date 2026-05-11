import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

android {
    namespace = "com.babymakisuk.coredrive"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // TODO Phase F: 蜉蜈･ google-api-client-android, drive
}
