# Changelog

## [1.0.2](https://github.com/eliezio/simple-pki/compare/v1.0.1...v1.0.2) (2022-10-31)


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
