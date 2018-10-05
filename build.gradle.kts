import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.2.70"
    `maven-publish`
}

group = "me.jartreg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    runtimeOnly("org.tukaani", "xz", "1.8")
    implementation("org.apache.commons", "commons-compress", "1.18")
    implementation("commons-codec", "commons-codec", "1.11")

    implementation("org.bouncycastle", "bcpg-jdk15on", "1.60")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
    plugins {
        create("aptRepo") {
            id = "me.jartreg.apt-repo"
            implementationClass = "me.jartreg.gradle.aptrepo.AptRepositoryPlugin"
        }
    }
}