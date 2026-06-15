plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.complex.terminal"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.complex.terminal"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0"
    buildConfigField("String", "COMPLEX_API_BASE_URL", "\"${System.getenv("COMPLEX_ANDROID_API_BASE_URL") ?: "https://192.168.88.160"}\"")
  }

  buildFeatures {
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}
