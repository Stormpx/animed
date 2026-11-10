import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    id("org.graalvm.buildtools.native") version "1.1.18"
    application
}

group = "org.stormpx"
version = "0.0.5-bgm"

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
    // 繁简 + 日文异体字归一化（标题匹配用），带短语级字典：頭髮->头发、咒術迴戰->咒术回战、戦記->战记
    implementation("com.github.houbb:opencc4j:1.14.0")
    implementation("ch.qos.logback:logback-core:1.5.13")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.8.1")
    implementation("io.ktor:ktor-server-cio:3.2.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions{
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

fun getAppName(): String{
    return project.name+"-"+project.version;
}

tasks.register("getVersion") {
    doLast {
        println(project.version)
    }
}

tasks.register("getAppName") {
    doLast {
        println(getAppName())
    }
}

// native-image 默认按构建机 CPU 探测特性，ARM runner 上会打开 CRC32 / LSE，
// 树莓派 3/4（Cortex-A53 / A72，ARMv8.0）两者都没有，直接报
// "machine does not support ... [FP, ASIMD, CRC32, LSE]"。
// 默认用兼容模式（armv8-a，不含可选扩展）；需要更高性能可用 -PnativeMarch=native 覆盖。
val nativeMarch: String? = providers.gradleProperty("nativeMarch").orNull
    ?: if (System.getProperty("os.arch") == "aarch64") "compatibility" else null

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            resources {
                autodetect()
                // opencc4j 的字典是 classpath 资源，native image 必须显式打进二进制
                includedPatterns.add("data/dictionary/.*")
            }
            mainClass.set("MainKt")
            debug.set(false)
            verbose.set(true)
            quickBuild.set(true)
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.matching("GraalVM Community"))
            })

            buildArgs.add("--initialize-at-build-time=ch.qos.logback,io.ktor.network,kotlin,kotlinx")
            // slf4j 2.x 的 Reporter 把 Reporter$TargetChoice / Reporter$Level 等枚举常量放在静态字段里，
            // 只标记 Reporter 本身会让这些常量跨“构建期/运行期”边界，分析阶段直接失败
            buildArgs.add("--initialize-at-build-time=org.slf4j.helpers")
            buildArgs.add("--initialize-at-run-time=kotlin.uuid,io.modelcontextprotocol")
            buildArgs.add("-H:+InstallExitHandlers")
            buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+StaticExecutableWithDynamicLibC")
            nativeMarch?.let { buildArgs.add("-march=$it") }
            buildArgs.add("--trace-class-initialization=org.slf4j.LoggerFactory,org.slf4j.MarkerFactory")
            jvmArgs.add("-Dsun.jnu.encoding=UTF-8")
            jvmArgs.add("-Dfile.encoding=UTF-8")

            imageName.set(getAppName())

        }
    }
    agent {
        defaultMode.set("standard")
        enabled.set(false)

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