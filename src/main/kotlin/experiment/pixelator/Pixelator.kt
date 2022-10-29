package experiment.pixelator

import arx.core.*
import arx.display.core.Image
import arx.display.core.SentinelImage
import experiment.pixelator.Pixelator.forKernels
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.Math.max
import java.lang.Math.pow
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*


data class CommandLineArgs(
    val flagValues : Map<String,String>,
    val flags : List<String>,
    val unnamedArgs : List<String>
) {
    fun flagValue(shortName: String, longName: String) : String? {
        return flagValues[shortName] ?: flagValues[longName]
    }

    fun hasFlag(shortName : String, longName : String) : Boolean {
        return flags.contains(shortName) || flags.contains(longName) || flagValue(shortName, longName) != null
    }
}

fun extractArgs(args : Array<String>) : CommandLineArgs {
    var namedArgs = mapOf<String,String>()
    var unnamedArgs = listOf<String>()
    var flags = listOf<String>()

    var pendingNamedArg : String? = null
    for (arg in args) {
        if (arg.startsWith("--") || arg.startsWith("-")) {
            val stripped = arg.substringAfterLast('-')
            if (pendingNamedArg != null) {
                flags = flags + stripped
                pendingNamedArg = null
            } else {
                pendingNamedArg = stripped
            }
        } else {
            if (pendingNamedArg != null) {
                namedArgs = namedArgs + (pendingNamedArg to arg)
                pendingNamedArg = null
            } else {
                unnamedArgs = unnamedArgs + arg
            }
        }
    }
    return CommandLineArgs(namedArgs, flags, unnamedArgs)
}


enum class ArtifactType {
    Image,
    Gradient,
    Palette,
    Weights
}

enum class ArtifactScale {
    Source,
    Target
}


//data class ArtifactKey(val type : ArtifactType, val scale : ArtifactScale)

sealed interface Artifact {
    val image : arx.display.core.Image
    data class Image(override val image : arx.display.core.Image) : Artifact
    data class Gradients(val gradients : FiniteGrid2D<Vec2f>) : Artifact {
        override val image: arx.display.core.Image = TODO()
    }
    data class Palette(val colors : List<RGBA>) : Artifact {
        override val image: arx.display.core.Image = Pixelator.toPaletteImage(colors).first
    }
    data class Weights(val weights : FiniteGrid2D<Float>) : Artifact {
        override val image: arx.display.core.Image = arx.display.core.Image.ofSize(weights.dimensions)
    }
}

data class Input(val key: ArtifactKey, val optional: Boolean = false)

interface PipelineStage {
    interface Parameter {
        val name : String
        fun setFromString(str: String)
        data class StringParam(override val name : String, var value : String) : Parameter {
            override fun setFromString(str: String) {
                value = str
            }
        }

        data class FloatParam(override val name : String, var value : Float) : Parameter {
            override fun setFromString(str: String) {
                str.toFloatOrNull()?.let { value = it }
            }
        }

        data class BooleanParam(override val name : String, var value : Boolean) : Parameter {
            override fun setFromString(str: String) {
                str.toBooleanStrictOrNull()?.let { value = it }
            }
        }

        data class IntParam(override val name : String, var value : Int) : Parameter {
            override fun setFromString(str: String) {
                str.toIntOrNull()?.let { value = it }
            }
        }
    }


    val inputs : Set<Input>
    val outputs : Set<ArtifactKey>
    var parameters : Set<Parameter>

    fun process(inputs : Map<ArtifactKey, Artifact>) : Map<ArtifactKey, Artifact>

    fun Map<ArtifactKey, Artifact>.image(k : ArtifactKey) : Image? {
        return (this[k] as? Artifact.Image)?.image
    }

    fun Map<ArtifactKey, Artifact>.palette(k : ArtifactKey) : List<RGBA>? {
        return (this[k] as? Artifact.Palette)?.colors
    }

    fun Map<ArtifactKey, Artifact>.weights(k : ArtifactKey) : FiniteGrid2D<Float>? {
        return (this[k] as? Artifact.Weights)?.weights
    }

    fun Map<ArtifactKey, Artifact>.gradients(k : ArtifactKey) : FiniteGrid2D<Vec2f>? {
        return (this[k] as? Artifact.Gradients)?.gradients
    }
}

data class ArtifactKey(val name : String, val type: ArtifactType) {
    companion object {
        operator fun invoke(t : ArtifactType) : ArtifactKey { return ArtifactKey(name = t.name, type = t) }
    }
}


class Pipeline {
    data class SlotIdentifier(val stage : PipelineStage, val key : ArtifactKey)

    val stages : MutableList<PipelineStage> = mutableListOf()

