plugins {
    id("java")
}

group = "de.dasbabypixel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "derklaroRepoReleases"
        url = uri("https://repository.derklaro.dev/releases")
    }
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "DarkCube"
        credentials(PasswordCredentials::class)
        url = uri("https://nexus.darkcube.eu/repository/darkcube-group/")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    implementation("com.github.juliarn.npc-lib:npc-lib-minestom:3.0.0-beta5")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("eu.cloudnetservice.cloudnet:wrapper-jvm:4.0.0-RC9")
}
