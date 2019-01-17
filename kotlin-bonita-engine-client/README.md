# Bonita engine client example using Kotlin
This example project aims at illustrating how someone can call a running Bonita Runtime (running under Tomcat for instance)
to perform simple operations using APIs.

We propose to use Kotlin, to illustrate that any language running on the JVM can be used to interact with bonita client library.

## Requisites
* Have Java + Maven installed + Internet connection to download dependencies
* Have a Bonita Community Tomcat / WildFly bundle installed and started
* This bundle must be listening at localhost:8080/bonita (by default), or you must pass extra parameter if you have specific values

## Build the project
Simply run

> mvn install -DskipTests

from the root folder of this project.

## Run example as junit test
### with default server values
Simply run

> mvn test

from the root folder of this project.

### with custom server url and webapp name
> mvn test -Dserver.url=127.0.0.1:8181 -Dapplication.name=bonita-kotlin