plugins {
    java
}

val id = project.property("id") as String
val extensionName = providers.gradleProperty("extensionName").get()
val extensionAuthor = providers.gradleProperty("author").get()
val extensionVersion = version.toString()
val geyserApiVersion = "2.11.3"

repositories {
    // Repo for the Geyser API artifact
    maven("https://repo.opencollab.dev/main/")

    // Add other repositories here
    mavenCentral()
}

dependencies {
    compileOnly("org.geysermc.geyser:api:$geyserApiVersion-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val idRegex = Regex("[a-z][a-z0-9-_]{0,63}")
if (idRegex.matches(id).not()) {
    throw IllegalArgumentException("Invalid extension id $id! Must only contain lowercase letters, " +
            "and cannot start with a number.")
}

val nameRegex = Regex("^[A-Za-z_.-]+$")
if (nameRegex.matches(extensionName).not()) {
    throw IllegalArgumentException("Invalid extension name $extensionName! Must fit regex: ${nameRegex.pattern})")
}

tasks {
    processResources {
        filesMatching("extension.yml") {
            expand(
                "id" to id,
                "name" to extensionName,
                "api" to geyserApiVersion,
                "version" to extensionVersion,
                "author" to extensionAuthor
            )
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        archiveFileName = "Slimefun-Geyser-Extension-$extensionVersion.jar"
    }
}
