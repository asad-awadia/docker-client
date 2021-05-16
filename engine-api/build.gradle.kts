plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.kapt")
  id("com.github.ben-manes.versions")
}

version = "1.41"

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.10")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
  implementation("com.squareup.moshi:moshi:1.12.0")
//  implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
  kapt("com.squareup.moshi:moshi-kotlin-codegen:1.12.0")
  implementation("com.squareup.okhttp3:okhttp:4.9.1")
  testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
  implementation("de.gesellix:docker-api-model:2021-07-21T22-22-21")
  implementation("de.gesellix:docker-engine:2021-07-21T22-22-21")
  implementation("de.gesellix:docker-filesocket:2021-06-06T17-29-35")

  implementation("org.slf4j:slf4j-api:[1.7,)")
  testImplementation("ch.qos.logback:logback-classic:1.2.3")

//  implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
//  implementation("org.apache.commons:commons-lang3:3.10")
//  implementation("javax.annotation:javax.annotation-api:1.3.2")
//  testImplementation("junit:junit:4.13.1")
}

tasks.withType(Test::class.java) {
  useJUnitPlatform()
}

//tasks.javadoc {
//  options.tags = ["http.response.details:a:Http Response Details"]
//}
