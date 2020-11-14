![build](https://github.com/ArneLimburg/testflight/workflows/build/badge.svg)[![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=security_rating)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=bugs)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_testflight&metric=coverage)](https://sonarcloud.io/dashboard?id=ArneLimburg_testflight)

# Testflight.Space

Testflight.Space is a JUnit 5 extension for flyway to mandate fast database tests with real databases and real flyway migrations.

Simply annotate your JUnit 5 test with `@Flyway` and a fresh database will be started for every test execution.

## Supported databases

Flyway extension internally uses Testcontainers to start databases so every database that is supported by Testcontainers will be supported eventually. Currently only PostgreSQL is supported.

## Initial test data

In addition to the automatic flyway migration, you can specify database scripts that are executed after the migration.
Simply configure them via `@Flyway(testDataScripts = {...})`.

## Using a custom docker image

If you want to use a custom docker image for your database, specify it via `@Flyway(dockerImage = "")`.

## Why is Testflight.Space so fast?

Testflight.Space does the actual migration at most once per test suite execution (That means i.e. 'once per `mvn test` run).
Then the resulting docker image is cached and reused for every test.
If you don't change your flyway scripts and your test data, Testflight.Space will even reuse that image for further test executions.
So as long as the database and test data does not change, the database image (with the test data) will be reused,
even when you run the tests from your IDE.
