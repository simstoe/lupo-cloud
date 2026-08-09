plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lupo-node"))
    implementation(project(":lupo-api"))
}

tasks.test {
    useJUnitPlatform()
}