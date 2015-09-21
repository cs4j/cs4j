__cs4j__  (Cron Scheduler for Java) - is a small and efficient Cron style task scheduler for Java.

[![Build Status](https://travis-ci.org/cs4j/cs4j.svg?branch=master)]	(https://travis-ci.org/cs4j/cs4j)

## Building

```
mvn -DskipTests=true clean package install
```

## Usage

```java
Scheduler scheduler = new Scheduler(10) ; // Creates scheduler with a pool of 10 threads.
Service service = ...; // get service instance.
scheduler.schedule(service); // enable scheduling for all methods with @Scheduled annotation
...
scheduler.shutdown(); // shuts down the scheduler.
```

Example of Service class:
```java
public class Service {
   
    // runs every first second each minute
    @Scheduled(cron = "1 * * * * * *") // Cron format: second, minute, hour, day, month, day of the week  
    void ping() {
        log.info("pong")
    }
}

```

### Implementation details
The scheduler is based on Spring's CronSequenceGenerator class. It uses compatible syntax, inherits and successfully passes all original Spring tests.
 
The package is recommended when you can' use  original Spring implementation for some reason.
CS4J has no additional runtime dependencies and it's result binary size is about 11kb.

### Requirements

Java 1.6+


### License

This project available under Apache License 2.0.
