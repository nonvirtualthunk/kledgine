package arx.display.core

import arx.core.*
import com.typesafe.config.ConfigValue
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageWrite
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer


interface ImageRef {
    fun toImage() : Image

    fun isSentinel() : Boolean

    companion object : FromConfigCreator<ImageRef> {
        override fun createFromConfig(cv: ConfigValue?): ImageRef {
            val str = cv.asStr()
            return if (str != null) {
                ImagePath(str)
            } else {
                SentinelImageRef
            }
        }
    }
}

data class ImagePath(val path: String) : ImageRef {
    private var img: Image? = null
    override fun toImage(): Image {
        if (img == null) {
            img = Resources.image(path)
        }
        return img!!
    }

    override fun isSentinel(): Boolean {
        return false
    }
}

object SentinelImageRef : ImageRef {
    val img = SentinelImage
    override fun toImage(): Image {
        return img
    }

    override fun isSentinel(): Boolean {
        return true
    }
}

object SentinelImage : Image() {
    init {
        val w = 64
        val h = 64
        data = ByteBuffer.allocate(w * h * 4)
        data = MemoryUtil.memCalloc(w*h*4)
        dimensions = Vec2i(w,h)
        ymult = w * 4
        withPixels { x, y, v -> if (x == 0 || y == 0 || x == 63 || y == 63) { v(0u,0u,0u,255u) } else { v((x*2).toUByte(), (y * 4).toUByte(), (x*2).toUByte(), 255u) } }
    }
    override fun isSentinel(): Boolean {
        return true
    }

    override fun toString(): String {
        return "SentinelImage"
    }
}

@Suppress("NOTHING_TO_INLINE")
open class Image internal constructor() : ImageRef {
    var data : ByteBuffer = ByteBuffer.allocate(0)
    var dimensions  = Vec2i(0,0)
    internal var ymult : Int  = 0
    inline val width get() = dimensions.x
    inline val height get() = dimensions.y
    var revision: Int = 1
    var path : String? = null
    var destroyed: Boolean = false

    private inline fun offset(x: Int, y: Int): Int = y * ymult + (x shl 2)

    override fun toImage(): Image {
        return this
    }

    override fun isSentinel(): Boolean {
        return false
    }

    fun pixelf(x: Int, y: Int) : Vec4f {
        data.position(offset(x,y))
        return Vec4f((data.get().toUByte()).toFloat() / 255.0f,
            (data.get().toUByte()).toFloat() / 255.0f,
            (data.get().toUByte()).toFloat() / 255.0f,
            (data.get().toUByte()).toFloat() / 255.0f)
    }

    fun pixel(x: Int, y: Int, v: Vec4f){
        data.position(offset(x,y))
        v.r = (data.get().toUByte()).toFloat() / 255.0f
        v.g = (data.get().toUByte()).toFloat() / 255.0f
        v.b = (data.get().toUByte()).toFloat() / 255.0f
        v.a = (data.get().toUByte()).toFloat() / 255.0f
    }

    fun pixel(x: Int, y: Int) : RGBA {
        data.position(offset(x,y))
        return RGBA(data.get().toUByte(), data.get().toUByte(), data.get().toUByte(), data.get().toUByte())
    }

    fun pixel(x: Int, y: Int, v: RGBA) {
        data.position(offset(x,y))
        v.r = data.get().toUByte()
        v.g = data.get().toUByte()
        v.b = data.get().toUByte()
        v.a = data.get().toUByte()
    }

    operator fun set(x : Int, y : Int, v: RGBA) {
        data.position(offset(x,y))
        data.put(v.r.toByte())
        data.put(v.g.toByte())
        data.put(v.b.toByte())
        data.put(v.a.toByte())
    }

    fun set(x : Int, y : Int, r : Int, g : Int, b : Int, a : Int) {
        data.position(offset(x,y))
        data.put(r.toByte())
        data.put(g.toByte())
        data.put(b.toByte())
        data.put(a.toByte())
    }

    operator fun set(x : Int, y : Int, q: Int, v: UByte) {
        data.put(offset(x,y) + q,v.toByte())
    }

    operator fun get(x: Int, y: Int): RGBA {
        return pixel(x,y)
    }

    fun getClamped(x: Int, y : Int) : RGBA {
        return get(x.clamp(0,width - 1), y.clamp(0, height - 1))
    }

    fun sample(x: Int, y: Int, s: Int) : UByte {
        return data.get(offset(x,y) + s).toUByte()
    }

    fun copyFrom(src: Image, targetPos: Vec2i) {
        src.data.position(0)
        this.data.position(0)
        for (dy in 0 until src.height) {
            val srcAddr = MemoryUtil.memAddress(src.data, src.offset(0, dy))
            val tgtAddr = MemoryUtil.memAddress(this.data, this.offset(targetPos.x, targetPos.y + dy))
            MemoryUtil.memCopy(srcAddr, tgtAddr, src.width * 4L)
        }
    }

    fun withPixels(fn: (Int,Int,RGBA) -> Unit) : Image {
        data.position(0)
        val v = RGBA(0u,0u,0u,0u)
        for (y in 0 until height) {
            for (x in 0 until width) {
                fn(x,y,v)
                data.put(v.r.toByte())
                data.put(v.g.toByte())
                data.put(v.b.toByte())
                data.put(v.a.toByte())
            }
        }
        return this
    }

