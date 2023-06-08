group = "io.github.adamkrocz"
version = "0.0.1"

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("jarfileHashPlugin") {
            id = "jarfile-hash-plugin"
            implementationClass = "io.github.adamkrocz.JarfileHashPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("./repo")
        }
    }
}
