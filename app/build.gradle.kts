plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.sonyhomekittvbridge"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.sonyhomekittvbridge"
        minSdk = 21
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0-alpha"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("io.github.hap-java:hap:2.0.7")
}
