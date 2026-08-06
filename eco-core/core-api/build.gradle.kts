group = "com.exanthiax"
version = rootProject.version

base {
    archivesName.set("ecominions-api")
}

// The published API artifact. Paper is the ONLY dependency permitted here:
// no eco, no libreforge, no implementation types. Consumers compile against
// this module without owning the plugin jar.
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
