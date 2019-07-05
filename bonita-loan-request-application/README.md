# Bonita Loan Request Application


This application is an example of how one can embedded Bonita Engine (BPM workflow engine)
is a **Spring Boot** application.  
The propose use-case is an application based on a process that allow someone to request
a Loan to their bank. On its side, the bank can review the request, approve or reject the loan request
and say why.


## Scope
In this tutorial, you will learn how to write an application, using Spring Boot framework, that integrates
Bonita Execution Engine to operate processes.  
You will learn how to configure Bonita Engine to point to the database of your choice, tune the connection pool size


## Prerequisites
### Database

If you just want to run an embedded H2 database, nothing is required.

To have your application point to a MySQL, PostgreSQL, Microsoft SQL Server, or Oracle database make sure
you have a Database server up and running, and that it contains a schema reserved for Bonita Engine (default name is `bonita`).  
For deeper details on database preparation for Bonita, see [the specific documentation page](https://documentation.bonitasoft.com/bonita/current/database-configuration).

### Processes
This tutorial assumes you have basic knowledge of BPMN / process designing.

## Use case
For this example, we will develop and interact with the following process.

![Loan Request process diagram](loan-request-diagram.png)

## Let's write the application step by step

### Boostrap of the application, using Spring boot
Let's write a Gradle build with the minimum Spring boot + Kotlin requirements:
```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.1.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    kotlin("jvm") version "1.3.41"
    kotlin("plugin.spring") version "1.3.41"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // Embed an application server:
    implementation("org.springframework.boot:spring-boot-starter")
    
    // Libs to code in Kotlin:
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

// configure Kotlin compiler:
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
```

Write the main Spring boot class to launch our application:
```kotlin
package org.bonitasoft.loanrequest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LoanRequestApplication

fun main(args: Array<String>) {
	runApplication<LoanRequestApplication>(*args)
}
```
In the current state, our application can already be run (but does not do anything) by typing in the command line:
```
./gradlew bootRun
```
and can then be accessed at http://localhost:8080/

Now, let's add Bonita Engine in the equation in our `build.gradle.kts`:
```kotlin
...
dependencies {
    ...
    val bonitaEngineVersion = "7.9.0"

    // adding dependency on bonita-engine-spring-boot-starter automatically provides
    // and starts a Bonita Engine when used in a Spring Boot application:
    implementation("org.bonitasoft.engine:bonita-engine-spring-boot-starter:$bonitaEngineVersion")

    // use bonita-client to be able to interact with the running Engine
    // to deploy and run instances of processes:
    implementation("org.bonitasoft.engine:bonita-client:$bonitaEngineVersion")
    
    // Add the database driver we want Bonita to use:
    runtime("com.h2database:h2:1.4.199")
    ...
}
```
Now, through the magic of Spring boot, a Bonita Engine is automatically started when our application starts.  
We can see Engine startup logs in the console:
```
|09:44:15.601|main|INFO |o.b.p.s.ScriptExecutor| configuration for Database vendor: h2
|09:44:15.989|main|INFO |o.b.p.s.PlatformSetup| Connected to 'h2' database with url: 'jdbc:h2:file:./build/h2_database/bonita' with user: 'BONITA'
|09:44:16.341|main|INFO |o.b.p.s.ScriptExecutor| Executed SQL script file:/home/manu/.gradle/caches/modules-2/files-2.1/org.bonitasoft.platform/platform-resources/7.9.0/c183cb/platform-resources-7.9.0.jar!/sql/h2/createTables.sql
...
|09:44:26.437|main|INFO |o.b.e.a.i.PlatformAPIImpl| THREAD_ID=1 | HOSTNAME=manu-laptop | Start service of platform : org.bonitasoft.engine.classloader.ClassLoaderServiceImpl
|09:44:26.438|main|INFO |o.b.e.a.i.PlatformAPIImpl| THREAD_ID=1 | HOSTNAME=manu-laptop | Start service of platform : org.bonitasoft.engine.cache.ehcache.PlatformEhCacheCacheService
|09:44:26.490|main|INFO |o.b.e.a.i.PlatformAPIImpl| THREAD_ID=1 | HOSTNAME=manu-laptop | Start service of platform : org.bonitasoft.engine.service.BonitaTaskExecutor
...
|09:44:26.708|main|INFO |o.b.e.a.i.t.SetServiceState| THREAD_ID=1 | HOSTNAME=manu-laptop | TENANT_ID=1 | start tenant-level service org.bonitasoft.engine.cache.ehcache.EhCacheCacheService on tenant with ID 1
|09:44:26.718|main|INFO |o.b.e.a.i.t.SetServiceState| THREAD_ID=1 | HOSTNAME=manu-laptop | TENANT_ID=1 | start tenant-level service org.bonitasoft.engine.business.data.impl.JPABusinessDataRepositoryImpl on tenant with ID 1
```





Let's write some integration tests for our application:
```kotlin
package org.bonitasoft.loanrequest

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class LoanRequestApplicationTests {

    @Test
    fun `process should be started and can be operated with human tasks`() {
        // TO be completed
    }

}
```


### Bonus: cherry on the cake
Spring boot allows to easily tune the banner that is displayed when an application starts.  
Simply put a `banner.txt` file in the `resources` folder with some ASCII art:
```kotlin
 _                        ______                           _
| |                       | ___ \                         | |
| |     ___   __ _ _ __   | |_/ /___  __ _ _   _  ___  ___| |_
| |    / _ \ / _` | '_ \  |    // _ \/ _` | | | |/ _ \/ __| __|
| |___| (_) | (_| | | | | | |\ \  __/ (_| | |_| |  __/\__ \ |_
\_____/\___/ \__,_|_| |_| \_| \_\___|\__, |\__,_|\___||___/\__|
                                        | |
                                        |_|
```



## Build your application
run `./gradlew build` to build the binaries. It will generate a jar file in `build/libs/` folder.


## Run your application
Simply run the gradle command:
```
./gradlew bootRun
```
or run the previously built jar file:
```bash
java -jar build/libs/bonita-loan-request-application.jar
```
You can then access the list of processes by opening a web browser at `http://localhost:8080/processes`