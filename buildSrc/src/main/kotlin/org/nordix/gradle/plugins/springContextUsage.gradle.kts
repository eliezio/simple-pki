package org.nordix.gradle.plugins

tasks.create("ctxstat") {
    val testReportDir = project.layout.buildDirectory.dir("test-results/test").get().asFile
    val jUnitXmlReports = fileTree(testReportDir) { include("*.xml") }
    inputs.files(jUnitXmlReports)
        .skipWhenEmpty()
        .withPropertyName("jUnitXmlReports")
    val reuseYamlReport = project.layout.buildDirectory.file("reports/ctxstat/application-contexts-reuse-report.yml").get().asFile
    outputs.file(reuseYamlReport)

    doLast {
        reportApplicationContextsReuse(jUnitXmlReports, reuseYamlReport)
    }
}

val TaskContainer.ctxstat: TaskProvider<Task>
    get() = named<Task>("ctxstat")
