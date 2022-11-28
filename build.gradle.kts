import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

val projectGroup = "kr.jclab.mux"
val projectVersion = "0.0.2-rc1"

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
