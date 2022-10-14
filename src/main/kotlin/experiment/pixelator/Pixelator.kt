package experiment.pixelator

import arx.core.*
import arx.display.core.Image
import java.io.File
import java.lang.Math.max
import java.lang.Math.pow
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*

fun main() {
    Pixelator.pixelate(Pixelator.Params(
        image = Image.load("/Users/sbock/Downloads/ai/fire_pillar_1.png"),
        paletteSize = 32,
        targetSize = 64
    )).apply {
        val path = "/tmp/pixelator/"
        val dir = File(path)
        dir.listFiles()?.filter { it.extension == "png" }?.forEach { it.delete() }
        dir.mkdir()
        for ((i,v) in stages.withIndex()) {
            val (stageName, stageImage) = v
            stageImage.writeToFile("$path${i}_$stageName.png")
        }
        finalImage.writeToFile("${path}${stages.size}_final.png")
    }
}

object Pixelator {
    const val doGammaCorrection = false

    data class Params(
        var image : Image,
        var paletteSize : Int,
        var targetSize : Int,
    )

    data class Result(
        val stages : List<Pair<String, Image>>,
        val finalImage : Image
    )

    class Kernel(val size : Int) {
        val data = Array(size*size) { RGBA() }

        operator fun get(x : Int, y : Int) : RGBA {
            return data[y * size + x]
        }
    }


    fun Image.forKernels(scale : Int, fn : (Int, Int, Kernel) -> Unit) {
        val kernel = Kernel(scale)

        for (x in 0 until width / scale) {
            for (y in 0 until height / scale) {
                val ax = x * scale
                val ay = y * scale
                for (dx in 0 until scale) {
                    for (dy in 0 until scale) {
                        pixel(ax + dx, ay + dy, kernel[dx, dy])
                    }
                }
                fn(x, y, kernel)
            }
        }
    }

    fun Image.forSlidingWindow(windowSize : Int, clamped : Boolean, fn : (Int, Int, Kernel) -> Unit) {
        val kernel = Kernel(windowSize)

        val koffset = if (clamped) { windowSize / 2 } else { 0 }
        val woffset = if (clamped) { 0 } else { windowSize - 1 }
        for (x in 0 until width - woffset) {
            for (y in 0 until height - woffset) {
                for (dx in 0 until windowSize) {
                    for (dy in 0 until windowSize) {
                        pixel((x + dx - koffset).clamp(0, width - 1), (y + dy - koffset).clamp(0, height - 1), kernel[dx, dy])
                    }
                }
                fn(x, y, kernel)
            }
        }
    }

    fun gammaCorrect(v : RGBA) {
        if (doGammaCorrection) {
            v.r = (pow(v.r.toDouble() / 255.0, 2.2) * 255.0).toInt().toUByte()
            v.g = (pow(v.g.toDouble() / 255.0, 2.2) * 255.0).toInt().toUByte()
            v.b = (pow(v.b.toDouble() / 255.0, 2.2) * 255.0).toInt().toUByte()
        }
    }

    fun gammaUnCorrect(v : RGBA) {
        if (doGammaCorrection) {
            v.r = (pow(v.r.toDouble() / 255.0, 1.0 / 2.2) * 255.0).toInt().toUByte()
            v.g = (pow(v.g.toDouble() / 255.0, 1.0 / 2.2) * 255.0).toInt().toUByte()
            v.b = (pow(v.b.toDouble() / 255.0, 1.0 / 2.2) * 255.0).toInt().toUByte()
        }
    }

