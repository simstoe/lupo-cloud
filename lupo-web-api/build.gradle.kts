plugins {
    id("java-library")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

val springBootVersion = "3.5.3"

dependencies {
    implementation(project(":lupo-api"))
    implementation(project(":lupo-node"))


    api("org.springframework.boot:spring-boot-starter-web:$springBootVersion") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    implementation("org.springframework.boot:spring-boot-starter-log4j2:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-websocket:$springBootVersion") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

springBoot {
    mainClass.set("dev.simstoe.lupocloud.web.WebApiApplication")
}

tasks.test {
    useJUnitPlatform()
}
