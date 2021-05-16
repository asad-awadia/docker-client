plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.kapt")
  id("com.github.ben-manes.versions")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
  mavenCentral()
}

dependencies {
  constraints {
    implementation("org.slf4j:slf4j-api") {
      version {
        strictly("[1.7,1.8)")
        prefer("1.7.30")
      }
    }
    implementation("com.squareup.okio:okio") {
      version {
        strictly("[2.5,3)")
        prefer("2.10.0")
      }
    }
    listOf(
      "org.jetbrains.kotlin:kotlin-reflect",
      "org.jetbrains.kotlin:kotlin-stdlib",
      "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
      "org.jetbrains.kotlin:kotlin-stdlib-common",
      "org.jetbrains.kotlin:kotlin-test"
    ).onEach {
      implementation(it) {
        version {
          strictly("[1.3,1.5)")
          prefer("1.3.72")
        }
      }
    }
    listOf(
      "org.codehaus.groovy:groovy",
      "org.codehaus.groovy:groovy-json"
    ).onEach {
      implementation(it) {
        version {
          strictly("[2.5,)")
          prefer("2.5.13")
        }
      }
    }
  }
  implementation(project(":client"))
  implementation(project(":engine-api"))
  implementation("de.gesellix:docker-engine:2021-07-21T22-22-21")
  implementation("de.gesellix:docker-api-model:2021-07-21T22-22-21")
  implementation("com.squareup.moshi:moshi:1.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
  testImplementation("org.apache.commons:commons-compress:1.20")
  testImplementation("de.gesellix:testutil:[2020-10-03T10-08-28,)")

  implementation("org.slf4j:slf4j-api")
  runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

  testRuntimeOnly("cglib:cglib-nodep:3.3.0")
  testRuntimeOnly("ch.qos.logback:logback-classic:1.2.3")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
  testImplementation("org.junit.platform:junit-platform-launcher:1.7.2")
  testImplementation("org.junit.platform:junit-platform-commons:1.7.2")
}

tasks.withType(Test::class.java) {
  useJUnitPlatform()
}
