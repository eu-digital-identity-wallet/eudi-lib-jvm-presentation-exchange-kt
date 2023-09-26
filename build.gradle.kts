object Meta {
    const val ORG_URL = "https://github.com/eu-digital-identity-wallet"
    const val PROJ_DESCR = "Implementation of Presentation Exchange v2"
    const val PROJ_BASE_DIR = "https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt"
    const val PROJ_GIT_URL =
        "scm:git:git@github.com:eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt.git"
    const val PRJ_SSH_URL =
        "scm:git:ssh://github.com:eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt.git"
}

plugins {
    id("org.owasp.dependencycheck") version "8.4.0"
    id("org.sonarqube") version "4.3.1.3277"
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("com.diffplug.spotless") version "6.20.0"
    `java-library`
    `maven-publish`
    signing
    jacoco
}

java.sourceCompatibility = JavaVersion.VERSION_17

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.nfeld.jsonpathkt:jsonpathkt:2.0.1")
    implementation("net.pwall.json:json-kotlin-schema:0.41")
    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
    withJavadocJar()
}
kotlin {
    jvmToolchain(17)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            ),
        )
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}

val ktlintVersion = "0.50.0"
spotless {
    kotlin {
        ktlint(ktlintVersion)
        licenseHeaderFile("FileHeader.txt")
    }
    kotlinGradle {
        ktlint(ktlintVersion)
    }
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            pom {
                name.set(project.name)
                description.set(Meta.PROJ_DESCR)
                url.set(Meta.PROJ_BASE_DIR)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set(Meta.PROJ_GIT_URL)
                    developerConnection.set(Meta.PRJ_SSH_URL)
                    url.set(Meta.PROJ_BASE_DIR)
                }
                issueManagement {
                    system.set("github")
                    url.set(Meta.PROJ_BASE_DIR + "/issues")
                }
                ciManagement {
                    system.set("github")
                    url.set(Meta.PROJ_BASE_DIR + "/actions")
                }
                developers {
                    organization {
                        url.set(Meta.ORG_URL)
                    }
                }
            }
        }
    }
    repositories {

        val sonaUri =
            if ((extra["isReleaseVersion"]) as Boolean) {
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            } else {
                "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            }

        maven {
            name = "sonatype"
            url = uri(sonaUri)
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
    })
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["library"])
}
