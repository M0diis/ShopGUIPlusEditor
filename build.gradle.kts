plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "me.m0dii"
version = "2.1.0"

tasks.shadowJar {
    relocate("org.bstats", "me.m0dii.shopguipluseditor")
    minimize()

    archiveFileName = "ShopGUIPlusEditor-${version}.jar"
}

repositories {
    mavenCentral()

    flatDir {
        dirs("libs")
    }

    listOf(
        "https://jitpack.io",
        "https://repo.papermc.io/repository/maven-public/",
    ).forEach { repoUrl ->
        maven { url = uri(repoUrl) }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")

    compileOnly("com.github.brcdev-minecraft:shopgui-api:3.2.0") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
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
        minecraftVersion("26.2")
        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
        }

        downloadPlugins {
            modrinth("essentialsx", "2.21.2")
            modrinth("vaultunlocked", "2.9.0")

            pluginJars(
                file("libs/ShopGUIPlus-1.113.0.jar")
            )
        }

        doFirst(doFirstEula)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
