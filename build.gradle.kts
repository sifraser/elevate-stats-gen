import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    jcenter()
    mavenLocal()
}

version = "0.0.1"
group = "com.sifraser"

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "com.sifraser.elevate.statsgen.ElevateStatsGenKt"
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("com.opencsv:opencsv:5.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
