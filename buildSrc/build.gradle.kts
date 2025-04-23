plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    api(platform("com.fasterxml.jackson:jackson-bom:2.15.3"))

    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml")
}

