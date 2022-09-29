import com.github.gundy.semver4j.SemVer
import com.github.gundy.semver4j.model.Version
import org.apache.commons.text.StringSubstitutor
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.function.Predicate
import kotlin.math.floor

buildscript {
    dependencies {
        classpath("org.apache.commons:commons-text:1.10.0")
        classpath("org.jsoup", "jsoup", "1.15.3")
    }
}

plugins {
    java
    groovy
    jacoco

    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion

    id("org.springframework.boot") version "2.7.3"

    id("org.ajoberstar.grgit") version "5.0.0"
    id("com.github.ben-manes.versions") version "0.42.0"

    id("info.solidsoft.pitest") version "1.9.0"

    // Quality / Documentation Plugins
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    id("org.sonarqube") version "3.4.0.2513"
    id("com.adarshr.test-logger") version "3.2.0"
    id("com.github.ksoichiro.console.reporter") version "0.6.3"
    id("com.epages.restdocs-api-spec") version "0.16.2"
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    id("com.google.cloud.tools.jib") version "3.3.0"
}

apply(plugin = "io.spring.dependency-management")

description = "Simple-PKI API"
group = "org.nordix"
if (version == "unspecified") {
    version = grgit.describe(mapOf(
        "tags" to true,
        "always" to true,
    )).removePrefix("v")
}
println("version: $version")

val defaultDockerGroup = "ebo"
val dockerGroup: String? by project
val scmGroup = "eliezio"
val scmProject = "simple-pki"

val mainClassName = "org.nordix.simplepki.Application"

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

val restdocsApiSpecVersion: String by project

val bouncyCastleVersion = "1.70"
val mapstructVersion = "1.5.2.Final"
val spockFrameworkVersion = "2.2-groovy-3.0"
val spockReportsVersion = "2.3.1-groovy-3.0"

repositories {
    mavenCentral()
}

val asciidoctorExt: Configuration by configurations.creating

dependencies {
    asciidoctorExt("org.springframework.restdocs", "spring-restdocs-asciidoctor")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    kapt("org.mapstruct:mapstruct-processor:$mapstructVersion")

    implementation("javax.inject", "javax.inject", "1")
    implementation("org.springframework.boot", "spring-boot-starter")
    implementation("org.springframework.boot", "spring-boot-starter-web") {
        exclude(module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot", "spring-boot-starter-undertow")
    implementation("org.springframework.boot", "spring-boot-starter-actuator")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.bouncycastle", "bcprov-jdk15on", bouncyCastleVersion)
    implementation("org.bouncycastle", "bcpkix-jdk15on", bouncyCastleVersion)

    runtimeOnly("com.h2database", "h2")
    runtimeOnly("mysql", "mysql-connector-java")

    testImplementation("com.tngtech.archunit:archunit-junit5:0.23.1")
    testImplementation("org.springframework.boot", "spring-boot-starter-test")
    testImplementation(platform("org.spockframework:spock-bom:$spockFrameworkVersion"))
    testImplementation("org.spockframework:spock-core")
    testImplementation("org.spockframework:spock-junit4")
    testImplementation("org.spockframework:spock-spring")
    testImplementation("org.codehaus.groovy:groovy-sql")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.springframework.restdocs", "spring-restdocs-restassured")
    testImplementation("com.epages", "restdocs-api-spec-restassured", restdocsApiSpecVersion)
    testImplementation("ch.qos.logback", "logback-classic")

    testRuntimeOnly("com.athaydes", "spock-reports", spockReportsVersion)
}

val docsDir = "$buildDir/resources/main/static"
file(docsDir).mkdirs()

val snippetsDir = "$buildDir/generated-snippets"
val publicReportsDir = "public"
val pitestReportsDir = "$publicReportsDir/pitest"

tasks.compileJava {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.suppressGeneratorTimestamp=true",
        "-Amapstruct.suppressGeneratorVersionInfoComment=true",
    ))
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
}

/*
 * All Tests
 */
tasks.withType<Test> {
    useJUnitPlatform()

    // faster start-up time
    jvmArgs("-noverify", "-XX:TieredStopAtLevel=2")

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
    pitestVersion.set("1.9.5")
    junit5PluginVersion.set("0.15")
    //** reproducible build
    timestampedReports.set(false)
    excludedClasses.set(setOf(mainClassName))
    mutationThreshold.set(100)
}

tasks.pitest {
    reportDir.set(file(pitestReportsDir))
}

/*
 * Test-Driven Documentation
 */
val docFilesMap = mapOf("README" to "index")

