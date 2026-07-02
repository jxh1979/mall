plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization")
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
    buildConfigField("String", "COMPLEX_API_ALLOWED_HOSTS", "\"${System.getenv("COMPLEX_ANDROID_API_ALLOWED_HOSTS") ?: "192.168.88.160,localhost,127.0.0.1,::1"}\"")
  }

  buildFeatures {
    buildConfig = true
  }

  // 直接复用已实现的端侧业务层（apps/android-terminal/src/main），不重复造：
  //   TerminalWorkflow / TerminalModels / InMemoryLocalStore（含 nowIso）。
  // 排除三个文件：
  //   - TerminalApiClient.kt：JVM 版用 java.net.http.HttpClient，Android SDK 不含该包；
  //     由 android-app 内的 TerminalApiClient（HttpURLConnection 实现，同包同签名）替代。
  //   - P0TerminalSmoke.kt / P0Fixture.kt：JVM 冒烟/测试夹具，依赖 System.getenv 等，
  //     非现场 UI 所需。
  sourceSets {
    getByName("main") {
      kotlin.srcDir("../../src/main/kotlin")
    }
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

// 排除 JVM 专用源文件（详见上方 sourceSets 注释）：
//   - TerminalApiClient.kt（java.net.http，Android 不可用，已由本工程同包实现替代）
//   - P0TerminalSmoke.kt / P0Fixture.kt（JVM 冒烟/夹具）
// 用绝对路径精确排除「共享 JVM 源目录」中的这三个文件，避免误伤本工程
// app/src 下同名的 Android 版 TerminalApiClient.kt。
val sharedJvmSrc = file("../../src/main/kotlin").canonicalPath
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  exclude { it.file.canonicalPath.startsWith(sharedJvmSrc) && it.name in setOf(
    "TerminalApiClient.kt",
    "P0TerminalSmoke.kt",
    "P0Fixture.kt",
  ) }
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
  // 4.2 扫码相机：ZXing Embedded（本地解码，无云依赖；平板/工作手机摄像头即用）
  implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