    val inputMappings : MutableMap<SlotIdentifier, SlotIdentifier> = mutableMapOf()

    var sortedStages = listOf<PipelineStage>()

    val needsUpdate : MutableSet<PipelineStage> = mutableSetOf()

    val artifacts : MutableMap<SlotIdentifier, Artifact> = mutableMapOf()

    var parameterStates = mutableMapOf<Pair<PipelineStage, String>, String>()

    fun addStage(stage: PipelineStage) {
        stages.add(stage)
        for (param in stage.parameters) {
            parameterStates[stage to param.name] = param.toString()
        }
        sortedStages = sortStages(stages)
        needsUpdate.add(stage)
    }

    fun sortStages(stages : List<PipelineStage>) : List<PipelineStage> {
        val out = mutableListOf<PipelineStage>()
        stages.forEach { topologicalSort(it, out) }
        return out
    }
    fun topologicalSort(stage : PipelineStage, out : MutableList<PipelineStage>) {
        if (! out.contains(stage)) {
            for (input in stage.inputs) {
                inputMappings[SlotIdentifier(stage, input.key)]?.let {
                    topologicalSort(it.stage,  out)
                }
            }
            out.add(stage)
        }
    }

    internal fun recursiveDependents(stage : PipelineStage, accum : MutableSet<PipelineStage>) {
        if (!accum.add(stage)) {
            return
        }
        for ((output, input) in inputMappings) {
            if (input.stage == stage) {
                recursiveDependents(output.stage, accum)
            }
        }
    }

    fun setInputMapping(srcStage: PipelineStage, srcOutputKey : ArtifactKey, targetStage : PipelineStage, targetInputKey : ArtifactKey) {
        inputMappings[SlotIdentifier(targetStage, targetInputKey)] = SlotIdentifier(srcStage, srcOutputKey)
        needsUpdate.add(targetStage)
    }

    fun update() {
        for (stage in stages) {
            for (param in stage.parameters) {
                if (parameterStates[stage to param.name] != param.toString()) {
                    parameterStates[stage to param.name] = param.toString()
                    needsUpdate.add(stage)
                    println("Triggering update due to param change")
                }
            }
        }

        val allUpdates = mutableSetOf<PipelineStage>()
        needsUpdate.forEach { recursiveDependents(it, allUpdates) }
        needsUpdate.clear()

        sortedStages.forEach { stage ->
            if (allUpdates.contains(stage)) {
                val mappedArtifacts = mutableMapOf<ArtifactKey, Artifact>()
                var missingArtifact = false
                for (input in stage.inputs) {
                    var artifact: Artifact? = null
                    val mappedTo = inputMappings[SlotIdentifier(stage, input.key)]
                    if (mappedTo != null) {
                        artifact = artifacts[mappedTo]
                    }

                    if (artifact == null && !input.optional) {
                        missingArtifact = true
                        break
                    } else {
                        if (artifact != null) {
                            mappedArtifacts[input.key] = artifact
                        }
                    }
                }
                if (!missingArtifact) {
                    val results = stage.process(mappedArtifacts)
                    if (!stage.outputs.all { results.containsKey(it) }) {
                        Noto.warn("Error in stage $stage, expected outputs: ${stage.outputs}, actual: ${results.keys}")
                    }
                    for ((k, v) in results) {
                        val existing = artifacts[SlotIdentifier(stage, k)]
                        existing?.image?.destroy()
                        artifacts[SlotIdentifier(stage, k)] = v
                    }
                }
            }
        }
    }
}


class SourceImage : PipelineStage {
    val imageKey = ArtifactKey("image", ArtifactType.Image)
    val pathParam = PipelineStage.Parameter.StringParam(name = "path", value = "")

    override var parameters : Set<PipelineStage.Parameter> = setOf(pathParam)
    override val inputs: Set<Input> = setOf()
    override val outputs: Set<ArtifactKey> = setOf(imageKey)
    override fun process(inputs: Map<ArtifactKey, Artifact>) : Map<ArtifactKey, Artifact> {
        return mapOf(imageKey to Artifact.Image(image = Image.load(pathParam.value)))
    }
}

class LinearDownscale : PipelineStage {
    val imageInKey = ArtifactKey("input image", ArtifactType.Image)
    val imageOutKey = ArtifactKey("scaled image", ArtifactType.Image)

    val targetSizeParam = PipelineStage.Parameter.IntParam("target size", 512)

    override val inputs: Set<Input> = setOf(Input(imageInKey))
    override val outputs: Set<ArtifactKey> = setOf(imageOutKey)
    override var parameters: Set<PipelineStage.Parameter> = setOf()

