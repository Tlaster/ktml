plugins {
    kotlin("multiplatform") version "1.7.10"
}

group = "moe.tlaster"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    ios()
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
    js {
        browser()
        nodejs()
    }
    
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-client-core:2.1.0")
                implementation("io.ktor:ktor-client-okhttp:2.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
            }
        }
    }
}
