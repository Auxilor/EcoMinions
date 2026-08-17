group = "com.exanthiax"
version = rootProject.version

val ecoVersion = rootProject.findProperty("eco-version")

dependencies {
    implementation(project(":eco-core:core-api"))

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.willfp:eco:$ecoVersion")
    compileOnly("com.willfp:EcoShop:2026.33")

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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
