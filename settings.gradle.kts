pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "kotlin-dynamic-forms"
include(":forms-core")
// Run core tests on a JVM-only machine: ./gradlew -PcoreOnly :forms-core:test
if (!providers.gradleProperty("coreOnly").isPresent) {
    include(":forms-compose")
}
