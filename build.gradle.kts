plugins {
    java
}

group = "dev.zeropng"
version = providers.fileContents(layout.projectDirectory.file("version.txt")).asText.get().trim()
val pluginVersion = version.toString()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.84-stable")
    compileOnly("me.clip:placeholderapi:2.12.2")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.2")
    testImplementation("io.papermc.paper:paper-api:26.2.build.84-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("pluginVersion", pluginVersion)
}

tasks.jar {
    archiveBaseName.set("EssentialsCore")
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the project version used for release automation."
    doLast {
        println(pluginVersion)
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