    fun transformPixels(fn: (Int, Int, RGBA) -> Unit) : Image {
        data.position(0)
        val v = RGBA(0u,0u,0u,0u)
        for (y in 0 until height) {
            for (x in 0 until width) {
                v(data.get().toUByte(), data.get().toUByte(), data.get().toUByte(), data.get().toUByte())
                data.position(offset(x, y))
                fn(x,y,v)
                data.put(v.r.toByte())
                data.put(v.g.toByte())
                data.put(v.b.toByte())
                data.put(v.a.toByte())
            }
        }
        return this
    }

    fun map(fn: (Int, Int, RGBA) -> Unit) : Image {
        val out = ofSize(width, height)
        out.data.position(0)
        data.position(0)
        val v = RGBA(0u,0u,0u,0u)
        for (y in 0 until height) {
            for (x in 0 until width) {
                v(data.get().toUByte(), data.get().toUByte(), data.get().toUByte(), data.get().toUByte())
                fn(x,y,v)
                out.data.put(v.r.toByte())
                out.data.put(v.g.toByte())
                out.data.put(v.b.toByte())
                out.data.put(v.a.toByte())
            }
        }
        return out
    }

    fun forEachPixel(fn : (Int, Int, RGBA) -> Unit) {
        data.position(0)
        val v = RGBA(0u,0u,0u,0u)
        for (y in 0 until height) {
            for (x in 0 until width) {
                v(data.get().toUByte(), data.get().toUByte(), data.get().toUByte(), data.get().toUByte())
                fn(x,y,v)
            }
        }
    }

    fun writeToFile(path: String) {
        data.position(0)
        STBImageWrite.stbi_flip_vertically_on_write(true)
        STBImageWrite.stbi_write_png(path, width, height, 4, data, 4*width)
    }

    fun destroy() {
        MemoryUtil.memFree(data)
        destroyed = true
    }

    override fun toString(): String {
        return if (path != null) {
            "Image($path)"
        } else {
            "Image(${width}x${height})"
        }
    }

    companion object {
        val ImageLoadingTimer = Metrics.timer("ImageLoading")

        fun load(path: String): Image {
            return ImageLoadingTimer.timeStmt {
                MemoryStack.stackPush().use { stack ->
                    val width = stack.callocInt(1)
                    val height = stack.callocInt(1)
                    val channels = stack.callocInt(1)
                    STBImage.nstbi_set_flip_vertically_on_load(1)
                    val buff = STBImage.stbi_load(path, width, height, channels, 4)

                    if (buff == null) {
                        System.err.println("Could not load image: $path")
                        SentinelImageRef.img
                    } else {
                        val img = Image()
                        img.data = buff
                        img.dimensions = Vec2i(width.get(0), height.get(0))
                        img.ymult = img.dimensions.x * 4

                        img
                    }
                }
            }
        }

        fun ofSize(w: Int, h: Int) : Image {
            val img = Image()
            img.data = MemoryUtil.memCalloc(w*h*4)
            for (i in 0 until w * h) {
                img.data.put(-1)
                img.data.put(-1)
                img.data.put(-1)
                img.data.put(0)
            }
            img.dimensions = Vec2i(w,h)
            img.ymult = w * 4

            return img
        }

        fun ofSize(v : Vec2i) : Image {
            return ofSize(v.x, v.y)
        }

        fun copy(other : Image) : Image {
            val img = ofSize(other.width, other.height)
            img.copyFrom(other, Vec2i(0,0))
            return img
        }
    }
}

/*
    Vec4ub(227,221,211,255)

    // Temporarily abandoned lower level reading of images, stbi seems to work just as well at present
    // though it may not give direct access to the raw bytes? I'm not sure it would be any different though

            val pngReader = ImageIO.getImageReadersBySuffix("png").next()
            val raster = pngReader.readRaster(0, pngReader.defaultReadParam)
            (raster.dataBuffer as DataBufferByte).data

            img.data = ByteBuffer.allocateDirect(raster.width * raster.height * 4).order(ByteOrder.nativeOrder())
            var ri = 0
            for (y in raster.height - 1 downTo 0) {
                for (x in 0 until raster.width) {
                    raster.getPix
                    img.data.put(raster.getSample(x,y,0))
                }
            }
 */

/*
        fun parseBGRA(bgra : Int, target: ByteBuffer) {
            target.put((bgra shr 16) and 0x00ff)
            target.put((bgra shr 8) and 0x00ff)
            target.put((bgra shr 0) and 0x00ff)
            target.put((bgra shr 24) and 0x00ff)
        }


        fun load(stream: InputStream) : Image {
            val img = Image()

            val src = ImageIO.read(stream)
            img.data = ByteBuffer.allocateDirect(src.width * src.height * 4).order(ByteOrder.nativeOrder())
            img.dimensions = Vec2i(src.width, src.height)
            img.ymult = src.width * 4


            for (y in src.height - 1 downTo 0) {
                for (x in 0 until src.width) {
                    parseBGRA(src.getRGB(x,y), img.data)
                }
            }

            return img
        }

        private fun ByteBuffer.put(i: Int) {
            put(i.toByte())

        }
 */