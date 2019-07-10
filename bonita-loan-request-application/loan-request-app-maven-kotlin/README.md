# Bonita Loan Request Application (Maven version)


This application is an example of how one can embedded Bonita Engine (BPM workflow engine)
is a **Spring Boot** application.  
The propose use-case is an application based on a process that allow someone to request
a Loan to their bank. On its side, the bank can review the request, approve or reject the loan request
and say why.


## Full details of this tutorial
The fully detailed tutorial can be viewed in the [Gradle version of this example](../loan-request-app-gradle-kotlin).  
Apart from the build / run part, the rest of the example remains the same.

## Build your application with Maven
run `./mvnw clean package` to build the binaries. It will generate a jar file in `target/` folder.


## Run your application with Maven
Simply run the Maven command:
```
./mvnw clean spring-boot:run
```
or run the previously built jar file:
```bash
java -jar target/bonita-loan-request-application-1.0.0.jar
```
You can then access the list of processes by opening a web browser at `http://localhost:8080/processes`