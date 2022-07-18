import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    application
}

group = "org.stormpx"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

dependencies {
//    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.apptastic:rssreader:2.5.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.register<Copy>("copy") {
    from(layout.projectDirectory.file("src/main/resources/jaxb.index"))
    into(layout.buildDirectory.dir("classes/kotlin/main/org/stormpx/arimedown"))
}



application {
    mainClass.set("MainKt")
}