    fun pixelate(params : Params) : Result {
        with(params) {
            val stages = mutableListOf<Pair<String, Image>>()
            val outDims = chooseResultDimensions()
            val scale = min(largestWholeRatio(outDims), 8)
            val srcImg = if ((image.width - outDims.x * scale) > 1) {
                Noto.info("Resizing input image from ${image.width} to ${outDims * scale}")
                bilinearDownscale(image, outDims * scale)
            } else {
                image
            }
            stages += "Source" to srcImg.map { _,_,v -> v }

            srcImg.transformPixels { x,y,v -> gammaCorrect(v) }

            val palette = extractKMeansPalette(srcImg, FiniteGrid2D(srcImg.dimensions, 1), paletteSize, 15)

            val kMeansImage = Image.ofSize(outDims.x, outDims.y)
            val error = Array(paletteSize) { 0L }
            srcImg.forKernels(scale) { x,y,kernel ->
                for (i in error.indices) {
                    error[i] = 0L
                }

                for (c in kernel.data) {
                    for (i in 0 until paletteSize) {
                        error[i] += colorDistance2(palette[i], c).toLong()
                    }
                }

                kMeansImage[x,y] = palette[error.withIndex().minBy { v -> v.value }.index]
            }

            val sortedPalette = palette.sortedBy { it.r }
            stages += "ReducedPaletteImage" to kMeansImage.map { _,_,v -> gammaUnCorrect(v) }

            val contrastGradients = computeContrastGradients2(srcImg)
            val contrastImg = contrastGradientsToImage(contrastGradients)

            stages += "ContrastGradients" to contrastImg

            val reducedPaletteContrast = contrastGradientsToImage(computeContrastGradients2(kMeansImage))
            stages += "ReducedPaletteContrast" to reducedPaletteContrast


            val downscaledContrast = contrastImg.downscaleBy(outDims) { kernel, v ->
                v(0u,0u,0u,255u)
                kernel.data.forEach { d -> max(d, v, v) }
            }
            stages += "DownscaledContrast" to downscaledContrast

            // take the difference between the downscaled contrast and the computed contrast
            // of the kmeans image * 3, then square the result. Basically just amplifying the
            // major discrepancies while downplaying the minor ones
            val contrastDelta = Image.ofSize(outDims.x, outDims.y).let {out ->
                reducedPaletteContrast.forEachPixel { x, y, rpc ->
                    val dsc = downscaledContrast[x,y]
                    val r = csqrd(max(dsc.r.toInt() - rpc.r.toInt() * 2, 0))
                    val g = csqrd(max(dsc.g.toInt() - rpc.g.toInt() * 2, 0))
                    out.set(x,y, r, g, 0, 255)
                }
                out
            }
            stages += "ContrastDelta" to contrastDelta


            val pixelWeights = FiniteGrid2D(srcImg.dimensions, 1)
            for (x in 0 until srcImg.width) {
                for (y in 0 until srcImg.height) {
                    val cdelta = contrastDelta[x / scale, y / scale]
                    val weight = if (cdelta.r > 50u || cdelta.g > 50u) {
                        1 + (contrastGradients[x, y].magnitude() * 100.0).toInt()
                    } else {
                        1
                    }
                    pixelWeights[x,y] = weight
                }
            }

            val adjustedPalette = extractKMeansPalette(srcImg, pixelWeights, paletteSize, 15)
            val sortedAdjustedPalette = adjustedPalette.sortedBy { it.r }
            stages += "ReducedPalette" to Image.ofSize(16,16 * paletteSize).withPixels { x, y, v -> v(sortedPalette[y / 16]) }
            stages += "AdjustedPalette" to Image.ofSize(16,16 * paletteSize).withPixels { x, y, v -> v(sortedAdjustedPalette[y / 16]) }

            val contrastAdjustedImage = Image.ofSize(outDims)
            srcImg.forKernels(scale) { x,y,kernel ->
                for (i in error.indices) {
                    error[i] = 0L
                }

                for (dx in 0 until kernel.size) {
                    for (dy in 0 until kernel.size) {
                        val c = kernel[dx,dy]
                        val cdelta = contrastDelta[x, y]
                        val mult = if (cdelta.r > 50u || cdelta.g > 50u) {
                            1.0 + contrastGradients[x * kernel.size + dx, y * kernel.size + dy].magnitude() * 20.0
                        } else {
                            1.0
                        }

                        for (i in 0 until paletteSize) {
                            error[i] += (colorDistance2(adjustedPalette[i], c).toDouble() * mult).toLong()
                        }
                    }
                }

                contrastAdjustedImage[x,y] = adjustedPalette[error.withIndex().minBy { v -> v.value }.index]
            }

            contrastAdjustedImage.transformPixels { _,_,v -> gammaUnCorrect(v) }
            stages += "ContrastAdjustedImage" to contrastAdjustedImage

            return Result(stages, contrastAdjustedImage)
        }
    }

    private fun csqrd(b : UByte) : UByte {
        return csqrd(b.toInt()).toUByte()
    }

    private fun csqrd(b : Int) : Int {
        return (pow(b.toDouble() / 255.0, 2.0) * 255.0).toInt()
    }

    private fun contrastGradient(nx : RGBA, c : RGBA, px : RGBA) : Float {
        val avgDist = ((colorDistance(nx, c) + colorDistance(c, px)) * 0.5) / 255.0
        return pow(avgDist, 2.0).toFloat()
    }

