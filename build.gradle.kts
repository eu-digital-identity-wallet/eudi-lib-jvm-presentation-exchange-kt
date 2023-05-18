plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    `java-library`
    `maven-publish`
}

group = "eu.europa.ec.euidw"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    mavenLocal()
}


dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.nfeld.jsonpathkt:jsonpathkt:2.0.1")
    implementation("net.pwall.json:json-kotlin-schema:0.39")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}


tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}





publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
    val publishMvnRepo = System.getenv("PUBLISH_MVN_REPO")?.let { uri(it) }
    if (null != publishMvnRepo)
        repositories {

            maven {
                name = "EudiwPackages"
                url = uri(publishMvnRepo)
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }

            }
    } else println("Warning: PUBLISH_MVN_REPO undefined. Won't publish")
}