    override fun process(inputs: Map<ArtifactKey, Artifact>): Map<ArtifactKey, Artifact> {
        val img = inputs.image(imageInKey) ?: return mapOf()

        val targetSize = if (img.width > img.height) {
            Vec2i(((img.width.toFloat() / img.height.toFloat()) * targetSizeParam.value).toInt(), targetSizeParam.value)
        } else {
            Vec2i(targetSizeParam.value, ((img.height.toFloat() / img.width.toFloat()) * targetSizeParam.value).toInt())
        }

        val outDX = 1.0f / targetSize.x - 0.00001f
        val outDY = 1.0f / targetSize.y - 0.00001f

        fun sample(xf : Float, yf : Float, v : RGBA) {
            img.pixel((xf * img.width).toInt(), (yf * img.height).toInt(), v)
        }


        val a = RGBA()
        val b = RGBA()
        val c = RGBA()
        val d = RGBA()

        val outImg = Image.ofSize(targetSize.x, targetSize.y).withPixels { x,y,v ->
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
        return mapOf(imageOutKey to Artifact.Image(outImg))
    }
}

class ReducePalette : PipelineStage {
    val imageKey = ArtifactKey(ArtifactType.Image)
    val paletteKey = ArtifactKey(ArtifactType.Palette)
    val weightsKey = ArtifactKey(ArtifactType.Weights)

    val paletteSizeParam = PipelineStage.Parameter.IntParam(name = "palette size", value = 32)
    val iterationsParam = PipelineStage.Parameter.IntParam(name = "iterations", value = 20)
    val powerParam = PipelineStage.Parameter.FloatParam(name = "error ^ power", value = 2.0f)
    val medianCutParam = PipelineStage.Parameter.BooleanParam(name = "median split", value = false)

    override val inputs: Set<Input> = setOf(Input(imageKey), Input(weightsKey, optional = true))
    override val outputs: Set<ArtifactKey> = setOf(paletteKey)
    override var parameters: Set<PipelineStage.Parameter> = setOf(paletteSizeParam, iterationsParam, powerParam, medianCutParam)

    override fun process(inputs: Map<ArtifactKey, Artifact>): Map<ArtifactKey, Artifact> {
        val srcImg = (inputs[imageKey] as? Artifact.Image)?.image ?: return mapOf()
        val weightings = (inputs[weightsKey] as? Artifact.Weights)?.weights ?: FiniteGrid2D(Vec2i(0,0), 1.0f)

        val colorCounts = mutableMapOf<RGBA, Float>()

        fun downsampleRGBA(v: RGBA): RGBA {
            return RGBA(((v.r / 2u) * 2u + 1u).toUByte(), ((v.g / 2u) * 2u + 1u).toUByte(), ((v.b / 2u + 1u) * 2u).toUByte(), ((v.a / 2u) * 2u + 1u).toUByte())
        }
        srcImg.forEachPixel { x, y, v ->
            val weight = weightings[x, y]
            colorCounts.compute(downsampleRGBA(v)) { _, a -> (a ?: 0.0f) + weight }
        }

        if (! medianCutParam.value) {
            val params = KMeansParams(
                colorCounts = colorCounts,
                k = paletteSizeParam.value,
                iterations = iterationsParam.value,
                distFn = { a, b -> Pixelator.colorDistance(a, b) },
                startingCenters = emptyList(),
                errorPower = powerParam.value,
                centroidFn = { members ->
                    val sum = Vec4d()
                    var countSum = 0.0
                    for ((p, count) in members) {
                        sum.r += p.r.toDouble() * count
                        sum.g += p.g.toDouble() * count
                        sum.b += p.b.toDouble() * count
                        sum.a += p.a.toDouble() * count
                        countSum += count
                    }
                    RGBA(
                        ((sum.r / countSum)).clamp(0.0, 255.0).toUInt(),
                        ((sum.g / countSum)).clamp(0.0, 255.0).toUInt(),
                        ((sum.b / countSum)).clamp(0.0, 255.0).toUInt(),
                        ((sum.a / countSum)).clamp(0.0, 255.0).toUInt()
                    )
                }
            )

            val palette = kMeans(params)

            return mapOf(paletteKey to Artifact.Palette(colors = palette))
        } else {
            val palette = medianCutPalette(MedianCutParams(colorCounts = colorCounts, k = paletteSizeParam.value))
            return mapOf(paletteKey to Artifact.Palette(colors = palette))
        }
    }
}


class ReduceImage : PipelineStage {
    val imageInKey = ArtifactKey(ArtifactType.Image)
    val paletteKey = ArtifactKey(ArtifactType.Palette)
    val imageOutKey = ArtifactKey(ArtifactType.Image)

