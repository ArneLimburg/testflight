[![maintained](https://img.shields.io/badge/Maintained-yes-brightgreen.svg)](https://github.com/ArneLimburg/testflight/graphs/commit-activity) [![maven central](https://maven-badges.herokuapp.com/maven-central/space.testflight/testflight/badge.svg)](https://maven-badges.herokuapp.com/maven-central/space.testflight/testflight) ![build](https://github.com/ArneLimburg/testflight/workflows/build/badge.svg) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=security_rating)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=bugs)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=coverage)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight)

# Testflight.Space

Testflight.Space is a JUnit 5 extension for flyway to mandate fast database tests with real databases and real flyway migrations.

Simply annotate your JUnit 5 test with `@Flyway` and a fresh database will be started for every of your tests.

## Configuring database startup

By default a fresh instance is started for every test method,
but you can also configure Testflight.Space to do it for every test execution.
This may be interesting, when a method will be executed multiple times (i.e. for parameterized tests).

```
@Flyway(databaseInstance = DatabaseInstance.PER_TEST_EXECUTION)
```

Or, when you just want to start it once per test class, you can use `DatabaseInstance.PER_TEST_CLASS`. 

## Accessing the database

After starting the database Testflight.Space will inject the connection properties into Java system properties.
By default the following properties are used:

```
space.testflight.jdbc.url
space.testflight.jdbc.username
space.testflight.jdbc.password
```

You can change the property names via:

```
@Flyway(configuration = {
  @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
  @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
  @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
})
```

## Supported databases

Flyway extension internally uses Testcontainers to start databases so every database that is supported by Testcontainers will be supported eventually. Currently only PostgreSQL is supported.

## Initial test data

In addition to the automatic flyway migration, you can specify database scripts that are executed after the migration.
Simply configure them via `@Flyway(testDataScripts = {...})`.

## Using a custom docker image

If you want to use a custom docker image for your database, specify it via `@Flyway(dockerImage = "")`.

## Integration with other test frameworks

When using Testflight.Space together with another testing framework that starts a container for you,
and manages database access like
[Spring Testing](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html) or
[Quarkus Testing](https://quarkus.io/guides/getting-started-testing) there are two things to consider.

### JUnit 5 Extension ordering

You first have to ensure, that Testflight.Space starts first,
so that the database is already there, when the container tries to access it.
You can ensure that by declaring the ``FlywayExtension`` **before** the respective test extensions.

```
@ExtendWith({ FlywayExtension.class, QuarkusTestExtension.class })
@Flyway(...)
@QuarkusTest
class DatabaseTest {
    ...
}
```

### Refreshing of connections

Normally a container maintains a connection pool to ensure high performance in production environment.
However with Testflight.Space this might be a problem.
Since Testflight.Space by default starts a new database instance for every test method,
an open connection that resides in a connection pool even after the test method,
will then be invalid. There are two ways to handle that problem.
Either you disable connection pooling for your tests completely.
I.e. in Quarkus you can do that with the following property:

```
quarkus.datasource.jdbc.pooling-enabled=false
```
 
Or you configure the connection pool
such that it verifies every connection before it is given to the client.
I.e. when using a c3p0 pool you can configure that with the following property:

```
c3p0.testConnectionOnCheckout=true
```

## Integration with Hibernate 

The easiest way to integrate Testflight.Space with a test where Hibernate is used
(being it standalone or in a container) is to add the Hibernate c3p0 support with

```
<dependency>
  <groupId>org.hibernate</groupId>
  <artifactId>hibernate-c3p0</artifactId>
  <version>${your.hibernate.version}</version>
  <scope>test</scope>
</dependency>
```

Then you can configure c3p0 like described above.

## Why is Testflight.Space so fast?

Testflight.Space does the actual migration at most once per test suite execution (That means i.e. 'once per `mvn test` run).
Then the resulting docker image is cached and reused for every test.
If you don't change your flyway scripts and your test data, Testflight.Space will even reuse that image for further test executions.
So as long as the database and test data does not change, the database image (with the test data) will be reused,
even when you run the tests from your IDE.
