plugins {
    id("java")
}

dependencies {
    implementation(project(":lupo-api"))

    implementation("org.jline:jline:4.0.0");
    implementation("org.jline:jline-console:4.0.0")
    implementation("org.jline:jline-terminal-jansi:3.30.15")
}