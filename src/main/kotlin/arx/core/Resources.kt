package arx.core

import arx.display.core.Image
import arx.display.core.Shader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

object Resources {
    val projectSuffix = System.getProperty("projectName")?.let { "$it/" } ?: ""
    val baseAssetPath = System.getProperty("assetPath") ?: "src/main/resources/"
    val assetPath = baseAssetPath + projectSuffix
    val arxAssetPath = baseAssetPath + "arx/"
    private var images = ConcurrentHashMap<String, Image>()
    private var shaders = ConcurrentHashMap<String, Shader>()
    private var typefaces = ConcurrentHashMap<String, ArxTypeface>()
    private var configs = ConcurrentHashMap<String, Config>()


    private fun pickPath(str: String): String {
        return if (str.startsWith("/") && Files.exists(Path.of(str))) {
            str
        } else if (str.startsWith("/") && Files.exists(Path.of(str.substring(1)))) {
            str.substring(1)
        } else if (Files.exists(Path.of(assetPath + str))) {
            assetPath + str
        } else if (Files.exists(Path.of(baseAssetPath + str))) {
            baseAssetPath + str
        } else if (Files.exists(Path.of(arxAssetPath + str))) {
            arxAssetPath + str
        } else {
            assetPath + str
        }
    }

    fun image(str: String): Image = images.getOrPut(str) { Image.load(pickPath(str)) }
    fun shader(str: String): Shader = shaders.getOrPut(str) {
        Shader(pickPath("$str.vertex").substringBefore(".vertex"))
    }
    fun typeface(str: String): ArxTypeface = typefaces.getOrPut(str) { ArxTypeface.load(pickPath(str)) }
    fun font(str: String, size: Int): ArxFont {
        return typeface(str).withSize(size)
    }

    fun config(str: String): Config {
        return configs.getOrPut(str) {
            val f = File(pickPath(str))
            ConfigFactory.parseFile(f).resolve()
        }
    }

    fun inputStream(path: String): InputStream = FileInputStream(pickPath(path))
}