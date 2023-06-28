plugins {
    kotlin("jvm") version "1.7.20"
}

val projectGroup = "kr.jclab.mux"
val projectVersion = Version.PROJECT

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
