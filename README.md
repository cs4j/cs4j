__cs4j__  [Cron Scheduler for Java] - is a small Cron style task scheduler for Java.

[![Build Status](https://travis-ci.org/cs4j/cs4j.svg?branch=master)]	(https://travis-ci.org/cs4j/cs4j)

## Building

```
mvn -DskipTests=true clean package install
```

## Maven

Add snapshots repository to your pom.xml file. The project now is in alpha stage and has no public releases to Central Repository yet.
```xml
<repositories>
    <repository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>
```

Add project dependency:
```xml
<dependency>
    <groupId>com.github.cs4j</groupId>
    <artifactId>cs4j</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency
```

## Usage

```java
// Create scheduler with a pool of 10 threads.
Scheduler scheduler = new Scheduler(10) ;
 // get service instance. This can be any Java object with @Scheduled methods.
Service service = ...; 
// enable scheduling for all methods with @Scheduled annotation
scheduler.schedule(service); 
...
scheduler.shutdown(); // shutdown the scheduler.
```

Example of Service class:
```java
public class Service {
    // Cron format: second, minute, hour, day, month, day of the week
    // The method runs every first second each minute
    @Scheduled(cron = "1 * * * * * *")   
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

Java 1.7+


### License

This project available under Apache License 2.0.
