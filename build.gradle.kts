/*
 * This file was generated by the Gradle 'init' task.
 */

import java.io.File

plugins {
    `java-library`
    `maven-publish`
    `signing`
    /*id("jarfile-hash-plugin") version "0.0.1"*/
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

group = "io.github.adamkorcz"
version = "0.1.18"
description = "Adams test java project"
java.sourceCompatibility = JavaVersion.VERSION_1_8

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "test-java-project"
            from(components["java"])
            val base_dir = "build/libs/slsa-attestations/"
            var counter = 0
            File(base_dir).walkTopDown().forEach {
                throw StopExecutionException("Hello")
                var path = it.getName()
                val name = path.replace(project.name + "-" + project.version, "").split(".", limit=2)
                if (name.size != 2) {
                    throw StopExecutionException("Found incorrect file name: " + path)
                }
                var cls = name[0]
                var ext = name[1]
                if (cls.startsWith("-")) {
                    cls = cls.substring(1)
                }
                artifact (base_dir + path) {
                    classifier = cls
                    extension = ext
                }
                counter = counter.inc()
            }
            if (counter == 0) {
                throw StopExecutionException("No files were found in build/libs/slsa-attestations. This is a blocker.")
            }
            pom {
                name.set("test-java-project")
                description.set("Adams test java project")
                url.set("https://github.com/AdamKorcz/test-java-project")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        id.set("adamkrocz")
                        name.set("Adam K")
                        email.set("Adam@adalogics.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/adamkorcz/test-java-project.git")
                    developerConnection.set("scm:git:ssh://github.com:simpligility/test-java-project.git")
                    url.set("http://github.com/adamkorcz/test-java-project/tree/main")
                }
            }
        }
    }
    repositories {
        maven {
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
            name = "test-java-project"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
