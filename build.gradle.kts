import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    id("java")
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.3.1"
    id("com.willfp.libreforge-gradle-plugin") version "2.0.1"
}

group = "com.exanthiax"
version = findProperty("version")!!
val libreforgeVersion = findProperty("libreforge-version")

base {
    archivesName.set(project.name)
}

dependencies {
    implementation(project(path = ":eco-core:core-plugin", configuration = "shadow"))
}

publishing {
    publications {
        // maven-private: the libreforge jar (shaded plugin with libreforge embedded)
        create<MavenPublication>("private") {
            artifactId = rootProject.name
        }
        // maven-releases (served publicly via the maven-public group): the API jar
        create<MavenPublication>("release") {
            artifactId = rootProject.name
        }
    }
    repositories {
        maven {
            name = "Auxilor"
            url = uri("https://repo.auxilor.io/repository/maven-private/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
        maven {
            name = "AuxilorReleases"
            url = uri("https://repo.auxilor.io/repository/maven-releases/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

// The API spans core-api and core-plugin, so the published jar merges both. It is
// the plain output, not shadowJar: shadowJar drops META-INF (taking the
// .kotlin_module with it, which hides every top-level declaration from the Kotlin
// compiler) and relocates kotlin.* into com.willfp.eco.libs.kotlin, which rewrites
// @kotlin.Metadata and makes the whole API read as Java.
val apiJar by tasks.registering(Jar::class) {
    archiveClassifier.set("api")

    listOf(":eco-core:core-api", ":eco-core:core-plugin").forEach { path ->
        from(project(path).tasks.named<Jar>("jar").map { zipTree(it.archiveFile) })
    }
}

// Neither publication is attached to a software component, so only the single jar
// and its pom are published - no sources, javadoc, or classified variants.
afterEvaluate {
    publishing.publications.named<MavenPublication>("private") {
        artifact(tasks.named("libreforgeJar"))
    }
    publishing.publications.named<MavenPublication>("release") {
        artifact(apiJar) {
            classifier = ""
        }
    }
}

tasks.register("publishToAuxilor") {
    dependsOn(
        "publishPrivatePublicationToAuxilorRepository",
        "publishReleasePublicationToAuxilorReleasesRepository",
    )
}

tasks.register<Copy>("dist") {
    dependsOn("libreforgeJar")
    from(tasks.named("libreforgeJar").map { it.outputs.files })
    into(layout.projectDirectory.dir("dist"))
    rename { "${rootProject.name}-${project.version}.jar" }
}

tasks.named("build") {
    finalizedBy("dist")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.auxilor.io/repository/maven-public/")
        maven("https://repo.codemc.org/repository/nms/")
        maven("https://jitpack.io")
    }

    dependencies {
        // eco/libreforge are declared per-module, NOT here: the published
        // core-api module must not see them.
        compileOnly("org.jetbrains:annotations:26.0.2")
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    }

    tasks {
        shadowJar {
            exclude("META-INF/**")
            relocate("com.willfp.libreforge.loader", "com.exanthiax.ecominions.libreforge.loader")
            relocate("kotlin", "com.willfp.eco.libs.kotlin")
            relocate("kotlin.jvm", "com.willfp.eco.libs.kotlin.jvm")
            relocate("kotlin.coroutines", "com.willfp.eco.libs.kotlin.coroutines")
            relocate("kotlin.reflect", "com.willfp.eco.libs.kotlin.reflect")
        }

        compileKotlin {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_21
            }
        }

        compileTestKotlin {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_21
            }
        }

        compileJava {
            options.isDeprecation = true
            options.encoding = "UTF-8"
        }

        processResources {
            filesMatching(listOf("**plugin.yml", "**eco.yml")) {
                expand(
                    "version" to project.version,
                    "libreforgeVersion" to libreforgeVersion!!,
                    "pluginName" to rootProject.name
                )
            }
        }

        build {
            dependsOn(shadowJar)
        }

        withType<JavaCompile>().configureEach {
            options.release = 21
        }
    }

    java {
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
}