    val scaleFactorParam = PipelineStage.Parameter.IntParam("scale factor", 4)
    val ditherFactionParam = PipelineStage.Parameter.FloatParam("dither factor", 1.0f)

    override val inputs: Set<Input> = setOf(Input(imageInKey), Input(paletteKey))
    override val outputs: Set<ArtifactKey> = setOf(imageOutKey)
    override var parameters: Set<PipelineStage.Parameter> = setOf(scaleFactorParam, ditherFactionParam)

    override fun process(inputs: Map<ArtifactKey, Artifact>): Map<ArtifactKey, Artifact> {
        val palette = inputs.palette(paletteKey) ?: return mapOf()
        val srcImg = inputs.image(imageInKey) ?: return mapOf()

        val ditherAmount = ditherFactionParam.value

        val scale = scaleFactorParam.value
        val outDims = srcImg.dimensions / scale

        val paletteSize = palette.size
        val result = Image.ofSize(outDims.x, outDims.y)
        val error = Array(paletteSize) { 0.0 }
        val diffusedError = FiniteGrid2D(outDims, Array(paletteSize) { 0.0 })
        for (x in 0 until outDims.x) {
            for (y in 0 until outDims.y) {
                diffusedError[x,y] = Array(paletteSize) { 0.0 }
            }
        }

        srcImg.forKernels(scale) { x,y,kernel ->
            for (i in error.indices) {
                error[i] = diffusedError[x,y][i] * ditherAmount
            }

            for (dx in 0 until kernel.size) {
                for (dy in 0 until kernel.size) {
                    val c = kernel[dx,dy]
                    for (i in 0 until paletteSize) {
                        val e = Pixelator.colorDistance2(c, palette[i]).toDouble()
                        error[i] = error[i] + e
                    }
                }
            }

            val minIndex = error.withIndex().minBy { v -> v.value }.index
            val minError = error[minIndex]
            for (i in error.indices) {
                if (i == minIndex) {
                    for (dv in Pixelator.atkinsonDV) {
                        diffusedError[x + dv.x, y + dv.y][i] += minError * (1.0 / 7.0)
                    }

                }
            }

            result[x,y] = palette[minIndex]
        }
        return mapOf(imageOutKey to Artifact.Image(result))
    }
}

//
//interface PipelineStage {
//    val provides: Set<ArtifactKey>
//    val dependsOn: Set<ArtifactKey>
//    fun process (pipeline: Pipeline) : Map<ArtifactKey, Any>
//}
//class Pipeline {
//    class Artifact(var value: Any, var revision: Int)
//
//    internal var pipelineStages: List<PipelineStage> = emptyList()
//
//
//
//
//    val artifacts: MutableMap<ArtifactKey, Artifact> = mutableMapOf()
//}

fun main(args : Array<String>) {
    val parsed = extractArgs(args)

    val params = Pixelator.Params(
        paletteSize = parsed.flagValue("p", "paletteSize")?.toIntOrNull() ?: 32,
        targetSize = parsed.flagValue("s", "size")?.toIntOrNull() ?: 64,
        dither = parsed.hasFlag("d", "dither"),
        ditherWeight = parsed.flagValue("d", "dither")?.toFloatOrNull() ?: 1.0f,
        image = SentinelImage
    )

    val prefix = if (parsed.unnamedArgs.isEmpty()) { "/Users/sbock/Downloads/ai/" } else { "" }
    val toProcess = parsed.unnamedArgs.ifEmpty { listOf("arrow_icon", "fire_pillar_1", "impressionist_lion", "fire_cave", "tarot_card_fox", "sorceress_512") }


    for (img in toProcess) {
        val chosenPath = listOf("${prefix}$img.png", "${prefix}$img").find { File(it).exists() }
        Pixelator.pixelate(
            params.copy(image = if (chosenPath != null) {
                Image.load(chosenPath)
            } else {
                throw IllegalArgumentException("Invalid path : ${prefix}$img")
            })
        ).apply {
            val imgEnd = img.substringAfterLast("/")
            val path = "/tmp/pixelator/${imgEnd}/"
            val dir = File(path)
            dir.listFiles()?.filter { it.extension == "png" }?.forEach { it.delete() }
            dir.mkdirs()
            for ((i, v) in stages.withIndex()) {
                val (stageName, stageImage) = v
                stageImage.writeToFile("$path${i}_$stageName.png")
            }
            finalImage.writeToFile("${path}${stages.size}_final.png")
        }
    }
}

object Pixelator {
    const val doGammaCorrection = false

