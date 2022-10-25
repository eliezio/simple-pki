import org.jsoup.Jsoup
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

buildscript {
    repositories.addAll(rootProject.buildscript.repositories)
    dependencies {
        classpath("org.jsoup", "jsoup", "1.15.3")
    }
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
        val pitestReportsDir = "${rootProject.buildDir}/reports/pitest"
        val doc = Jsoup.parse(file("${pitestReportsDir}/index.html"), "ASCII")
        // WARNING: highly dependent on Pitest version!
        // The HTML report is the only report that contains the overall result :-(
        val mutationCoverageStats = doc.select("html body table:first-of-type tbody tr td:eq(2) div div:last-of-type")[0].text()
        val (killed, total) = mutationCoverageStats.split("/").map { it.toInt() }
        val ratio = killed / total.toDouble()
        val ratioPercentage = if (killed == total) 100 else (ratio * 100).roundToInt().coerceAtMost(99)
        val powerScale = HSB(redHue + ratio * greenHue, saturation, brightness)
        val json = buildShieldsJson("Pitest", "$ratioPercentage% ($mutationCoverageStats)", powerScale)
        java.io.File("$pitestReportsDir/shields.json").writeText(json)
        println("Mutation Coverage: %.1f%%".format(ratio * 100))
    }
}

// See https://shields.io/endpoint
fun buildShieldsJson(label: String, message: String, hsb: HSB) = """{
    "schemaVersion": 1,
    "label": "$label",
    "message": "$message",
    "color": "${HSBtoRGB(hsb)}"
}"""

data class HSB(val hue: Double, val saturation: Double, val brightness: Double)

data class RGB(val red: Double, val green: Double, val blue: Double) {
    private fun scaleTo255(value: Double) = (value * 255.0).roundToInt()

    override fun toString(): String {
        return "#%02x%02x%02x".format(scaleTo255(red), scaleTo255(green), scaleTo255(blue))
    }
}

@Suppress("FunctionName")
// Based on java.aws.Color::HSBtoRGB()
fun HSBtoRGB(hsb: HSB): RGB {
    if (hsb.saturation < 0.00001f) {
        return RGB(hsb.brightness, hsb.brightness, hsb.brightness)
    } else {
        val h = (hsb.hue - floor(hsb.hue)) * 6.0
        val f = h - floor(h)
        val b = hsb.brightness
        val p = hsb.brightness * (1.0 - hsb.saturation)
        val q = hsb.brightness * (1.0 - hsb.saturation * f)
        val t = hsb.brightness * (1.0 - hsb.saturation * (1.0 - f))
        return when (h.toInt()) {
            0 -> RGB(b, t, p)
            1 -> RGB(q, b, p)
            2 -> RGB(p, b, t)
            3 -> RGB(p, q, b)
            4 -> RGB(t, p, b)
            else -> RGB(b, p, q)
        }
    }
}
