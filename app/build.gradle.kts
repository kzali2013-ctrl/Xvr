plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.xvrviewer"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.xvrviewer"
        minSdk = 19
        multiDexEnabled = true
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.7")
    implementation("com.google.android.exoplayer:exoplayer-rtsp:2.18.7")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.18.7")
}
