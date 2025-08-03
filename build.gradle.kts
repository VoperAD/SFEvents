import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = "io.github.voperad"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    fun DependencyHandlerScope.libraryAndTest(dependency: Any) {
        library(dependency)
        testImplementation(dependency)
    }

    library(kotlin("stdlib"))
    libraryAndTest(kotlin("reflect"))


    compileOnly("org.spigotmc:spigot-api:1.16.1-R0.1-SNAPSHOT")
    compileOnly("com.github.Slimefun:Slimefun4:RC-37")
    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("io.github.seggan:sf4k:0.4.1")
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("org.apache.maven:maven-artifact:4.0.0-rc-4")
    libraryAndTest("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

bukkit {
    name = "SFEvents"
    version = "0.1.0"
    author = "VoperAD"
    main = "io.github.voperad.sfevents.SFEvents"
    apiVersion = "1.13"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    depend = listOf("Slimefun", "PlaceholderAPI")
    softDepend = listOf("SimpleClans")
    prefix = "SF-Events"
}

tasks.compileKotlin {
    compilerOptions.javaParameters = true
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(sourceSets.main.get().output)
}

tasks.shadowJar {
    dependsOn(tasks.test)

    val basePackage = "io.github.voperad"
    relocate("co.aikar.commands", "$basePackage.acf")
    relocate("co.aikar.locales", "$basePackage.locales")
    relocate("org.bstats", "$basePackage.bstats")

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-common"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
    }

    archiveClassifier = ""
    mergeServiceFiles()
    val envDestDir = System.getenv("DESTINATION_DIR")
    if (envDestDir != null) {
        val outputDir = file(envDestDir)
        destinationDirectory.set(outputDir)

        doFirst {
            outputDir.listFiles { _, name ->
                name.startsWith(archiveBaseName.get()) && name.endsWith(archiveExtension.get())
            }?.forEach {
                println("Deleting ${it.name}")
                it.delete()
            }
        }
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.runServer {
    downloadPlugins {
        url("https://blob.build/dl/Slimefun4/Dev/1149")
        url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.21.0-dev+111-b54c8c1.jar")
        url("https://www.spigotmc.org/resources/placeholderapi.6245/download?version=541946")
    }
    maxHeapSize = "2G"
    minecraftVersion("1.20.1")
}




