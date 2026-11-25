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

    packaging {
        resources {
            // Several libraries include this metadata file; keep one copy in the APK.
            pickFirsts += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    implementation("io.github.hap-java:hap:2.0.7") {
        // org.glassfish:javax.json already bundles these API classes.
        exclude(group = "javax.json", module = "javax.json-api")
        // HAP-Java PairVerifyManager imports djb.Curve25519 from org.zeromq.
        exclude(group = "net.vrallev.ecc", module = "ecc-25519-java")
    }
}