    private fun contrastGradientsToImage(contrastGradients : FiniteGrid2D<Vec2f>) : Image {
        return contrastGradients.run {
            val out = Image.ofSize(dimensions.x, dimensions.y)
            for (x in 0 until dimensions.x) {
                for (y in 0 until dimensions.y) {
                    val v = this[x,y]
                    out.set(x, y, (v.x * 255.0).toInt().clamp(0, 255), (v.y * 255.0).toInt().clamp(0, 255), 0, 255)
                }
            }
            out
        }
    }

    private fun computeContrastGradients(srcImg: Image): FiniteGrid2D<Vec2f> {
        val ret = FiniteGrid2D(srcImg.dimensions - 1, Vec2f(0.0f,0.0f))
        srcImg.forSlidingWindow(2, clamped = false) { x,y,kernel ->
            val xmag = pow(((colorDistance(kernel[0,0],kernel[1,0]) + colorDistance(kernel[0,1], kernel[1,1])) * 0.5) / 255.0, 2.0)
            val ymag = pow(((colorDistance(kernel[0,0],kernel[0,1]) + colorDistance(kernel[1,0], kernel[1,1])) * 0.5) / 255.0, 2.0)

            ret[x,y] = Vec2f(xmag.toFloat(), ymag.toFloat())
        }

        return ret
    }

    private fun computeContrastGradients2(srcImg: Image): FiniteGrid2D<Vec2f> {
        val ret = FiniteGrid2D(srcImg.dimensions, Vec2f(0.0f,0.0f))
        srcImg.forSlidingWindow(3, clamped = true) { x,y,kernel ->
            val v = Vec2f()
            for (axis in Axis2D.values()) {
                v[axis.ordinal] = contrastGradient(kernel[1 - axis.vec.x,1 - axis.vec.y], kernel[1,1], kernel[1 + axis.vec.x,1 + axis.vec.y])
            }
            ret[x,y] = v
        }

        return ret
    }

    private fun Image.downscaleBy(outDim : Vec2i, fn : (Kernel, RGBA) -> Unit) : Image {
        val out = Image.ofSize(outDim.x, outDim.y)
        val scale = ceil(this.width.toFloat() / outDim.x.toFloat()).toInt()
        val v = RGBA()
        this.forKernels(scale) { x, y, kernel ->
            fn(kernel, v)
            out[x,y] = v
        }
        return out
//        val kernel = Kernel(ceil(this.width.toFloat() / outDim.x.toFloat()).toInt())
//        for (x in 0 until out.width) {
//            for (y in 0 until out.height) {
//                val ox = x * kernel.size
//                val oy = y * kernel.size
//
//            }
//        }
    }


    private fun bilinearDownscale(img: Image, targetSize : Vec2i) : Image {
        val outDX = 1.0f / targetSize.x - 0.00001f
        val outDY = 1.0f / targetSize.y - 0.00001f

        fun sample(xf : Float, yf : Float, v : RGBA) {
            img.pixel((xf * img.width).toInt(), (yf * img.height).toInt(), v)
        }


        val a = RGBA()
        val b = RGBA()
        val c = RGBA()
        val d = RGBA()

        return Image.ofSize(targetSize.x, targetSize.y).withPixels { x,y,v ->
            val xf = x.toFloat() / targetSize.x.toFloat()
            val yf = y.toFloat() / targetSize.y.toFloat()

            sample(xf,yf, a)
            sample(xf + outDX, yf, b)
            sample(xf, yf + outDY, c)
            sample(xf + outDX, yf + outDY, d)

            val newR = ((a.r + b.r + c.r + d.r) / 4u).clamp(0u, 255u).toUByte()
            val newG = ((a.g + b.g + c.g + d.g) / 4u).clamp(0u, 255u).toUByte()
            val newB = ((a.b + b.b + c.b + d.b) / 4u).clamp(0u, 255u).toUByte()
            val newA = ((a.a + b.a + c.a + d.a) / 4u).clamp(0u, 255u).toUByte()

            v(newR, newG, newB, newA)
        }
    }
    
    private fun Params.largestWholeRatio(outDims : Vec2i) : Int {
        return (image.width.toFloat() / outDims.x.toFloat()).toInt()
    }

    private fun Params.chooseResultDimensions() : Vec2i {
        val woh = image.width.toFloat() / image.height.toFloat()
        return if (image.width >= image.height) {
            Vec2i((targetSize * woh).roundToInt(), targetSize)
        } else {
            Vec2i(targetSize, (targetSize / woh).roundToInt())
        }
    }


