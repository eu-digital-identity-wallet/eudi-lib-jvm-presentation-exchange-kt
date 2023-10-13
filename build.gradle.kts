import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL
import kotlin.jvm.optionals.getOrNull

plugins {
    base
    `java-library`
    `maven-publish`
    signing
    jacoco
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.dependencycheck)
}

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.jsonpathkt)
    implementation(libs.json.kotlin.schema)
    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
    val javaVersion = libs.versions.java.get()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
}
kotlin {
    jvmToolchain {
        val javaVersion = libs.versions.java.get()
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
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

//
// Redefine javadocJar in terms of Dokka
//
val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

//
// Configuration of Dokka engine
//
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            // used as project name in the header
            moduleName.set("Presentation Exchange")

            // contains descriptions for the module and the packages
            includes.from("Module.md")

            documentedVisibilities.set(setOf(Visibility.PUBLIC, Visibility.PROTECTED))

            val remoteSourceUrl = System.getenv()["GIT_REF_NAME"]?.let { URL("${Meta.PROJ_BASE_DIR}/tree/$it/src") }
            remoteSourceUrl
                ?.let {
                    sourceLink {
                        localDirectory.set(projectDir.resolve("src"))
                        remoteUrl.set(it)
                        remoteLineSuffix.set("#L")
                    }
                }
        }
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        ktlint(ktlintVersion)
        licenseHeaderFile("FileHeader.txt")
    }
    kotlinGradle {
        ktlint(ktlintVersion)
    }
}

object Meta {
    const val ORG_URL = "https://github.com/eu-digital-identity-wallet"
    const val PROJ_DESCR = "Implementation of Presentation Exchange v2"
    const val PROJ_BASE_DIR = "https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt"
    const val PROJ_GIT_URL =
        "scm:git:git@github.com:eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt.git"
    const val PRJ_SSH_URL =
        "scm:git:ssh://github.com:eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt.git"
}
publishing {

    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifacts + artifact(javadocJar)
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