    data class Params(
        var image : Image,
        var paletteSize : Int,
        var targetSize : Int,
        var dither : Boolean,
        var ditherWeight : Float
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
        if (params.image.isSentinel()) {
            throw java.lang.IllegalArgumentException("Could not load image")
        }
        with(params) {
            var lastTime = System.currentTimeMillis()
            val printTime = { s : String -> println("$s: ${System.currentTimeMillis() - lastTime}ms"); lastTime = System.currentTimeMillis() }

            val stages = mutableListOf<Pair<String, Image>>()
            val outDims = chooseResultDimensions()
            val scale = min(largestWholeRatio(outDims), 8)
            val srcImg = if ((image.width - outDims.x * scale) > 1) {
                Noto.info("Resizing input image from ${image.width} to ${outDims * scale}")
                val dsi = bilinearDownscale(image, outDims * scale)
                printTime("Downscaling")
                dsi
            } else {
                image
            }
            stages += "Source" to srcImg.map { _,_,v -> v }

            srcImg.transformPixels { x,y,v -> gammaCorrect(v) }
            printTime("Gamma correction")

            val palette = extractKMeansPalette(srcImg, FiniteGrid2D(srcImg.dimensions, 1), paletteSize, 20)
            printTime("Iniital kMeans")

            val kMeansImage = computeReducedPaletteImageMinErr(srcImg, outDims, palette, { _,_ -> ErrorAccum.Sum }) { _, _, _, _, rawColor, paletteColor ->
                colorDistance2(rawColor, paletteColor).toDouble()
            }
            printTime("kMeans image")

            stages += "ReducedPaletteImage" to kMeansImage.map { _,_,v -> gammaUnCorrect(v) }
            printTime("Gamma uncorrection")

            val contrastGradients = computeContrastGradients2(srcImg)
            val contrastImg = contrastGradientsToImage(contrastGradients)
            printTime("Contrast gradients + image")

            stages += "ContrastGradients" to contrastImg

            val reducedPaletteContrast = contrastGradientsToImage(computeContrastGradients2(kMeansImage))
            stages += "ReducedPaletteContrast" to reducedPaletteContrast
            printTime("Contrast gradient rendering")

            val downscaledContrastGradients = FiniteGrid2D(outDims, Vec2f())
            for (x in 0 until outDims.x) {
                for (y in 0 until outDims.y) {
                    var maxX = 0.0f
                    var maxY = 0.0f
                    for (dx in 0 until scale) {
                        for (dy in 0 until scale) {
                            val v = contrastGradients[x*scale + dx, y*scale + dy]
                            if (abs(v.x) > abs(maxX)) {
                                maxX = v.x
                            }
                            if (abs(v.y) > abs(maxY)) {
                                maxY = v.y
                            }
                        }
                    }
                    downscaledContrastGradients[x,y] = Vec2f(maxX, maxY)
                }
            }

//            val downscaledContrast = contrastImg.downscaleBy(outDims) { kernel, v ->
//                v(0u,0u,0u,255u)
//                kernel.data.forEach { d -> max(d, v, v) }
//            }
            val downscaledContrast = contrastGradientsToImage(downscaledContrastGradients)
            stages += "DownscaledContrast" to downscaledContrast
            printTime("Contrast downscale")

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
            printTime("Contrast delta")


            val pixelWeights = FiniteGrid2D(srcImg.dimensions, 1)
            for (x in 0 until srcImg.width) {
                for (y in 0 until srcImg.height) {
                    val cdelta = contrastDelta[x / scale, y / scale]
                    val weight = if (cdelta.r > 50u || cdelta.g > 50u) {
                        1 + (contrastGradients[x, y].magnitude() * 15.0).toInt()
                    } else {
                        1
                    }
                    pixelWeights[x,y] = weight
                }
            }

            val adjustedPalette = extractKMeansPalette(srcImg, pixelWeights, paletteSize, 20)
            printTime("Reweighted kMeans")
            val (paletteImage, paletteCenters) = toPaletteImage(palette)
            val (adjustedPaletteImage, _) = toPaletteImage(adjustedPalette, paletteCenters)
            stages += "ReducedPalette" to paletteImage
            stages += "AdjustedPalette" to adjustedPaletteImage
            printTime("Palette images")

            val contrastAdjustedImage = computeReducedPaletteImageMinErr(srcImg, outDims, adjustedPalette, {x,y ->
                if (contrastDelta[x, y].r > 50u || contrastDelta[x, y].g > 50u) {
                    ErrorAccum.Sum
                } else {
                    ErrorAccum.Sum
                }
            }) { x,y,ax,ay,rawColor, paletteColor ->
//                val cdelta = contrastDelta[x, y]
//                val cdelta = downscaledContrast[x,y]
                val cdelta = downscaledContrastGradients[x,y]
                val mult = if (abs(cdelta.x) > 0.1 || abs(cdelta.y) > 0.1) {
//                    val mult = if (abs(cdelta.x) > 50u || cdelta.g > 50u) {
                    1.0 + contrastGradients[ax, ay].magnitude() * 2.0
                } else {
                    1.0
                }
//                colorDistance2(rawColor, paletteColor).toDouble() * mult

                var dist = colorDistance2(rawColor, paletteColor).toDouble() * mult
                if (abs(cdelta.x) > 0.1f) {
                   val xGrad = signedContrastGradient(kMeansImage.getClamped(x - 1,y), paletteColor, kMeansImage.getClamped(x + 1,y))
                   dist += abs(xGrad - cdelta.x) * 200.0f
                }
                if (abs(cdelta.y) > 0.1f) {
                    val yGrad = signedContrastGradient(kMeansImage.getClamped(x,y-1), paletteColor, kMeansImage.getClamped(x,y+1))
                    dist += abs(yGrad - cdelta.y) * 200.0f
                }

//                var dist = colorDistance2(rawColor, paletteColor).toDouble() * mult
//                if (cdelta.r > 50u) {
//                    val xGrad = contrastGradient(kMeansImage.getClamped(x - 1,y), paletteColor, kMeansImage.getClamped(x + 1,y))
//                    dist -= xGrad * cdelta.r.toDouble()
//                }
//                if (cdelta.g > 50u) {
//                    val yGrad = contrastGradient(kMeansImage.getClamped(x,y-1), paletteColor, kMeansImage.getClamped(x,y+1))
//                    dist -= yGrad * cdelta.g.toDouble()
//                }

                dist
            }
            printTime("Adjusted palette image")

            contrastAdjustedImage.transformPixels { _,_,v -> gammaUnCorrect(v) }
            stages += "ContrastAdjustedImage" to contrastAdjustedImage

            return Result(stages, contrastAdjustedImage)
        }
    }

