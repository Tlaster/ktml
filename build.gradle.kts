import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  kotlin("multiplatform") version "2.0.0"
  id("com.vanniktech.maven.publish") version "0.25.3"
}


val libName = "ktml"
val libGroup = "moe.tlaster"
val libVersion = "0.0.7"

group = libGroup
version = libVersion


repositories {
  mavenCentral()
}

kotlin {
  applyDefaultHierarchyTemplate()
  jvm {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_1_8)
    }
    testRuns.named("test") {
      executionTask.configure {
        useJUnitPlatform()
      }
    }
  }
  js {
    browser()
    nodejs()
  }
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  macosX64()
  macosArm64()
  watchosX64()
  watchosArm64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosX64()
  tvosSimulatorArm64()
  mingwX64()

  linuxX64()
  linuxArm64()


  sourceSets {
    val commonMain by getting
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.S01)
  signAllPublications()
  coordinates(
    groupId = libGroup,
    artifactId = libName,
    version = libVersion,
  )
  pom {
    name.set(libName)
    description.set("Html parser for Kotlin Multiplatform")
    url.set("https://github.com/Tlaster/ktml")
    licenses {
      license {
        name.set("MIT")
        url.set("https://opensource.org/licenses/MIT")
      }
    }
    developers {
      developer {
        id.set("Tlaster")
        name.set("James Tlaster")
        email.set("tlaster@outlook.com")
      }
    }
    scm {
      url.set("https://github.com/Tlaster/ktml")
      connection.set("scm:git:git://github.com/Tlaster/ktml.git")
      developerConnection.set("scm:git:git://github.com/Tlaster/ktml.git")
    }
  }
}
