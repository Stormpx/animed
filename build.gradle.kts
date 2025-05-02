import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

group = "org.stormpx"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
//    jcenter()
}

dependencies {
    implementation("org.simplejavamail:simple-java-mail:7.4.0")
//    api("com.apptastic:rssreader:2.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("com.charleskorn.kaml:kaml:0.53.0")
    implementation("ch.qos.logback:logback-core:1.5.13")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.5.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    compilerOptions{
        jvmTarget.set(JvmTarget.JVM_17)
    }
}




application {
    mainClass.set("MainKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("animed")
    archiveClassifier.set("")
    archiveVersion.set("")
}