    private fun computeReducedPaletteImage(srcImg: Image, outDims: Vec2i, palette: List<RGBA>, errorFn: (Int, Int, Int, Int, RGBA, RGBA) -> Double): Image {
        val paletteSize = palette.size
        val scale = srcImg.width /  outDims.x
        val result = Image.ofSize(outDims.x, outDims.y)
        val error = Array(paletteSize) { 0.0 }
        srcImg.forKernels(scale) { x,y,kernel ->
            for (i in error.indices) {
                error[i] = 0.0
            }

            for (dx in 0 until kernel.size) {
                for (dy in 0 until kernel.size) {
                    val c = kernel[dx,dy]
                    for (i in 0 until paletteSize) {
                        error[i] += errorFn(x,y,x*kernel.size + dx,y*kernel.size + dy,c,palette[i])
                    }
                }
            }

            result[x,y] = palette[error.withIndex().minBy { v -> v.value }.index]
        }
        return result
    }

    enum class ErrorAccum {
        Sum,
        Min
    }


    val atkinsonDV = arrayOf(Vec2i(1,0), Vec2i(2,0), Vec2i(0,1), Vec2i(0,2), Vec2i(1,1), Vec2i(1,-1))

    private fun computeReducedPaletteImageMinErr(srcImg: Image, outDims: Vec2i, palette: List<RGBA>, accumFn : (Int,Int) -> ErrorAccum,  errorFn: (Int, Int, Int, Int, RGBA, RGBA) -> Double): Image {
        val paletteSize = palette.size
        val scale = srcImg.width /  outDims.x
        val result = Image.ofSize(outDims.x, outDims.y)
        val error = Array(paletteSize) { 0.0 }
        val diffusedError = FiniteGrid2D(outDims, Array(paletteSize) { 0.0 })
        for (x in 0 until outDims.x) {
            for (y in 0 until outDims.y) {
                diffusedError[x,y] = Array(paletteSize) { 0.0 }
            }
        }
        val weights = Array(paletteSize) { 0.0 }
        val rand = ThreadLocalRandom.current()
        srcImg.forKernels(scale) { x,y,kernel ->
            val accum = accumFn(x,y)

            for (i in error.indices) {
                error[i] = when (accum) {
                    ErrorAccum.Sum -> diffusedError[x,y][i]
                    ErrorAccum.Min -> Double.MAX_VALUE
                }
            }

            for (dx in 0 until kernel.size) {
                for (dy in 0 until kernel.size) {
                    val c = kernel[dx,dy]
                    for (i in 0 until paletteSize) {
                        val e = errorFn(x,y,x*kernel.size + dx,y*kernel.size + dy,c,palette[i])
                        error[i] = when (accum) {
                            ErrorAccum.Sum -> error[i] + e
                            ErrorAccum.Min -> error[i].min(e)
                        }
                    }
                }
            }

            val minIndex = error.withIndex().minBy { v -> v.value }.index
            val minError = error[minIndex]
            for (i in error.indices) {
                if (i == minIndex) {
//                    for (dx in 0 .. 1) {
//                        for (dy in 0 .. 1) {
//                            val diffuseWeight = if (dx == 1 && dy == 1) { 2.0 / 16.0 } else { 7.0 / 16.0 }
//                            diffusedError[x + dx, y + dy][i] = diffusedError[x + dx, y + dy][i] + minError * diffuseWeight * 0.85
//                        }
//                    }
                    for (dv in atkinsonDV) {
                        diffusedError[x + dv.x, y + dv.y][i] += minError * (1.0 / 7.0)
                    }

                }
            }

            result[x,y] = palette[minIndex]


//            var w = rand.nextDouble(sumWeight)
//            for (i in error.indices) {
//                w -= weights[i]
//                if (w < 0.00001) {
//                    result[x,y] = palette[i]
//                    break
//                }
//            }



        }
        return result
    }


