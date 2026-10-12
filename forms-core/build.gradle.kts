plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
    `maven-publish`
}
kotlin { jvmToolchain(17); explicitApi() }
dependencies {
    api(libs.coroutines.core)
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
}
java { withSourcesJar() }
publishing {
    publications {
        create<MavenPublication>("core") { from(components["java"]) }
    }
}
