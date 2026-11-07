plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.mikeyphw.mixplorercompanion"
    compileSdk = 37
    defaultConfig {
        applicationId = "io.github.mikeyphw.mixplorercompanion"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-r1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // AGP 9 built-in Kotlin inherits Java targetCompatibility (17).
    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
    testImplementation("junit:junit:4.13.2")
}
