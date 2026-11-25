plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zirakusa.notas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zirakusa.notas"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.7.0"
        // ML Kit's on-device text recognition is native code: only 64-bit ARM phones (all recent ones), like Luma.
        ndk { abiFilters += "arm64-v8a" }
        // Modo MCP: the same public relay as Stone, Luma and Calculadora (one device id per app). Empty = no MCP.
        val relayUrl = (project.findProperty("notasRelayUrl") as String?).orEmpty().trimEnd('/')
        buildConfigField("String", "RELAY_URL", "\"$relayUrl\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signed like Stone, Luma, Calculadora and Gateway Hub (the hub recognises the same developer).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // Phone app built for 64-bit ARM only (see abiFilters), like Luma.
    lint { disable += "ChromeOsAbiSupport" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Screenshots of the screens land in app/build/screenshots .
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    // «Bloqueado» asks for the phone's lock, like Luma's vault.
    implementation("androidx.biometric:biometric:1.1.0")
    // Same coroutines as Stone, Luma and Calculadora (the drawer opens with them).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // Photos in the notes (same image library as Luma).
    implementation("io.coil-kt:coil-compose:2.7.0")
    // «Reconocimiento de texto»: ML Kit inside the phone, the same as Luma.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    // Turns camera photos upright (EXIF), also on older phones.
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    // Modo MCP: WebSocket to the relay (same library and version as Stone, Luma and Calculadora).
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
