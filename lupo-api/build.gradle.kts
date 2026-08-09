plugins {
    id("java-library")
    id("com.google.protobuf")
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.73.0"
val protobufVersion = "4.29.3"

dependencies {
    implementation("org.jline:jline:4.0.0");
    implementation("org.jline:jline-console:4.0.0")
    implementation("org.jline:jline-terminal-jansi:3.30.15")

    implementation("org.apache.logging.log4j:log4j-api:2.26.1")
    implementation("org.apache.logging.log4j:log4j-core:2.26.1")

    api("io.grpc:grpc-stub:$grpcVersion")
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    api("com.google.code.gson:gson:2.11.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}