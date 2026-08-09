plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
    id("com.google.protobuf") version "0.9.4" apply false
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
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

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer::class.java) {
        resource = "META-INF/spring.factories"
    }
}

tasks.test {
    useJUnitPlatform()
}