    private fun extractKMeansPalette(srcImg: Image, weightings: FiniteGrid2D<Int>, k : Int, iterations : Int): List<RGBA> {
        val rand = ThreadLocalRandom.current()

        val colorCounts = mutableMapOf<RGBA, Int>()

        fun downsampleRGBA(v: RGBA): RGBA {
            return RGBA(((v.r / 2u) * 2u + 1u).toUByte(), ((v.g / 2u) * 2u + 1u).toUByte(), ((v.b / 2u + 1u) * 2u).toUByte(), ((v.a / 2u) * 2u + 1u).toUByte())
        }
        srcImg.forEachPixel { x, y, v ->
            val weight = weightings[x,y]
            colorCounts.compute(downsampleRGBA(v)) { _, a -> (a ?: 0) + weight }
        }

        val centers = mutableListOf<RGBA>()
        val members = Array<MutableList<Pair<RGBA, Int>>>(k) { mutableListOf() }

        centers.add(colorCounts.keys.first())
        for (i in 1 until k) {
            val colorsAndWeights = mutableListOf<Pair<RGBA, Double>>()
            var sumDist = 0.0
            for ((p, count) in colorCounts) {
                var minDist = 100000.0
                for (j in 0 until i) {
                    minDist = min(minDist, colorDistance(p, centers[j]))
                }
                val weight = (minDist * minDist) * count
                colorsAndWeights.add(p to weight)
                sumDist += weight
            }

            var r = rand.nextDouble(sumDist - 0.0001)
            for ((p, d) in colorsAndWeights) {
                r -= d
                if (r <= 0.0) {
                    centers.add(p)
                    break
                }
            }
        }

        for (iteration in 0 until iterations) {
//            for (i in 0 until k) {
//                var closestColor = centers[i]
//                var closestDist = 10000.0
//                for (j in i + 1 until k) {
//                    val d = colorDistance(centers[i], centers[j])
//                    if (d < closestDist) {
//                        closestDist = d
//                        closestColor = centers[j]
//                    }
//                }
//                val dc = (closestColor.toFloat() - centers[i].toFloat())
//                dc.normalizeSafe()
//                val distFactor = pow(closestDist / 255.0, 1.0 / 3.0) * 0.05
//                centers[i] = RGBAf(centers[i].toFloat() - dc * distFactor.toFloat())
//            }

            // assign
            members.forEach { it.clear() }
            for ((p, count) in colorCounts) {
                var bestK = 0
                var bestDist = 10000.0
                for (i in 0 until k) {
                    val dist = colorDistance(centers[i], p)
                    if (dist < bestDist) {
                        bestDist = dist
                        bestK = i
                    }
                }
                members[bestK].add(p to count)
            }

            // recenter
            for (i in 0 until k) {
                val sum = Vec4d()
                var countSum = 0.0
                for ((p, count) in members[i]) {
                    sum.r += p.r.toDouble() * count
                    sum.g += p.g.toDouble() * count
                    sum.b += p.b.toDouble() * count
                    sum.a += p.a.toDouble() * count
                    countSum += count
                }
                centers[i] = RGBA(
                    ((sum.r / countSum)).clamp(0.0, 255.0).toUInt(),
                    ((sum.g / countSum)).clamp(0.0, 255.0).toUInt(),
                    ((sum.b / countSum)).clamp(0.0, 255.0).toUInt(),
                    ((sum.a / countSum)).clamp(0.0, 255.0).toUInt()
                )
            }
        }

        return centers
    }

    fun colorDistance2(e1: RGBA, e2: RGBA): Int {
        val rmean: Long = (e1.r.toLong() + e2.r.toLong()) shr 1
        val r = e1.r.toLong() - e2.r.toLong()
        val g = e1.g.toLong() - e2.g.toLong()
        val b = e1.b.toLong() - e2.b.toLong()
        return ((((512 + rmean) * r * r) shr 8) + 4 * g * g + (((767 - rmean) * b * b) shr 8)).toInt()
    }

    fun colorDistance(e1: RGBA, e2: RGBA): Double {
        val rmean: Long = (e1.r.toLong() + e2.r.toLong()) shr 1
        val r = e1.r.toLong() - e2.r.toLong()
        val g = e1.g.toLong() - e2.g.toLong()
        val b = e1.b.toLong() - e2.b.toLong()
        return sqrt(((((512 + rmean) * r * r) shr 8) + 4 * g * g + (((767 - rmean) * b * b) shr 8)).toDouble())
    }

    fun perceptualBrightness(c: RGBA): Double {
        return (c.r.toDouble() / 255.0) * 0.299 +
                (c.g.toDouble() / 255.0) * 0.587 +
                (c.b.toDouble() / 255.0) * 0.114
    }

    val dxs = arrayOf(-1,1,0,0)
    val dys = arrayOf(0,0,-1,1)
}