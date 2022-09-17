package arx.core

import arx.display.core.Image
import arx.display.core.Shader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

object Resources {
    var assetPath = System.getProperty("assetPath") ?: "src/main/resources/"
    private var images = ConcurrentHashMap<String, Image>()
    private var shaders = ConcurrentHashMap<String, Shader>()
    private var typefaces = ConcurrentHashMap<String, ArxTypeface>()
    private var configs = ConcurrentHashMap<String, Config>()


    fun image(str: String) : Image = images.getOrPut(str) { Image.load(assetPath + str) }
    fun shader(str: String) : Shader = shaders.getOrPut(str) { Shader(assetPath + str) }
    fun typeface(str: String) : ArxTypeface = typefaces.getOrPut(str) { ArxTypeface.load(assetPath + str) }
    fun font(str: String, size: Int): ArxFont {
         return typeface(str).withSize(size)
    }
    fun config(str: String) : Config {
        return configs.getOrPut(str) { ConfigFactory.parseFile(File(assetPath + str)) }
    }

    fun inputStream(path: String): InputStream = FileInputStream(assetPath + path)
}