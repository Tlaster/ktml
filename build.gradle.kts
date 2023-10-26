import org.jetbrains.kotlin.konan.target.HostManager
import java.util.Properties
import com.vanniktech.maven.publish.SonatypeHost

plugins {
  kotlin("multiplatform") version "1.9.0"
  id("com.vanniktech.maven.publish") version "0.25.3"
}

group = "moe.tlaster"
version = "0.0.5"

repositories {
  mavenCentral()
}

kotlin {
  targetHierarchy.default()
  jvm {
    jvmToolchain(11)
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
  ios()
  iosSimulatorArm64()
  macosX64()
  macosArm64()
  watchos()
  watchosSimulatorArm64()
  tvos()
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
  pom {
    name.set("ktml")
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
