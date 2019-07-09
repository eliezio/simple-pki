import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.math.floor

buildscript {
    val restdocsApiSpecVersion: String by project

    dependencies {
        classpath("com.epages", "restdocs-api-spec-gradle-plugin", restdocsApiSpecVersion)
        classpath("org.jsoup", "jsoup", "1.12.1")
    }
}

plugins {
    java
    groovy
    jacoco

    id("org.springframework.boot") version "2.1.6.RELEASE"

    id("nebula.release") version "9.2.0"

    id("info.solidsoft.pitest") version "1.4.0"

    // Quality / Documentation Plugins
    id("org.sonarqube") version "2.7.1"
    id("com.adarshr.test-logger") version "1.7.0"
    id("com.github.ksoichiro.console.reporter") version "0.6.2"
    // NOTE: version 1.5.10 and above are incompatible with GKD and/or Nebula Release plugin
    id("org.asciidoctor.convert") version "1.5.6"

    id("com.google.cloud.tools.jib") version "1.3.0"
}

apply(plugin = "io.spring.dependency-management")
apply(plugin = "com.epages.restdocs-api-spec")

description = "Simple-PKI API"
group = "org.nordix"
val defaultDockerGroup = "ebo"
val dockerGroup: String? by project
val scmGroup = "eliezio"
val scmProject = "simple-pki"

val mainClassName = "org.nordix.simplepki.Application"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val restdocsApiSpecVersion: String by project

val bouncyCastleVersion = "1.61"
val spockFrameworkVersion = "1.3-groovy-2.5"
val spockReportsVersion = "1.6.2"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.projectlombok", "lombok")

    asciidoctor("org.springframework.restdocs", "spring-restdocs-asciidoctor")

    implementation("javax.inject", "javax.inject", "1")
    implementation("org.springframework.boot", "spring-boot-starter")
    implementation("org.springframework.boot", "spring-boot-starter-web") {
        exclude(module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot", "spring-boot-starter-undertow")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")

    implementation("org.bouncycastle", "bcprov-jdk15on", bouncyCastleVersion)
    implementation("org.bouncycastle", "bcpkix-jdk15on", bouncyCastleVersion)

    runtimeOnly("com.h2database", "h2")
    runtimeOnly("mysql", "mysql-connector-java")

    testImplementation("org.springframework.boot", "spring-boot-starter-test")
    testImplementation("org.spockframework", "spock-core", spockFrameworkVersion)
    testImplementation("org.spockframework", "spock-spring", spockFrameworkVersion)
    testImplementation("org.springframework.restdocs", "spring-restdocs-mockmvc")
    testImplementation("com.epages", "restdocs-api-spec-mockmvc", restdocsApiSpecVersion)
    testImplementation("ch.qos.logback", "logback-classic")

    testRuntimeOnly("com.athaydes", "spock-reports", spockReportsVersion)
}

val docsDir = "$buildDir/resources/main/static"
file(docsDir).mkdirs()

val snippetsDir = "$buildDir/generated-snippets"
val publicReportsDir = "public"
val spockReportsDir = "$publicReportsDir/spock"
val jacocoHtmlReportsDir = "$publicReportsDir/jacoco"
val pitestReportsDir = "$publicReportsDir/pitest"

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
    // faster start-up time
    jvmArgs("-noverify", "-XX:TieredStopAtLevel=2")
    systemProperty("com.athaydes.spockframework.report.outputDir", spockReportsDir)

    // register this extra output dir
    outputs.dir(snippetsDir)
}

/*
 * JaCoCo
 */
// Why we can't use just "task.jacocoTestReport": https://github.com/gradle/kotlin-dsl/issues/1176#issuecomment-435816812
tasks.getByName<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>().asIterable())

    reports {
        html.isEnabled = true
        html.destination = file(jacocoHtmlReportsDir)
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it) {
                exclude("**/Application.class")
            }
        }))
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    executionData(tasks.jacocoTestReport.get().executionData)

    violationRules {
        rule {
            element = "CLASS"
            excludes = listOf(mainClassName)
            limit {
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.reportCoverage {
    dependsOn(tasks.jacocoTestReport)
}

/*
 * Pitest
 */
pitest {
    pitestVersion.set("1.4.9")
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
    sourceDir = projectDir
    sources(delegateClosureOf<PatternSet> {
        include(docFilesMap.keys.map { "$it.adoc" })
    })
    separateOutputDirs = false
    outputDir = file(publicReportsDir)

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

fun File.removeLines(predicate: (String) -> Boolean) {
    val lines = readLines().filterNot(predicate)
    printWriter().use { out ->
        lines.forEach { out.println(it) }
    }
}

springBoot {
    // to create META-INF/build-info.properties. Its contents are exported by /info
    buildInfo {
        properties {
            //** reproducible build
            time = null
        }

        //** reproducible build
        // Watch https://github.com/spring-projects/spring-boot/issues/14494
        doLast {
            File(destinationDir, "build-info.properties")
                .removeLines { it.startsWith("#") }
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
        // Smaller than the default gcr/distroless/java
        image = "openjdk:8-jre-alpine"
    }
    to {
        val tagVersion = version.toString().substringBefore('-')
        image = listOfNotNull(dockerRegistry, dockerGroup ?: defaultDockerGroup, "${project.name}:$tagVersion")
            .joinToString("/")
    }
    extraDirectories {
        permissions = mapOf("/usr/local/bin/wait-for" to "755")
    }
    container {
        jvmFlags = listOf(
                "-noverify",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseCGroupMemoryLimitForHeap",
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
 * Pitest shields.io JSON
 */
val redHue = 0.0
val greenHue = 120.0 / 360.0
val saturation = 0.9
val brightness = 0.9

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
