plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
}

group = "eu.europa.ec.euidw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val kotlinxSerializationVersion = "1.5.0"
val jsonpathktVersion = "2.0.1"


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("com.nfeld.jsonpathkt:jsonpathkt:$jsonpathktVersion")
    implementation("net.pwall.json:json-kotlin-schema:0.39")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}




