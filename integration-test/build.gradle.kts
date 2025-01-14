plugins {
  groovy
  id("com.github.ben-manes.versions")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  constraints {
    implementation("de.gesellix:docker-engine") {
      version {
        strictly("[2022-02-01T01-01-01,)")
      }
    }
    implementation("de.gesellix:docker-filesocket") {
      version {
        strictly("[2022-02-01T01-01-01,)")
      }
    }
    implementation("de.gesellix:docker-remote-api-model-1-41") {
      version {
        strictly("[2022-07-25T19-52-00,)")
      }
    }
    implementation("org.slf4j:slf4j-api") {
      version {
        strictly("[1.7,1.8)")
        prefer("1.7.36")
      }
    }
    listOf(
      "com.squareup.okhttp3:mockwebserver",
      "com.squareup.okhttp3:okhttp"
    ).onEach {
      implementation(it) {
        version {
          strictly("[4,4.10)")
          prefer("4.10.0")
        }
      }
    }
    implementation("com.squareup.okio:okio") {
      version {
        strictly("[3,4)")
      }
    }
    implementation("com.squareup.moshi:moshi") {
      version {
        strictly("[1.12.0,2)")
      }
    }
    implementation("com.squareup.moshi:moshi-kotlin") {
      version {
        strictly("[1.12.0,2)")
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
          strictly("[1.5,1.8)")
          prefer("1.6.21")
        }
      }
    }
  }
  implementation(project(":client"))
  testImplementation("org.codehaus.groovy:groovy-json:[3,)")
  testImplementation("com.kohlschutter.junixsocket:junixsocket-core:[2.4,)")
  testImplementation("com.kohlschutter.junixsocket:junixsocket-common:[2.4,)")

  testImplementation("net.jodah:failsafe:2.4.4")
  testImplementation("org.apache.commons:commons-compress:1.21")

  testImplementation("org.slf4j:slf4j-api:[1.7,)")
  runtimeOnly("ch.qos.logback:logback-classic:[1.2,2)!!1.2.11")

  testImplementation("de.gesellix:docker-registry:2022-07-26T14-32-00")
  testImplementation("de.gesellix:testutil:[2020-10-03T10-08-28,)")
  testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
  testRuntimeOnly("cglib:cglib-nodep:3.3.0")
  testImplementation("org.apache.commons:commons-lang3:3.12.0")
  testRuntimeOnly("ch.qos.logback:logback-classic:[1.2,2)!!1.2.11")
}

tasks.withType(Test::class) {
  useJUnitPlatform()
}

tasks.check.get().shouldRunAfter(project(":client").tasks.check)
