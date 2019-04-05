buildscript {
    val restdocsApiSpecVersion: String by project

    dependencies {
        classpath("com.epages", "restdocs-api-spec-gradle-plugin", restdocsApiSpecVersion)
    }
}

plugins {
    java
    groovy
    jacoco

    id("nebula.project").version("6.0.3")
    id("nebula.integtest").version("6.0.3")

    id("org.springframework.boot").version("2.1.4.RELEASE")

    id("nebula.release").version("9.2.0")

    id("info.solidsoft.pitest").version("1.4.0")

    // Quality / Documentation Plugins
    id("com.adarshr.test-logger").version("1.6.0")
    id("com.github.ksoichiro.console.reporter").version("0.6.2")
    // NOTE: version 1.5.10 and above are incompatible with GKD and/or Nebula Release plugin
    id("org.asciidoctor.convert").version("1.5.6")

    id("com.google.cloud.tools.jib").version("1.2.0")
}

apply(plugin = "io.spring.dependency-management")
apply(plugin = "com.epages.restdocs-api-spec")

description = "Simple-PKI API"
group = "org.nordix"
val dockerGroup = "ebo"

val mainClassName = "org.nordix.simplepki.Application"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    compile {
        exclude(module = "spring-boot-starter-tomcat")
    }
}

val restdocsApiSpecVersion: String by project

val bouncyCastleVersion = "1.61"
val spockFrameworkVersion = "1.3-groovy-2.5"
val spockReportsVersion = "1.6.2"

repositories {
    jcenter()
}

dependencies {
    annotationProcessor("org.projectlombok", "lombok")

    asciidoctor("org.springframework.restdocs", "spring-restdocs-asciidoctor")

    implementation("javax.inject", "javax.inject", "1")
    implementation("org.springframework.boot", "spring-boot-starter")
    implementation("org.springframework.boot", "spring-boot-starter-web")
    implementation("org.springframework.boot", "spring-boot-starter-undertow")
    implementation("org.springframework.boot", "spring-boot-starter-data-jpa")

    implementation("org.bouncycastle", "bcprov-jdk15on", bouncyCastleVersion)
    implementation("org.bouncycastle", "bcpkix-jdk15on", bouncyCastleVersion)

    runtime("com.h2database", "h2")

    testImplementation("org.spockframework", "spock-core", spockFrameworkVersion)

    testRuntime("com.athaydes", "spock-reports", spockReportsVersion)

    integTestImplementation("org.springframework.boot", "spring-boot-starter-test")
    integTestImplementation("org.spockframework", "spock-spring", spockFrameworkVersion)
    integTestImplementation("org.springframework.restdocs", "spring-restdocs-mockmvc")
    integTestImplementation("com.epages", "restdocs-api-spec-mockmvc", restdocsApiSpecVersion)
    integTestImplementation("ch.qos.logback", "logback-classic")
}

val docsDir = "$buildDir/resources/main/static"
file(docsDir).mkdirs()

val snippetsDir = "$buildDir/generated-snippets"
val publicReportsDir = "public"
val spockReportsDir = "$publicReportsDir/spock"
val jacocoHtmlReportsDir = "$publicReportsDir/jacoco"
val pitestReportsDir = "$publicReportsDir/pitest"

/*
 * Reproducible Build
 */
tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

infoBroker {
    excludedManifestProperties = listOf("Build-Date", "Built-OS", "Built-By", "Build-Host")
}

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
            time = null
        }

        // Watch https://github.com/spring-projects/spring-boot/issues/14494
        doLast {
            File(destinationDir, "build-info.properties")
                    .removeLines { it.startsWith("#") }
        }
    }
}

tasks.asciidoctor {
    attributes(mapOf("reproducible" to ""))
}

/*
 * All Tests
 */
tasks.withType<Test> {
    jvmArgs("-noverify")
}

/*
 * Integration Tests
 */
tasks.integrationTest {
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
    executionData(file("$buildDir/jacoco/test.exec"), file("$buildDir/jacoco/integrationTest.exec"))

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

/*
 * Pitest
 */
pitest {
    pitestVersion = "1.4.7"
    timestampedReports = false
    testSourceSets = setOf(sourceSets.test.get(), sourceSets["integTest"])
    excludedClasses = setOf(mainClassName)
    mutationThreshold = 100
}

tasks.pitest {
    reportDir = file(pitestReportsDir)
}

/*
 * Test-Driven Documentation
 */
tasks.asciidoctor {
    separateOutputDirs = false
    outputDir = file(docsDir)

    attributes(mapOf("project-version" to version.toString()))
}

configure<com.epages.restdocs.apispec.gradle.OpenApi3Extension> {
    outputDirectory = docsDir
    setServer("http://localhost:8080")
    title = project.description!!
    version = project.version.toString()
}

/*
 * Docker Image
 */
jib {
    from {
        // Smaller than the default gcr/distroless/java
        image = "openjdk:8-jre-alpine"
    }
    to {
        val tagVersion = version.toString().substringBefore('-')
        image = "$dockerGroup/${project.name}:$tagVersion"
    }
    extraDirectories {
        permissions = mapOf("/usr/local/bin/wait-for" to "755")
    }
    container {
        jvmFlags = listOf(
                "-noverify", "-XX:TieredStopAtLevel=2",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseCGroupMemoryLimitForHeap",
                // See http://www.thezonemanager.com/2015/07/whats-so-special-about-devurandom.html
                "-Djava.security.egd=file:/dev/./urandom"
        )
        ports = listOf("8080")
        volumes = listOf("/data")
    }
}

/*
 * Improve Tasks Dependencies
 */
tasks {

    jacocoTestReport {
        dependsOn(withType<Test>().asIterable())
    }

    jacocoTestCoverageVerification {
        dependsOn(jacocoTestReport)
    }

    reportCoverage {
        dependsOn(jacocoTestReport)
    }

    // redundant since check already depends on reportCoverage
    check {
        dependsOn(jacocoTestReport, jacocoTestCoverageVerification)
    }

    asciidoctor {
        dependsOn(integrationTest)
    }

    bootJar {
        dependsOn(asciidoctor, "openapi3")
    }

    jibDockerBuild {
        dependsOn(build)
    }
}
