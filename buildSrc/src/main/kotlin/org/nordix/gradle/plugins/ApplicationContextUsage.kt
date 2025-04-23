@file:Suppress("SameParameterValue")

package org.nordix.gradle.plugins

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import org.gradle.api.file.FileTree
import java.io.File
import kotlin.math.roundToInt

private val yamlMapper: YAMLMapper = YAMLMapper().apply {
    setSerializationInclusion(Include.NON_NULL)
    enable(Feature.MINIMIZE_QUOTES)
    enable(Feature.INDENT_ARRAYS_WITH_INDICATOR)
    disable(Feature.SPLIT_LINES)
}

private val CONTEXT_CACHE_ACCESS_REGEX =
    ("(?<action>Storing|Retrieved) ApplicationContext \\[(?<hashCode>\\d+)] (?:from|in) cache (?:with|under) key" +
        " \\[(?<mccId>\\w*MergedContextConfiguration@[0-9a-f]{4,8})" +
        " testClass = (?<topLevelTestClass>([a-z][_a-z0-9]*\\.)+[A-Z][_a-zA-Z0-9]*)(\\.[_a-zA-Z][_a-zA-Z0-9]*)*, (?<configuration>.*)]$").toRegex()
private val NESTED_CONTEXT_CUSTOMIZER_REGEX = "\\[(ImportsContextCustomizer@[0-9a-f]{4,8}) key: (\\[.*])]".toRegex()
private val SPRING_ANNOTATION_REGEX = "(?<!\\w)@[\\w.]+\\([^)]*\\)".toRegex()
private val MOCKITO_BEANS_INFO_REGEX = ("Mockito beans used by" +
    " (?<topLevelTestClass>([a-z][_a-z0-9]*\\.)+[A-Z][_a-zA-Z0-9]*)(\\.[_a-zA-Z][_a-zA-Z0-9]*)*: (?<properties>\\{.+})$").toRegex()

private val ID_SUFFIX = "@[0-9a-f]+$".toRegex()

private val ANCHOR_REGEX = "^ *- hashCode: (\\d{4,})$".toRegex()

typealias Properties = Map<String, Any>

private val propertiesTypeReference = object : TypeReference<Properties>() {}

data class KeyWithLineReference<T>(
    val key: T,
    val similarity: Int,
    var line: Int? = null
) {
    @JsonValue
    fun toJson(): String {
        val sb = StringBuilder(key.toString())
        sb.append(" !similarity $similarity%%")
        line?.also { sb.append(" !line $it") }
        return sb.toString()
    }
}

@JsonPropertyOrder("hashCode", "useCounter", "similarTo", "topLevelTestClasses", "contextConfiguration")
class ApplicationContextUsage(
    val hashCode: Int,
    @get:JsonGetter
    val contextConfiguration: Properties
) {
    val topLevelTestClasses = mutableSetOf<String>()
    val useCounter: Int
        get() = topLevelTestClasses.size

    @JsonIgnore
    val abstractContextConfiguration = contextConfiguration.toAbstract()
    val similarTo = mutableListOf<KeyWithLineReference<Int>>()
    var mockitoBeans: Properties? = null

    @JsonIgnore
    val mockitoBeanNames = mutableSetOf<String>()
}

private fun String.toAbstract(): String = this.replace(ID_SUFFIX, "")

private fun Any?.toAbstract(): Any? {
    return when (this) {
        is String -> this.toAbstract()
        is List<*> -> this.map { it.toAbstract() }
        is Map<*, *> -> this.entries.associate { it.key.toAbstract() to it.value.toAbstract() }
        else -> this
    }
}

