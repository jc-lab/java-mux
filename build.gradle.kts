import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

val projectGroup = "kr.jclab.mux"
val projectVersion = "1.0-SNAPSHOT"

group = projectGroup
version = projectVersion

allprojects {
    group = projectGroup
    version = projectVersion
}

repositories {
    mavenCentral()
}

dependencies {
}
