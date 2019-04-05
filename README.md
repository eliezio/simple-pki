[![pipeline status](https://gitlab.com/ebo/simple-pki/badges/master/pipeline.svg)](https://gitlab.com/ebo/simple-pki/commits/master)
[![coverage report](https://gitlab.com/ebo/simple-pki/badges/master/coverage.svg)](https://ebo.gitlab.io/simple-pki/jacoco/)
[![spock reports](https://img.shields.io/badge/spock-reports-blue.svg)](https://ebo.gitlab.io/simple-pki/spock/)
[![pitest reports](https://img.shields.io/badge/pitest-reports-violet.svg)](https://ebo.gitlab.io/simple-pki/pitest/)

## Overview

This application provides the basic services typically found on any PKI:

- Generates X.509 certificates;
- Allow preemptive revocation of issued certificates;
- Generates an updated CRL (Certificate Revocation List);
- Supply the CA certificate.

The PKI implemented is comprised of a single CA with _no_ intermediate CAs.

## Building the application

To build the application from the sources just run:

```
./gradlew build
```

## Docker Image

You can also build a Docker image from the source code using the
[Jib plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin#build-your-image) like, for example:

```
./gradlew jibDockerBuild
```

## Running with 'debug' profile activated

If using Gradle, run:

```
./gradlew bootRun --args='--spring.profiles.active=debug'
```

