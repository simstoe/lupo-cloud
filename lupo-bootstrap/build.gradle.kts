plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lupo-node"))
    implementation(project(":lupo-api"))
    implementation(project(":lupo-web-api"))
}

tasks.test {
    useJUnitPlatform()
}