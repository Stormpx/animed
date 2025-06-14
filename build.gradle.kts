import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.graalvm.buildtools.native") version "0.10.6"
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
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions{
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            resources.autodetect()
            mainClass.set("MainKt")
            debug.set(false)
            useFatJar.set(true)
            fallback.set(false)
            verbose.set(true)
            quickBuild.set(true)
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.matching("Oracle Corporation"))
            })

            buildArgs.add("--initialize-at-build-time=org.slf4j.loggerFactory,ch.qos.logback,io.ktor.network,kotlin,kotlinx")
            buildArgs.add("--initialize-at-build-time=org.slf4j.helpers.Reporter")
            buildArgs.add("--initialize-at-run-time=kotlin.uuid,io.modelcontextprotocol")
            buildArgs.add("-H:+InstallExitHandlers")
            buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+StaticExecutableWithDynamicLibC")
            buildArgs.add("--trace-class-initialization=org.slf4j.LoggerFactory,org.slf4j.MarkerFactory")

            imageName.set("animed")

        }
    }
    agent {
        defaultMode.set("standard")
        enabled.set(true)

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