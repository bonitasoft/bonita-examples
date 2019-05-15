import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.1.4.RELEASE"
    id("org.jetbrains.kotlin.jvm") version "1.3.21"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.21"
}

repositories {
    mavenLocal()
    jcenter()
    maven("http://repositories.rd.lan/maven/all/")
}

dependencies {
    implementation("org.bonitasoft.engine:bonita-engine-spring-boot-starter:7.9.0-SNAPSHOT")
    implementation("org.bonitasoft.engine:bonita-client:7.9.0-SNAPSHOT")

    implementation("org.springframework.boot:spring-boot-starter-web:2.1.4.RELEASE")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // don't forget to add your jdbc drivers:
//    runtime("com.h2database:h2:1.4.199")
    runtime("mysql:mysql-connector-java:8.0.14")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}
