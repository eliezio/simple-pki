import com.github.gundy.semver4j.SemVer
import com.github.gundy.semver4j.model.Version
import org.apache.commons.text.StringSubstitutor
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val versions = libs.versions

    dependencies {
        classpath("org.apache.commons", "commons-text", versions.commons.text.get())
        classpath("com.github.gundy", "semver4j", versions.semver4j.get())
    }
}

plugins {
    java
    groovy
    jacoco

    val kotlinVersion = "1.9.22"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion

    id("org.springframework.boot") version "3.1.1"
    id("io.spring.dependency-management") version "1.1.4"

    id("org.ajoberstar.grgit") version "5.0.0"
    id("com.github.ben-manes.versions") version "0.46.0"

    id("info.solidsoft.pitest") version "1.9.11"

    // Quality / Documentation Plugins
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("org.sonarqube") version "4.0.0.2929"
    id("com.adarshr.test-logger") version "4.0.0"
    id("com.github.ksoichiro.console.reporter") version "0.6.3"
    id("com.epages.restdocs-api-spec") version "0.17.1"
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    id("com.google.cloud.tools.jib") version "3.3.1"
}

description = "Simple-PKI API"
group = "org.nordix"
if (version == "unspecified") {
    version = grgit.describe(mapOf(
        "tags" to true,
        "always" to true,
    )).removePrefix("v")
}
println("version: $version")

val defaultDockerGroup = "eliezio"
val dockerGroup: String? by project
val scmGroup = "eliezio"
val scmProject = "simple-pki"

val mainClassName = "org.nordix.simplepki.Application"

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

val versions = libs.versions

dependencyManagement {
    imports {
        mavenBom("org.spockframework:spock-bom:${versions.spock.bom.get()}")
        mavenBom("org.testcontainers:testcontainers-bom:${versions.testcontainers.get()}")
    }
}

val asciidoctorExt: Configuration by configurations.creating

dependencies {
    asciidoctorExt("org.springframework.restdocs", "spring-restdocs-asciidoctor")

    implementation("org.mapstruct", "mapstruct", versions.mapstruct.get())
    kapt("org.mapstruct", "mapstruct-processor", versions.mapstruct.get())

    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin")
    implementation("io.github.microutils", "kotlin-logging-jvm", versions.kotlin.logging.get())
    implementation("org.bouncycastle", "bcpkix-jdk15to18", versions.bouncycastle.get())
    implementation("org.bouncycastle", "bcprov-jdk15to18", versions.bouncycastle.get())
    implementation("org.hibernate.validator", "hibernate-validator")
    implementation("org.springframework.boot", "spring-boot-starter")
    implementation("org.springframework.boot", "spring-boot-starter-actuator")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.springframework.boot", "spring-boot-starter-undertow")
    implementation("org.springframework.boot", "spring-boot-starter-web") {
        exclude(module = "spring-boot-starter-tomcat")
    }

    runtimeOnly("org.postgresql", "postgresql")
    runtimeOnly("org.flywaydb", "flyway-core")

    testImplementation("ch.qos.logback", "logback-classic")
    testImplementation("com.epages", "restdocs-api-spec-restassured", versions.restdocs.apispec.get())
    testImplementation("com.tngtech.archunit", "archunit-junit5", versions.archunit.get())
    testImplementation("io.github.hakky54", "logcaptor", versions.logcaptor.get())
    testImplementation("io.rest-assured", "rest-assured")
    testImplementation("org.awaitility", "awaitility", versions.awaitility.get())
    testImplementation("org.codehaus.groovy", "groovy-sql")
    testImplementation("org.flywaydb", "flyway-core")
    testImplementation("org.spockframework", "spock-core")
    testImplementation("org.spockframework", "spock-spring")
    testImplementation("org.springframework.boot", "spring-boot-starter-test")
    testImplementation("org.springframework.restdocs", "spring-restdocs-restassured")
    testImplementation("org.testcontainers", "junit-jupiter")
    testImplementation("org.testcontainers", "postgresql")

    testRuntimeOnly("com.athaydes", "spock-reports", versions.spock.reports.get())
}

val docsDir = "$buildDir/resources/main/static"
file(docsDir).mkdirs()

val snippetsDir = "$buildDir/generated-snippets"