    private fun csqrd(b : UByte) : UByte {
        return csqrd(b.toInt()).toUByte()
    }

    private fun csqrd(b : Int) : Int {
        return (pow(b.toDouble() / 255.0, 2.0) * 255.0).toInt()
    }

    private fun contrastGradient(nx : RGBA, c : RGBA, px : RGBA) : Float {
        val avgDist = (abs(signedColorDistance(nx, c) - signedColorDistance(c, px)) * 0.5) / 255.0
        return pow(avgDist, 2.0).toFloat()
    }

    private fun signedContrastGradient(nx : RGBA, c : RGBA, px : RGBA) : Float {
        return (((signedColorDistance(nx, c) - signedColorDistance(c, px)) * 0.5f) / 255.0f).toFloat()
    }

    private fun hueDistance(a: Float, b: Float) : Float {
        return min(abs(a - b), abs(1.0f + a - b))
    }

    private fun hueDistance(a: Vec3f, b: Vec3f) : Float {
        return min(abs(a.x - b.x), abs(1.0f + a.x - b.x))
    }

    private fun hueDistance(a: HSL, b: HSL) : Float {
        return min(abs(a.h - b.h), abs(1.0f + a.h - b.h))
    }

    fun toPaletteImage(palette : List<RGBA>, baseCenters : List<RGBA> = emptyList()) : Pair<Image, List<RGBA>> {
//        Image.ofSize(16,16 * palette.size)
        if (palette.isEmpty()) {
            return SentinelImage to emptyList()
        }

        val stopAtMaxDist = 0.15f

        val dist = { a : HSL, b : HSL ->
            hueDistance(a, b) * min(2.0f * max(a.s,b.s), 1.0f) + abs(a.s - b.s) * 0.5f
        }

        val hslPalette = palette.map { toHSL(it) }
        var groupCenters : List<HSL> = emptyList()
        if (baseCenters.isNotEmpty()) {
            groupCenters = baseCenters.map { toHSL(it) }
        } else {
            for (k in 1 until max(palette.size / 2, 2)) {
                val startingCenters = (0 until k).map { i -> if (i == 0) { HSL(0.0f, 0.0f, 0.5f) } else { HSL(i.toFloat() / (k+1).toFloat(), 0.5f, 0.5f) } }
                val params = KMeansParams(
                        colorCounts = hslPalette.map { it to 1.0f }.toMap(),
                        k = k,
                        iterations = 20,
                        distFn = { a, b -> dist(a, b).toDouble() },
                        centroidFn = {members ->
//                            val sum = Vec4f()
//                            it.forEach { m ->
//                                sum += fromHSL(m.first).toFloat()
//                            }
//                            sum /= it.size.toFloat()
//                            toHSL(RGBAf(sum))
                            if (members.isEmpty()) {
                                HSL()
                            } else {
                                val hs = members.map { it.first.h }.sorted()
                                var biggestGapIndex = 0
                                var biggestGap = 0.0f
                                for (i in 0 until hs.size) {
                                    val d = hueDistance(hs[i], hs[(i + 1) % hs.size])
                                    if (d > biggestGap) {
                                        biggestGap = d
                                        biggestGapIndex = i
                                    }
                                }
                                val h = hs[(biggestGapIndex - hs.size / 2 + hs.size) % hs.size]

                                val s = members.map { it.first.s }.average().toFloat()
                                val l = members.map { it.first.l }.average().toFloat()

                                HSL(h, s, l)
                            }
                        },
                        startingCenters = startingCenters)
                groupCenters = kMeans(params)
                val maxDist = hslPalette.maxOfOrNull { c -> groupCenters.minOfOrNull { g -> dist(c, g) } ?: 0.0f }
                if ((maxDist ?: 0.0f) < stopAtMaxDist) {
                    break
                }
            }
        }

        groupCenters = groupCenters.sortedBy { it.h }
        var groups = groupCenters.map { mutableListOf<RGBA>() }
        for (i in 0 until palette.size) {
            val paletteColor = palette[i]
            val paletteHSL = hslPalette[i]
            val closestGroupIndex = groupCenters.withIndex().minBy { dist(it.value, paletteHSL) }.index
            groups[closestGroupIndex].add(paletteColor)
        }

        val gc = mutableListOf<HSL>()
        val g = mutableListOf<MutableList<RGBA>>()
        for (i in 0 until groups.size) {
            if (groups[i].isNotEmpty()) {
                gc.add(groupCenters[i])
                g.add(groups[i])
            }
        }
        groupCenters = gc
        groups = g

        val out = Image.ofSize(32 * groupCenters.size - 16, 16 * groups.map { it.size }.max())
        for (i in 0 until groups.size) {
            groups[i].sortBy { perceptualBrightness(it) }
            for (j in 0 until groups[i].size) {
                val ax = i * 32
                val ay = j * 16
                for (dx in 0 until 16) {
                    for (dy in 0 until 16) {
                        val h = toHSL(groups[i][j])
                        if (dx < 8) {
                            out[ax + dx, ay + dy] = RGBAf(h.s, h.s, h.s, 1.0f)
                        } else {
                            out[ax + dx, ay + dy] = groups[i][j]
                        }
                    }
                }
            }
        }

        return out to groupCenters.map { it.toRGBA() }
    }

