import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.net.URI

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
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    api(platform(libs.kotlinx.serialization.bom))
    api(libs.kotlinx.serialization.json)
    implementation(libs.jsonpathkt)
    implementation(libs.json.kotlin.schema)
    testImplementation(libs.kotlin.test)
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
dokka {
    // used as project name in the header
    moduleName = "Presentation Exchange"

    dokkaSourceSets.main {
        // contains descriptions for the module and the packages
        includes.from("Module.md")

        documentedVisibilities = setOf(VisibilityModifier.Public, VisibilityModifier.Protected)

        val remoteSourceUrl = System.getenv()["GIT_REF_NAME"]?.let { URI.create("${Meta.BASE_URL}/tree/$it/src") }
        remoteSourceUrl
            ?.let {
                sourceLink {
                    localDirectory = projectDir.resolve("src")
                    remoteUrl = it
                    remoteLineSuffix = "#L"
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
    configure(KotlinJvm(javadocJar = JavadocJar.Dokka(tasks.dokkaGeneratePublicationHtml), sourcesJar = true))

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