kapt {
    arguments {
        arg("mapstruct.suppressGeneratorTimestamp", "true")
        arg("mapstruct.suppressGeneratorVersionInfoComment", "true")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

/*
 * All Archives
 */
tasks.withType<AbstractArchiveTask> {
    //** reproducible build
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirMode = "755".toInt(radix = 8)
    fileMode = "644".toInt(radix = 8)
}

/*
 * All Tests
 */
tasks.withType<Test> {
    useJUnitPlatform()

    // faster start-up time
    jvmArgs("-noverify", "-XX:TieredStopAtLevel=1")

    // register this extra output dir
    outputs.dir(snippetsDir)
    finalizedBy(tasks.reportCoverage)
}

/*
 * JaCoCo
 */
tasks.withType<JacocoReportBase> {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it).apply {
                exclude("**/ApplicationKt.class")
                exclude("**/*MapperImpl.class")
                // see https://github.com/MicroUtils/kotlin-logging/wiki/Coverage-reporting
                exclude("**/*\$logger\$*.class")
            }
        }))
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.reportCoverage {
    dependsOn(tasks.jacocoTestReport)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

/*
 * Pitest
 */
pitest {
    pitestVersion.set("1.9.8")
    junit5PluginVersion.set("1.1.0")
    excludedClasses.set(listOf(
        mainClassName,
        "org.nordix.simplepki.application.KeyStoreConfig",
    ))
    avoidCallsTo.set(listOf(
        "kotlin.jvm.internal.Intrinsics",
    ))
    mutationThreshold.set(97)
    //** reproducible build
    timestampedReports.set(false)
}

tasks.pitest {
    dependsOn(tasks.test)

    finalizedBy("writePitestShieldsJson")
}

/*
 * Test-Driven Documentation
 */
tasks.asciidoctor {
    dependsOn(tasks.test)

    configurations("asciidoctorExt")
    sourceDir("$projectDir/src/docs/asciidoc")
    inputs.dir(snippetsDir)
    baseDirFollowsSourceDir()

    //** reproducible build
    attributes(mapOf("reproducible" to ""))
}

configure<com.epages.restdocs.apispec.gradle.OpenApi3Extension> {
    outputDirectory = docsDir
    setServer("http://localhost:8080")
    title = project.description!!
    version = project.version.toString()
}

/*
 * SpringBoot packaging
 */
tasks.jar {
    manifest {
        attributes(mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        ))
    }
}

springBoot {
    // to create META-INF/build-info.properties. Its contents are exported by /info
    buildInfo {
        properties {
            //** reproducible build
            excludes.set(setOf("time"))
        }
    }
}

tasks.bootJar {
    dependsOn(tasks.asciidoctor, "openapi3")
}

/*
 * SonarQube
 */
sonarqube {
    properties {
        properties(mapOf(
            "sonar.projectKey" to "${scmGroup}_$scmProject",
            "sonar.organization" to "$scmGroup-github",
            "sonar.links.homepage" to "https://github.com/$scmGroup/$scmProject",
            "sonar.links.ci" to "https://travis-ci.org/$scmGroup/$scmProject",
            "sonar.links.scm" to "https://github.com/$scmGroup/$scmProject",
            "sonar.links.issue" to "https://github.com/$scmGroup/$scmProject/issues",
            "sonar.exclusions" to "**/${mainClassName.substringAfterLast('.')}.java"
        ))
    }
}

/*
 * Docker Image
 */
val dockerRegistry: String? by project

jib {
    from {
        image = "amazoncorretto:11"
    }
    to {
        val tagVersion = version.toString()
        image = listOfNotNull(dockerRegistry, dockerGroup ?: defaultDockerGroup, "${project.name}:$tagVersion")
            .joinToString("/")
    }
    containerizingMode = "packaged"
    container {
        jvmFlags = listOf(
                // See http://www.thezonemanager.com/2015/07/whats-so-special-about-devurandom.html
                "-Djava.security.egd=file:/dev/./urandom"
        )
        ports = listOf("8080")
        volumes = listOf("/data")
    }
}

tasks.jibDockerBuild {
    dependsOn(tasks.build)
}

/*
 * ben-manes.versions
 */

val nonStableRegex by lazy {
    "([\\.-](alpha|beta|ea|m|preview|rc)[\\.-]?\\d*(-groovy-\\d+\\.\\d+)?|-b\\d+\\.\\d+)$".toRegex(RegexOption.IGNORE_CASE)
}

fun isNonStable(version: String): Boolean {
    return nonStableRegex.containsMatchIn(version)
}

val upgradesToIgnore = listOf(
    "ch.qos.logback:*:>\${currentVersionMajor}.\${currentVersionMinor}",
    "org.flywaydb:flyway-core:>\${currentVersionMajor}",
    "org.hibernate.validator:hibernate-validator:>\${currentVersionMajor}",
    "org.postgresql:postgresql:>\${currentVersionMajor}.\${currentVersionMinor}",
    "io.rest-assured:rest-assured:>\${currentVersionMajor}",
)

fun moduleMatcher(moduleSpec: String, currentVersion: String): (ModuleComponentIdentifier) -> Boolean {
    val (group, module, version) = moduleSpec.split(':')

    val currentVersionRange by lazy {
        Version.fromString(currentVersion).let { versionElements ->
            StringSubstitutor.replace(
                version,
                mapOf(
                    "currentVersionMajor" to versionElements.major,
                    "currentVersionMinor" to versionElements.minor,
                    "currentVersionPatch" to versionElements.patch,
                )
            )
        }
    }

    return {
        (it.group == group)
            && ((module == "*") || (it.module == module))
            && SemVer.satisfies(it.version, currentVersionRange)
    }
}

tasks.dependencyUpdates {
    rejectVersionIf {
        candidate.version.endsWith("-groovy-4.0")
            || isNonStable(candidate.version)
            || upgradesToIgnore.any { moduleMatcher(it, currentVersion)(candidate) }
    }
}

apply(from = "gradle/writePitestShieldsJson.gradle.kts")
