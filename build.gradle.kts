plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    `java-library`
    `maven-publish`
}

group = "eu.europa.ec.euidw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

val kotlinxSerializationVersion = "1.5.0"
val jsonpathktVersion = "2.0.1"


dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("com.nfeld.jsonpathkt:jsonpathkt:$jsonpathktVersion")
    implementation("net.pwall.json:json-kotlin-schema:0.39")
    testImplementation(kotlin("test"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    test {
        useJUnitPlatform()
    }
}



kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}






