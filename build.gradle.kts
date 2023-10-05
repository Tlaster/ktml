plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "moe.tlaster"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    macosX64()
    macosArm64()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    linuxArm64()
    linuxX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()


    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-client-core:2.3.4")
                implementation("io.ktor:ktor-client-okhttp:2.3.4")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}
