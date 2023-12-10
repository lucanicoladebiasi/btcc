# Getting Started

### Task
The mobile software testing team has 10 mobile phones that it needs to share for testing
purposes.
- Samsung Galaxy S9
- 2x Samsung Galaxy S8
- Motorola Nexus 6
- Oneplus 9
- Apple iPhone 13
- Apple iPhone 12
- Apple iPhone 11
- iPhone X
- Nokia 3310 

Please create a service that allows a phone to be booked / returned.

The following information should also be available for each phone.
- Availability (Yes / No)
- When it was booked
- Who booked the phone
  
Please send a notification to a message broker of your choice when a phone is either booked or returned.

---

### Proposed Solution

The task doesn't distinguish between book of the mobile to test and collection of the same,
hence the implementation considers the `\book` verb as the action to collect the `mobile`
by the `requestor` testing it, if the `mobile` is available. 
The mobile is booked until the `due` time. 

The `\return` action makes the `mobile` available to be booked again if who returns
it is the same who booked it.

The `\list` action returns the list of mobile devices as
* `AVAILABLE` if not booked/collected by anyone,
* `IN USE` if a `requestor` collected it, reporting when the book was `made` and return is`due`.
* `DUE` if the `requestor` should have returned the `mobile` because now is after `due`.

Time properties as 'book `made`' and 'return `due`' are expressed as
[ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) local date-time strings.

The software is a Kotlin + Gradle project developed with [JetBrain IDEA](https://www.jetbrains.com/idea/).

---

### Run

#### Kafka

Kafka must be available, if not follow the
[Apache Kafka Quickstart](https://kafka.apache.org/quickstart) guide to install it locally.

Create the topics `book` and `return` where events are produced.

* `bin/kafka-topics.sh --create --topic book --bootstrap-server localhost:9092`
* `bin/kafka-topics.sh --create --topic return --bootstrap-server localhost:9092`

Run two Kafka *Consumer* clients, one for each topic, in two different shells.

* `bin/kafka-console-consumer.sh --topic book --bootstrap-server localhost:9092`
* `bin/kafka-console-consumer.sh --topic return --bootstrap-server localhost:9092`

#### Spring Application Server

The Kafka *Broker* must be up and running, by default at `localhost:9092`, before the application server runs. 

The labels for the mobile devices are loaded as comma separated string elements by the `mobiles`
key in the [application.properties](./src/main/resources/application.properties) file.

* Open a shell in the directory where the woftware was downloaded. 
* Build the application server with `./gradlew bootJar`.
* Start the application server with `java -jar build/libs/btcc-0.0.1-SNAPSHOT.jar`
* Visit http://localhost:8080/swagger-ui/index.html how to use API end-points.
* Stop the application server pressing [CTRL+C].

### Test

At least seven mobiles must be defined to allow
[ControllerTest](./src/test/kotlin/com/github/lucanicoladebiasi/btcc/ControllerTest.kt)
to run tests in parallel and prove concurrent requests are handled correctly.

The directory `src/test/kotlin/com/github/lucanicoladebiasi/btc` has the test code 
covering the required behavior.

The test code embeds an application server to test REST-API end-point and an in-memory
Kafka using the port 9093 to test messaging functionalities.

Run `./gradlew test` to execute unit/integration tests.

---

### Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.0/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.0/gradle-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/3.2.0/reference/htmlsingle/index.html#web)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/docs/3.2.0/reference/htmlsingle/index.html#messaging.kafka)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/3.2.0/reference/htmlsingle/index.html#using.devtools)

### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Additional Links

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

