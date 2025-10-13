import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.net.URL

object Meta {
    const val BASE_URL = "https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-presentation-exchange-kt"
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dependencycheck)
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.jsonpathkt)
    implementation(libs.json.kotlin.schema)
    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.DEFAULT
        languageVersion = KotlinVersion.DEFAULT
    }

    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
        vendor = JvmVendorSpec.ADOPTIUM
        implementation = JvmImplementation.VENDOR_SPECIFIC
    }
}

tasks.test {
    useJUnitPlatform()
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

dependencyCheck {
    formats = listOf("XML", "HTML")

    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: properties["nvdApiKey"]?.toString()
    }
}