    private fun contrastGradientsToImage(contrastGradients : FiniteGrid2D<Vec2f>) : Image {
        return contrastGradients.run {
            val out = Image.ofSize(dimensions.x, dimensions.y)
            for (x in 0 until dimensions.x) {
                for (y in 0 until dimensions.y) {
                    val v = this[x,y]
                    out.set(x, y, (pow(v.x.toDouble(), 2.0) * 255.0).toInt().clamp(0, 255), (pow(v.y.toDouble(), 2.0) * 255.0).toInt().clamp(0, 255), 0, 255)
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
                v[axis.ordinal] = signedContrastGradient(kernel[1 - axis.vec.x,1 - axis.vec.y], kernel[1,1], kernel[1 + axis.vec.x,1 + axis.vec.y])
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
        val colorCounts = mutableMapOf<RGBA, Float>()

        fun downsampleRGBA(v: RGBA): RGBA {
            return RGBA(((v.r / 2u) * 2u + 1u).toUByte(), ((v.g / 2u) * 2u + 1u).toUByte(), ((v.b / 2u + 1u) * 2u).toUByte(), ((v.a / 2u) * 2u + 1u).toUByte())
        }
        srcImg.forEachPixel { x, y, v ->
            val weight = weightings[x, y]
            colorCounts.compute(downsampleRGBA(v)) { _, a -> (a ?: 0.0f) + weight }
        }

        return extractKMeansPalette(colorCounts, k , iterations)
    }

    private fun extractKMeansPalette(colorCounts : Map<RGBA, Float>, k : Int, iterations : Int): List<RGBA> {
        return kMeans(colorCounts, k, iterations, { a,b -> colorDistance(a,b) }) { members ->
            val sum = Vec4d()
            var countSum = 0.0
            for ((p, count) in members) {
                sum.r += p.r.toDouble() * count
                sum.g += p.g.toDouble() * count
                sum.b += p.b.toDouble() * count
                sum.a += p.a.toDouble() * count
                countSum += count
            }
            RGBA(
                ((sum.r / countSum)).clamp(0.0, 255.0).toUInt(),
                ((sum.g / countSum)).clamp(0.0, 255.0).toUInt(),
                ((sum.b / countSum)).clamp(0.0, 255.0).toUInt(),
                ((sum.a / countSum)).clamp(0.0, 255.0).toUInt()
            )
        }
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

    fun signedColorDistance(e1: RGBA, e2: RGBA): Double {
        val b1 = perceptualBrightness(e1)
        val b2 = perceptualBrightness(e2)

        val cd = colorDistance(e1, e2)
        return if (b1 > b2) {
            -cd
        } else {
            cd
        }
    }

    fun perceptualBrightness(c: RGBA): Double {
        return (c.r.toDouble() / 255.0) * 0.299 +
                (c.g.toDouble() / 255.0) * 0.587 +
                (c.b.toDouble() / 255.0) * 0.114
    }


    val dxs = arrayOf(-1,1,0,0)
    val dys = arrayOf(0,0,-1,1)
}