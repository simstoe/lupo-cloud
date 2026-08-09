plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jline:jline:4.0.0");
    implementation("org.jline:jline-console:4.0.0")
    implementation("org.jline:jline-terminal-jansi:3.30.15")

    implementation("org.apache.logging.log4j:log4j-api:2.26.1")
    implementation("org.apache.logging.log4j:log4j-core:2.26.1")

    api("io.netty:netty-all:4.1.114.Final")
    api("com.google.code.gson:gson:2.11.0")
}

tasks.test {
    useJUnitPlatform()
}