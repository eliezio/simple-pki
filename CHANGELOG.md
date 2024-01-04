# Changelog

## [1.1.1](https://github.com/eliezio/simple-pki/compare/v1.1.0...v1.1.1) (2024-01-04)


### 🐞 Bug Fixes

* Exclusion of time from buildInfo.properties ([8cdd2e2](https://github.com/eliezio/simple-pki/commit/8cdd2e27159cc2f5c5cd5f9f949c27b8940693b6))


### 🚧 Refactor

* Migrate to Spring Boot 3.2.1 and Java 17 ([1a771ae](https://github.com/eliezio/simple-pki/commit/1a771ae0aaf6e54e006c4ae1fe46d8ccee7b9b09))

## [1.1.0](https://github.com/eliezio/simple-pki/compare/v1.0.0...v1.1.0) (2022-11-02)


### 🐞 Bug Fixes

* **core:** Fix CA configuration, broken in container mode ([7b4c29f](https://github.com/eliezio/simple-pki/commit/7b4c29ff4a3a6b05fe94914dc9cc102f5987da13))
* **core:** Fix missing Implementation-Title and Version when running from docker image ([1af6972](https://github.com/eliezio/simple-pki/commit/1af69721e5eca009dc1f978d78157bcab9d886fa))
* **core:** Using JVM 11 in docker image ([f36c6e7](https://github.com/eliezio/simple-pki/commit/f36c6e7b2266d160824db4ad5f84d917dc0f404b))


### 🚧 Refactor

* **core:** Refactor to fit on Homberg's proposal of Hexagonal Architecture ([46a57c2](https://github.com/eliezio/simple-pki/commit/46a57c2e26fde892879a8370276cae77ca378485))
* **core:** Refactor to Hexagonal Architecture ([521e302](https://github.com/eliezio/simple-pki/commit/521e30248cb36e6c63d19a6d0b288bb19cf119a8))
* **core:** Rename SingleEntityRepository to PkiEntityRepository ([244b697](https://github.com/eliezio/simple-pki/commit/244b697ee3222b3f0e495e4c9107e8bfa1688cbc))
* **core:** Replace MockMvc by RestAssured ([4e35904](https://github.com/eliezio/simple-pki/commit/4e359048e7ed18a028491d10de41888b67b4152a))
* **core:** Simple ServiceEventLogger using Filter ([8719b9b](https://github.com/eliezio/simple-pki/commit/8719b9bf97c3f09e5d58390fec44532a2ec8c008))
* Drop support to lazy loading of CA cert ([a26fa80](https://github.com/eliezio/simple-pki/commit/a26fa80d0e812ea33ab22fdd283d41c73dbd55fd))
* Introduce detekt plugin ([27eb50c](https://github.com/eliezio/simple-pki/commit/27eb50c85bc9b821b19b388cf978762a350fd15b))
* Merge UT and IT ([b6f8f88](https://github.com/eliezio/simple-pki/commit/b6f8f8868754f4d4c9c5a7476eaea92adbe6f222))
* Migrate to Kotlin ([0bc1300](https://github.com/eliezio/simple-pki/commit/0bc13002b159cbb0effe2c3afef257df5d18a129))


### 🚇 Continuous Integration

* Add GH workflows (check, release-please) ([402875a](https://github.com/eliezio/simple-pki/commit/402875a82472e68060d96a01dd78f097fede9d6d))


### ⭐ New Features

* Switch to PostgreSQL ([06cee28](https://github.com/eliezio/simple-pki/commit/06cee28973488e26b3c9c07980b6a5c0ebb03a0a))

## [1.0.1](https://github.com/eliezio/simple-pki/compare/v1.0.0...v1.0.1) (2022-09-25)


### 🐞 Bug Fixes

* **core:** Fix CA configuration, broken in container mode ([cfd5ad8](https://github.com/eliezio/simple-pki/commit/cfd5ad8845eba1582199bf08aa7959df31ac4431))
* **core:** Fix missing Implementation-Title and Version when running from docker image ([23b9bc5](https://github.com/eliezio/simple-pki/commit/23b9bc5cd15c497e26142cf0b40baeaa622a8a6e))
* **core:** Using JVM 11 in docker image ([9fd5adb](https://github.com/eliezio/simple-pki/commit/9fd5adbbdcb842826968b5bcf4905c9fa76914f4))


### 🚧 Refactor

* **core:** Refactor to fit on Homberg's proposal of Hexagonal Architecture ([3bf1c94](https://github.com/eliezio/simple-pki/commit/3bf1c948f8af94a93a6e2a0a2a7dc1f45c910573))
* **core:** Refactor to Hexagonal Architecture ([b4fa69b](https://github.com/eliezio/simple-pki/commit/b4fa69b564a5774484f8798943eb10ccbc1693f9))
* **core:** Rename SingleEntityRepository to PkiEntityRepository ([635bb39](https://github.com/eliezio/simple-pki/commit/635bb39bb1ac8e666372fccb00bfe9bcb2b4b493))
* **core:** Replace MockMvc by RestAssured ([2965cde](https://github.com/eliezio/simple-pki/commit/2965cdeccf814e7810557f9f697ce845ead6603a))
* **core:** Simple ServiceEventLogger using Filter ([264abd6](https://github.com/eliezio/simple-pki/commit/264abd69e887b354c595b675ae594710e11ced75))
* Drop support to lazy loading of CA cert ([cb56483](https://github.com/eliezio/simple-pki/commit/cb564836748c893c2c8262461907384cb853a518))
* Introduce detekt plugin ([82f2602](https://github.com/eliezio/simple-pki/commit/82f2602447c45b005f9827961f485cb719b07ea1))
* Merge UT and IT ([427dce7](https://github.com/eliezio/simple-pki/commit/427dce737fd5b792835972911fe154155a3caab1))
* Migrate to Kotlin ([1b463b8](https://github.com/eliezio/simple-pki/commit/1b463b89b1b0d3e7a1e7c394e9b6693765d2bd06))


### 🚇 Continuous Integration

* Add GH workflows (check, release-please) ([77c05f9](https://github.com/eliezio/simple-pki/commit/77c05f922d9c2cef45c26661a893b05566123cda))
