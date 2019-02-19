# Bonita embedded engine example
This example project aims at illustrating how someone can embed a Bonita Engine runtime in a simple application,
and use its APIs to interaction with the BPM Engine

## Requisites
* Have Java 8 + Maven installed + Internet connection to download dependencies

## Build and run: one-liner
    mvn package && cd target && unzip *.zip && java -jar embedded-engine-example-*/embedded-engine-example-*.jar

## Build the project
Simply build

> mvn install

from the root folder of this project.

## Run example app as a standalone application
Simply unzip the previously built application and run it

    cd target
    unzip *.zip
    java -jar embedded-engine-example-*/embedded-engine-example-*.jar 

from the root folder of this project.
