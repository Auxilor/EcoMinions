group = "com.exanthiax"
version = rootProject.version

val ecoVersion = rootProject.findProperty("eco-version")

repositories {
    exclusiveContent {
        forRepository {
            maven("https://repo.codemc.org/repository/maven-public/") {
                metadataSources { artifact() }
            }
        }
        filter { includeModule("world.bentobox", "bentobox") }
    }
    exclusiveContent {
        forRepository {
            maven("https://nexus.iridiumdevelopment.net/repository/maven-releases/") {
                metadataSources { artifact() }
            }
        }
        filter { includeGroup("com.iridium") }
    }
}

dependencies {
    implementation(project(":eco-core:core-api"))

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.willfp:eco:$ecoVersion")
    compileOnly("com.willfp:EcoShop:2026.41.1")
    compileOnly("com.willfp:EcoItems:2026.43")
    compileOnly("com.bgsoftware:SuperiorSkyblockAPI:2026.3")
    compileOnly("world.bentobox:bentobox:2.7.0")
    compileOnly("com.iridium:IridiumSkyblock:4.1.5")
    compileOnly("com.github.angeschossen:LandsAPI:7.25.4") { isTransitive = false }

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.willfp:eco:$ecoVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.13")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

// The libreforge-gradle-plugin injects libreforge/libreforge-loader as
// compileOnly for main; mirror that onto the test classpath so tests can
// reference libreforge types (e.g. Dispatcher) directly.
configurations.testImplementation.get().extendsFrom(configurations.compileOnly.get())

// libreforge's published metadata declares a runtime dependency on
// libreforge.core:common, which was never published anywhere. It only bites at
// test runtime, and the shadow jar carries those classes itself, so drop it.
configurations.testRuntimeClasspath.get().exclude(group = "libreforge.core")

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1g"
    testLogging {
        events("passed", "skipped", "failed")
    }
}
