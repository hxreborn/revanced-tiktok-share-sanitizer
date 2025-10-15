plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "app.revanced.patches.tiktok.misc"
version = "0.1.0-SNAPSHOT"

val revancedPatcherVersion = libs.findVersion("revanced-patcher").get().requiredVersion
val revancedPatcherJar = layout.projectDirectory.file("build-env/tools/revanced-patcher-$revancedPatcherVersion-all.jar")

check(revancedPatcherJar.asFile.exists()) {
    "ReVanced patcher API JAR not found at ${revancedPatcherJar.asFile}. Run ./build-env/scripts/1-setup.sh to download it before building."
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.okhttp)
    compileOnly(files(revancedPatcherJar))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testing)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.get()
                .map { it.name }
                .joinToString(" ")
        )
    }
}

kotlin {
    jvmToolchain(17)
}
