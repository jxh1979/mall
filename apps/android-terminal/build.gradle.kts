plugins {
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.serialization") version "2.0.21"
  application
}

group = "com.complex"
version = "0.1.0"

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
  testImplementation(kotlin("test"))
}

kotlin {
  jvmToolchain(21)
}

application {
  mainClass.set("com.complex.terminal.P0TerminalSmokeKt")
}

tasks.test {
  useJUnitPlatform()
}
