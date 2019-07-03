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