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
        versionCode = 2
        versionName = "0.2.1-alpha"
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
            // Netty modules each ship their own version metadata.
            pickFirsts += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    // HAP-Java needs EdDSA classes during HomeKit pairing verification.
    implementation("net.i2p.crypto:eddsa:0.3.0")
    implementation("io.github.hap-java:hap:2.0.7") {
        // org.glassfish:javax.json already bundles these API classes.
        exclude(group = "javax.json", module = "javax.json-api")
        // HAP-Java PairVerifyManager imports djb.Curve25519 from org.zeromq.
        exclude(group = "net.vrallev.ecc", module = "ecc-25519-java")
    }
}
