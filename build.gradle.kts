import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import java.net.URL

object Meta {
    const val BASE_URL = "https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt"
}

plugins {
    base
    `java-library`
    jacoco
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.dependencycheck)
    alias(libs.plugins.maven.publish)
}

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
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
        vendor = JvmVendorSpec.ADOPTIUM
    }
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_0
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

            val remoteSourceUrl = System.getenv()["GIT_REF_NAME"]?.let { URL("${Meta.BASE_URL}/tree/$it/src") }
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

mavenPublishing {
    pom {
        ciManagement {
            system = "github"
            url = "${Meta.BASE_URL}/actions"
        }
    }
}

val nvdApiKey: String? = System.getenv("NVD_API_KEY") ?: properties["nvdApiKey"]?.toString()
val dependencyCheckExtension = extensions.findByType(DependencyCheckExtension::class.java)
dependencyCheckExtension?.apply {
    formats = mutableListOf("XML", "HTML")
    nvd.apiKey = nvdApiKey ?: ""
}
