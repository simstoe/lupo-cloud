plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
}

allprojects {
    group = "dev.simstoe"
    version = "2026-08-02.1"

    repositories {
        mavenCentral()
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("all")

    subprojects.forEach { subproject ->
        from(subproject.sourceSets.getByName("main").output)
    }

    configurations = subprojects.map { it.configurations.runtimeClasspath.get() }

    manifest {
        attributes(
            "Main-Class" to "dev.simstoe.lupocloud.bootstrap.CloudBootstrap"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}