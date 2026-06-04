plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "me.m0dii"
version = "2.0.0"

tasks.shadowJar {
    relocate("org.bstats", "me.m0dii.shopguipluseditor")
    minimize()

    archiveFileName = "ShopGUIPlusEditor-${version}.jar"
}

repositories {
    mavenLocal()
    mavenCentral()

    flatDir {
        dirs("libs")
    }

    listOf(
        "https://jitpack.io",
        "https://maven.enginehub.org/repo/",
        "https://repo.papermc.io/repository/maven-public/",
        "https://ci.ender.zone/plugin/repository/everything/",
    ).forEach { repoUrl ->
        maven { url = uri(repoUrl) }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    compileOnly("com.github.brcdev-minecraft:shopgui-api:3.0.0")
    compileOnly(files("libs/ShopGUIPlus-1.113.0.jar"))

    implementation("org.bstats:bstats-bukkit:2.2.1")
}

val doFirstEula: Task.() -> Unit = {
    val eulaFile = file("run/latest/eula.txt")
    eulaFile.parentFile.mkdirs()
    if (!eulaFile.exists()) {
        eulaFile.writeText("eula=true")
    }
}

tasks {
    runServer {
        runDirectory(file("run/latest"))
        minecraftVersion("1.21.11")

        doFirst(doFirstEula)
    }
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}