tasks.asciidoctor {
    dependsOn(tasks.test)

    configurations("asciidoctorExt")
    sourceDir("$projectDir/src/docs/asciidoc")
    inputs.dir(snippetsDir)
    setOutputDir(publicReportsDir)
    baseDirFollowsSourceDir()

    // My way to solve the issue https://github.com/spring-projects/spring-restdocs/issues/562
    attributes(mapOf("gradle-projectdir" to projectDir.path))

    //** reproducible build
    attributes(mapOf("reproducible" to ""))

    doLast {
        docFilesMap.forEach {
            file("$publicReportsDir/${it.key}.html")
                .renameTo(file("$publicReportsDir/${it.value}.html"))
        }
    }
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
            time = null
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
    extraDirectories {
        permissions.set(mapOf("/usr/local/bin/wait-for" to "755"))
    }
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

val upgradesToIgnore = listOf(
    "ch.qos.logback:*:>\${currentVersionMajor}.\${currentVersionMinor}",
    "com.h2database:h2:>\${currentVersionMajor}",
    "org.hibernate.validator:hibernate-validator:>\${currentVersionMajor}",
    "io.rest-assured:rest-assured:>\${currentVersionMajor}",
)

fun toModulePredicate(moduleSpec: String, currentVersion: String): Predicate<ModuleComponentIdentifier> {
    val (group, module, version) = moduleSpec.split(':')
    val currentVersionRange: String by lazy {
        val currentVersionObject = Version.fromString(currentVersion)
        StringSubstitutor.replace(
            version,
            mapOf(
                "currentVersionMajor" to currentVersionObject.major,
                "currentVersionMinor" to currentVersionObject.minor,
                "currentVersionPatch" to currentVersionObject.patch,
            )
        )
    }

    return Predicate {
        (it.group == group)
                && ((module == "*") || (it.module == module))
                && SemVer.satisfies(it.version, currentVersionRange)
    }
}

val nonStableRegex by lazy {
    "([\\.-](alpha|beta|ea|m|preview|rc)[\\.-]?\\d*(-groovy-\\d+\\.\\d+)?|-b\\d+\\.\\d+)$".toRegex(RegexOption.IGNORE_CASE)
}

fun isNonStable(version: String): Boolean {
    return nonStableRegex.containsMatchIn(version)
}


tasks.dependencyUpdates {
    rejectVersionIf {
        candidate.version.endsWith("-groovy-4.0")
                || isNonStable(candidate.version)
                || upgradesToIgnore.map { toModulePredicate(it, currentVersion) }
            .any { matcher -> matcher.test(candidate) }
    }
}

/*
 * Pitest shields.io JSON
 */
val redHue = 0.0
val greenHue = 120.0 / 360.0
val saturation = 0.9
val brightness = 0.9

// TODO: move to buildSrc
tasks.register("writePitestShieldsJson") {
    doLast {
        val doc: Document = Jsoup.parse(File("$pitestReportsDir/index.html"), "ASCII")
        // WARNING: highly dependent on Pitest version!
        // The HTML report is the only report that contains the overall result :-(
        val mutationCoverage = doc.select("html body table:first-of-type tbody tr td:last-of-type div div:last-of-type")[0].text()
        val values = mutationCoverage.split("/")
        val ratio = values[0].toFloat() / values[1].toFloat()
        val powerScale = HSB(redHue + ratio * greenHue, saturation, brightness)
        val json = buildShieldsJson("Pitest", mutationCoverage, powerScale)
        File("$pitestReportsDir/shields.json").writeText(json)

    }
}

tasks.pitest {
    finalizedBy("writePitestShieldsJson")
}

@Suppress("FunctionName")
fun HSBtoRGB(hsb: HSB): RGB {
    val h = (hsb.hue - floor(hsb.hue)) * 6.0
    val f = h - floor(h)
    val b = scaleTo255(hsb.brightness)
    val m = scaleTo255(hsb.brightness * (1.0 - hsb.saturation))
    val t = scaleTo255(hsb.brightness * (1.0 - hsb.saturation * (1.0 - f)))
    val q = scaleTo255(hsb.brightness * (1.0 - hsb.saturation * f))
    return when (h.toInt()) {
        0 -> RGB(b, t, m)
        1 -> RGB(q, b, m)
        2 -> RGB(m, b, t)
        3 -> RGB(m, q, b)
        4 -> RGB(t, m, b)
        else -> RGB(b, m, q)
    }
}

fun scaleTo255(value: Double) = (value * 255.0 + 0.5).toInt()

data class HSB(val hue: Double, val saturation: Double, val brightness: Double)

data class RGB(val red: Int, val green: Int, val blue: Int) {
    override fun toString(): String {
        return "#%02x%02x%02x".format(red, green, blue)
    }
}

// See https://shields.io/endpoint
fun buildShieldsJson(label: String, message: String, hsb: HSB) = """{
    "schemaVersion": 1,
    "label": "$label",
    "message": "$message",
    "color": "${HSBtoRGB(hsb)}"
}"""