fun reportApplicationContextsReuse(jUnitXmlReports: FileTree, reuseYamlReport: File) {
    val usagesMap = mutableMapOf<Int, ApplicationContextUsage>()
    val mockitoBeansPerClass = mutableMapOf<String, Properties>()

    jUnitXmlReports.forEach { file ->
        file.forEachLine { line ->
            CONTEXT_CACHE_ACCESS_REGEX.find(line)?.let { matchResult ->
                val hashCode = matchResult.groups["hashCode"]!!.value.toInt()
                val topLevelTestClass = matchResult.groups["topLevelTestClass"]!!.value
                val configuration = matchResult.groups["configuration"]!!.value
                usagesMap.computeIfAbsent(hashCode) { hc ->
                    ApplicationContextUsage(hc, configurationToProperties(configuration))
                }.topLevelTestClasses.add(topLevelTestClass)
            } ?: MOCKITO_BEANS_INFO_REGEX.find(line)?.let { matchResult ->
                val topLevelTestClass = matchResult.groups["topLevelTestClass"]!!.value
                val properties = matchResult.groups["properties"]!!.value
                mockitoBeansPerClass.computeIfAbsent(topLevelTestClass) { _ ->
                    propertiesToMockitoBeans(properties)
                }
            }
        }
    }

    val usagesList = usagesMap.values
        .sortedWith(compareBy(ApplicationContextUsage::useCounter).then(compareBy { it.topLevelTestClasses.first() }))
        .onEach { usage ->
            usage.mockitoBeans = mockitoBeansPerClass[usage.topLevelTestClasses.first()]
            usage.mockitoBeans?.let { beans ->
                beans.values.filterIsInstance<List<*>>().forEach { list ->
                    list.mapTo(usage.mockitoBeanNames) { it.toString() }
                }
            }
        }

    findSimilarConfigurations(usagesList)

    val draftYaml = yamlMapper.writeValueAsString(usagesList)
    val references: Map<Int, Int> = indexYamlDocument(draftYaml)
    patchLineReferences(usagesList, references)

    val finalYaml = yamlMapper.writeValueAsString(usagesList)
    reuseYamlReport.also {
        it.parentFile.mkdirs()
    }.writeText(finalYaml)
}

private fun configurationToProperties(text: String): Properties {
    val keyAsYaml = text.replace(" = ", ": ")
        .replace(NESTED_CONTEXT_CUSTOMIZER_REGEX) { matchResult ->
            "{ ${matchResult.groupValues[1]}: ${matchResult.groupValues[2]} }"
        }
        .replace(SPRING_ANNOTATION_REGEX) { matchResult ->
            "'${matchResult.value}'"
        }
    return try {
        yamlMapper.readValue("{ $keyAsYaml }", propertiesTypeReference)
    } catch (e: Exception) {
        System.err.println("Failed to parse a supposable YAML document:")
        System.err.println(keyAsYaml)
        throw e
    }
}

private fun propertiesToMockitoBeans(text: String): Properties {
    return try {
        yamlMapper.readValue(text, propertiesTypeReference)
    } catch (e: Exception) {
        System.err.println("Failed to parse a supposable YAML document:")
        System.err.println(text)
        throw e
    }
}

private fun findSimilarConfigurations(usagesList: List<ApplicationContextUsage>) {
    for (index in 1 until usagesList.size) {
        usagesList.zip(usagesList.drop(index)).forEach { (usage1, usage2) ->
            if (usage1.abstractContextConfiguration == usage2.abstractContextConfiguration) {
                val similarity = calculateSimilarity(usage1.mockitoBeanNames, usage2.mockitoBeanNames)
                usage1.similarTo.add(KeyWithLineReference(usage2.hashCode, similarity))
                usage2.similarTo.add(KeyWithLineReference(usage1.hashCode, similarity))
            }
        }
    }
}

private fun calculateSimilarity(mockitoBeanNames1: Set<String>, mockitoBeanNames2: Set<String>): Int {
    if (mockitoBeanNames1.isEmpty() && mockitoBeanNames2.isEmpty()) return 100
    val intersectSize = mockitoBeanNames1.intersect(mockitoBeanNames2).size
    return ((intersectSize * 200.0) / (mockitoBeanNames1.size + mockitoBeanNames2.size)).roundToInt()
}

private fun patchLineReferences(usagesList: List<ApplicationContextUsage>, references: Map<Int, Int>) {
    usagesList.onEach { usage ->
        usage.similarTo.onEach { it.line = references[it.key] }
            .sortBy { it.line }
    }
}

private fun indexYamlDocument(text: String): Map<Int, Int> {
    val anchors = mutableMapOf<Int, Int>()
    val lines = text.lines()
    lines.forEachIndexed { index, line ->
        ANCHOR_REGEX.find(line)?.let { matchResult ->
            anchors[matchResult.groups[1]!!.value.toInt()] = index + 1
        }
    }
    return anchors
}
