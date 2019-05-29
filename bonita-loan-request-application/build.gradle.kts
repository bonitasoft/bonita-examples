import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.1.4.RELEASE"
    id("org.jetbrains.kotlin.jvm") version "1.3.21"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.21"
}

repositories {
    mavenLocal()
    jcenter()
    // FIXME: remove this internal repository when Bonita 7.9.0 is out publicly:
    maven("http://repositories.rd.lan/maven/all/")
}

dependencies {
    val bonitaEngineVersion = "7.9.0-SNAPSHOT"

    // adding dependency on bonita-engine-spring-boot-starter automatically provides
    // and starts a Bonita Engine when used in a Spring Boot application:
    implementation("org.bonitasoft.engine:bonita-engine-spring-boot-starter:$bonitaEngineVersion")

    // use bonita-client to be able to interact with the running Engine
    // to deploy and run instances of processes:
    implementation("org.bonitasoft.engine:bonita-client:$bonitaEngineVersion")

    // API to simulate user waiting before executing a task:
    implementation("org.awaitility:awaitility:2.0.0")

    // Libs to expose Rest API through an embedded application server:
    implementation("org.springframework.boot:spring-boot-starter-web:2.1.4.RELEASE")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")

    // Libs to code in Kotlin:
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // don't forget to add your jdbc drivers corresponding to the database you are
    // pointing at (supported databases are H2, MySQL, PostgreSQL, MS SqlServer, Oracle DB:
    // runtime("com.h2database:h2:1.4.199")
    // runtime("mysql:mysql-connector-java:8.0.14")
    runtime("org.postgresql:postgresql:42.2.5")
    // runtime("com.microsoft.sqlserver:mssql-jdbc:7.2.1.jre8")
    // Oracle database drivers are not open-source and thus cannot be included here directly
}

// configure Kotlin compiler:
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}
