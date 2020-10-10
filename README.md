# Flyway Extension

The Flyway Extension is a JUnit 5 extension to mandate fast database tests with real databases and real flyway migrations.

Simply annotate your JUnit 5 test with `@Flyway` and a fresh database will be started for every test execution.

## Supported databases

Flyway extension internally uses Testcontainers to start databases so every database that is supported by Testcontainers will be supported eventually. Currently only PostgreSQL is supported.

## Initial test data

In addition to the automatic flyway migration, you can specify database scripts that are executed after the migration.
Simply configure them via `@Flyway(testDataScripts = {...})`.

## Using a custom docker image

If you want to use a custom docker image for your database, specify it via `@Flyway(dockerImage = "")`.

## Why is the Flyway Extension so fast?

The Flyway Extension does the actual migration at most once per run. Then the resulting docker image is cached and reused for every test.
If you don't change your flyway scripts and your test data, the Flyway Extension will even reuse that image for further runs.
