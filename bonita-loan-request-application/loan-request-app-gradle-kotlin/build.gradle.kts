import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.3.4.RELEASE"
//    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    id("org.jetbrains.kotlin.jvm") version "1.3.41"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.41"
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    val bonitaEngineVersion = "7.11.2"

    // adding dependency on bonita-engine-spring-boot-starter automatically provides
    // and starts a Bonita Engine when used in a Spring Boot application:
    implementation("org.bonitasoft.engine:bonita-engine-spring-boot-starter:$bonitaEngineVersion")
    // Use the following implementation if you are using Subscription version of Bonita:
    // implementation("com.bonitasoft.engine:bonita-engine-spring-boot-starter-sp:$bonitaEngineVersion")

    // use bonita-client to be able to interact with the running Engine
    // to deploy and run instances of processes:
    implementation("org.bonitasoft.engine:bonita-client:$bonitaEngineVersion")
    // Use the following implementation if you are using Subscription version of Bonita:
    // implementation("com.bonitasoft.engine:bonita-client-sp:$bonitaEngineVersion")

    // API to simulate user waiting before executing a task:
    implementation("org.awaitility:awaitility:2.0.0")

    // Libs to expose Rest API through an embedded application server:
    implementation("org.springframework.boot:spring-boot-starter-web:2.3.4.RELEASE")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")

    // Libs to code in Kotlin:
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // don't forget to add your jdbc drivers corresponding to the database you are
    // pointing at (supported databases are H2, MySQL, PostgreSQL, MS SqlServer, Oracle DB):

    runtime("com.h2database:h2:1.4.199")
    // runtime("mysql:mysql-connector-java:8.0.14")
    // runtime("org.postgresql:postgresql:42.2.5")
    // runtime("com.microsoft.sqlserver:mssql-jdbc:7.2.1.jre8")
    // Oracle database drivers are not open-source and thus cannot be included here directly
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

// configure Kotlin compiler:
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}

task<DependencyReportTask>("listDependencies") {
    setGroup("Documentation")
    setDescription("List runtime dependencies")
    configurations = setOf(project.configurations.runtimeClasspath.get())
}