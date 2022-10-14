package experiment.pixelator

import arx.application.Application
import arx.core.*
import arx.display.core.Image
import arx.display.core.Key
import arx.display.windowing.*
import arx.display.windowing.components.ImageScale
import arx.display.windowing.components.ImageWidget
import arx.engine.*
import arx.engine.Event
import java.lang.Integer.max
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt


//val srcImagePath = "/Users/sbock/Downloads/ai/sorceress_512.png"
//val srcImagePath = "/Users/sbock/Downloads/ai/Samson_isometric_tactical_battle_pine_tree_painting_725bdfd0-d9b5-440e-920d-818e9c8bfdb6.png"
//val srcImagePath = "/Users/sbock/Downloads/ai/Samson_isometric_painting_of_a_oak_tree_in_summer_single_tree_s_4995fe18-eff8-47da-8c5d-a47a33902436.png"
//val srcImagePath = "/Users/sbock/Downloads/ai/Samson_lush_tree_in_an_abandoned_courtyard_victorian_stanley_ar_a302f03b-6a25-4ed0-9e8a-1b176d164f37.png"
//val srcImagePath = "/Users/sbock/Downloads/ai/Samson_a_roaring_forge_deep_in_the_lava_caves_of_a_mountain_sta_cabd0bfd-b88e-4f09-a5ff-707db3f2a98c.png"
val srcImagePath = "/Users/sbock/Downloads/ai/fire_pillar_1.png"
//val srcImagePath = "/Users/sbock/Downloads/ai/warrior_man_256.png"
//val srcImagePath = "/Users/sbock/Downloads/ai/impressionist_lion.png"

val targetSize = 64

const val PaletteSize: Int = 32

val childrenPerParent = 3

const val populationSize = 300


object EvolutionComponent : DisplayComponent() {

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

    val dxs = arrayOf(-1,1,0,0)
    val dys = arrayOf(0,0,-1,1)

    var naiveImage = Image.ofSize(targetSize, targetSize)
    var bestImg = Image.ofSize(targetSize, targetSize)
    val srcImg = Image.load(srcImagePath)
//        .run {
//        if (width > 512) {
//            val newImg = Image.ofSize(512, 512)
//
//        } else {
//            this
//        }
//    }

    val srcContrastRaw = IntArray(srcImg.width * srcImg.height).apply {
        for (x in 0 until srcImg.width) {
            for (y in 0 until srcImg.height) {
                if (x == 0 || y == 0 || x == srcImg.width - 1 || y == srcImg.height - 1) {
                    this[x * srcImg.height + y] = 128
                } else {
                    val s = srcImg[x, y]

                    var sum = 0.0
                    for (q in 0 until 4) {
                        val dx = dxs[q]
                        val dy = dys[q]
                        val dist = colorDistance(srcImg[x + dx, y + dy], s)
                        sum += dist * dist
                    }

                    this[x * srcImg.height + y] = sum.toInt() / 4
                }
            }
        }

    }

    val srcContrast = IntArray(targetSize * targetSize).apply {
        val scale = srcImg.width / targetSize
        for (x in 0 until targetSize) {
            for (y in 0 until targetSize) {
                var sum = 0L
                for (dx in 0 until scale) {
                    for (dy in 0 until scale) {
                        sum += srcContrastRaw[(x * scale + dx) * srcImg.height + (y * scale + dy)]
                    }
                }
                sum /= scale * scale
//                this[x * targetSize + y] = (sum / (scale * scale)).toInt()
                var m = 0
                for (dx in 0 until scale) {
                    for (dy in 0 until scale) {
                        m = maxOf(m, srcContrastRaw[(x * scale + dx) * srcImg.height + (y * scale + dy)])
                    }
                }
                this[x * targetSize + y] = ((m.toLong() + sum) / 2L).toInt()
            }
        }
    }

    var bestTransform = srcImg


    const val paletteMutationChance = 0.33f
    const val maxPaletteShiftPerComponent = 4

    const val colorMutationChance = 0.33f
    const val maxPixelMutations = 2

    class Individual(val palette: List<RGBA>, val data: ByteBuffer) {
        var fitness: Float = Float.MIN_VALUE
        var error: IntBuffer = IntBuffer.allocate(data.capacity())

        fun determineFitness(colors: Image, contrast: IntArray) {
            if (fitness == Float.MIN_VALUE) {
                val scale = colors.width / targetSize

                data.position(0)
                error.position(0)
                var sumSquareDist = 0.0
                for (x in 0 until targetSize) {
                    for (y in 0 until targetSize) {
                        var avgSqureDist = 0.0

                        val c = palette[data.get().toInt()]
                        for (dx in 0 until scale) {
                            for (dy in 0 until scale) {
                                val dist = colorDistance(colors[x * scale + dx, y * scale + dy], c)
                                avgSqureDist += dist * dist
                            }
                        }

                        avgSqureDist /= (scale * scale)
                        error.put(avgSqureDist.toInt())
                        sumSquareDist += avgSqureDist
                    }
                }

                var sumContrast = 0.0
                for (x in 1 until targetSize - 1) {
                    for (y in 1 until targetSize - 1) {
                        val srcContrast = contrast[x * targetSize + y]

                        val s = palette[data.get(x * targetSize + y).toInt()]
                        var sum = 0.0
                        for (q in 0 until 4) {
                            val dx = dxs[q]
                            val dy = dys[q]

                            val c = palette[data.get((x + dx) * targetSize + (y + dy)).toInt()]
                            val dist = colorDistance(s, c)
                            sum += dist * dist
                        }

                        val deltaContrast = abs((sum).toInt() / 4 - srcContrast)
                        sumContrast += deltaContrast
                        error.put(y * targetSize + x, error.get(y * targetSize + x) + deltaContrast)
                    }
                }

//                println(sumSquareDist.toFloat() / (sumSquareDist.toFloat() + sumContrast.toFloat()))
                fitness = -sumSquareDist.toFloat() - sumContrast.toFloat()
            }
        }

        fun makeChildWith(other: Individual): Individual {
            val rand = ThreadLocalRandom.current()
            val combinedPalette = MutableList(PaletteSize) { i ->
                if (rand.nextBoolean()) {
                    this.palette[i]
                } else {
                    other.palette[i]
                }
            }

            if (rand.nextFloat() < paletteMutationChance) {
                val i = rand.nextInt(PaletteSize)
                val c = combinedPalette[i]
                combinedPalette[i] = RGBA(
                    (c.r.toInt() + rand.nextInt(maxPaletteShiftPerComponent) - maxPaletteShiftPerComponent / 2).clamp(0, 255).toUByte(),
                    (c.g.toInt() + rand.nextInt(maxPaletteShiftPerComponent) - maxPaletteShiftPerComponent / 2).clamp(0, 255).toUByte(),
                    (c.b.toInt() + rand.nextInt(maxPaletteShiftPerComponent) - maxPaletteShiftPerComponent / 2).clamp(0, 255).toUByte(),
                    255u
                )
            }


//            val lb = data.asLongBuffer()
//            lb.position(0)
//            lb.limit(lb.capacity())
            data.position(0)
            other.data.position(0)
//
//            val olb = other.data.asLongBuffer()
//            olb.position(0)
//            olb.limit(olb.capacity())

            val newData = ByteBuffer.allocate(data.capacity())

            var a = 0
            for (i in 0 until data.limit() / 64) {
                val r = rand.nextLong()
                for (j in 0 until 64) {
                    newData.put(
                        if ((r and (1L shr j)) != 0L) {
                            data.get(a)
                        } else {
                            other.data.get(a)
                        }
                    )
                    a++
                }
            }

            if (rand.nextFloat() < colorMutationChance) {
                var i = 0
                val numMutations = rand.nextInt(maxPixelMutations - 1) + 1
                while (i < numMutations) {
                    val x = rand.nextInt(targetSize)
                    val y = rand.nextInt(targetSize)
                    if (error[y * targetSize + x] > 200) {
                        newData.put(y * targetSize + x, rand.nextInt(PaletteSize).toByte())
                        i += 1
                    }
                }
            }

//            val nlb = newData.asLongBuffer()

//            for (i in 0 until lb.limit()) {
//                val a = lb.get()
//                val b = lb.get()
//
//                val x = rand.nextLong()
//
//                // take half the
//                nlb.put((a and x) or (b and (x.inv())) )
//            }

            return Individual(
                palette = combinedPalette,
                data = newData
            )
        }
    }

    val threadPool = Executors.newFixedThreadPool(8) { r ->
        val tmp = Thread(r)
        tmp.isDaemon = true
        tmp
    }

    class Generation(pop: List<Individual>) {

        init {
            pop.map {
                threadPool.submit {
                    it.determineFitness(srcImg, srcContrast)
                }
            }.forEach { it.get() }
        }

        val population = pop.sortedByDescending { p ->
            p.fitness
        }

        fun createNewGeneration(): Generation {
            val maxFitness = population.maxOf { it.fitness }
            val minFitness = population.minOf { it.fitness }
            population.forEach { i ->
                i.fitness = (i.fitness - minFitness) / (maxFitness - minFitness)
            }
            val reproducingPopulation = population.subList(0, (population.size / 3) * 2)
            val totalFitness = reproducingPopulation.sumOf { i -> i.fitness.toDouble() * i.fitness.toDouble() }

            val rand = ThreadLocalRandom.current()
            fun pickIndividual(): Individual {
                var r = rand.nextDouble(totalFitness)
                for (i in reproducingPopulation.indices) {
                    r -= reproducingPopulation[i].fitness * reproducingPopulation[i].fitness
                    if (r <= 0.0) {
                        return reproducingPopulation[i]
                    }
                }
                println("fallthrough pick case")
                return reproducingPopulation[0]
            }

            val newPop = mutableListOf<Individual>()


            val futures = mutableListOf<Future<*>>()
            for (i in 0 until population.size / childrenPerParent) {
                futures.add(threadPool.submit {
                    val primary = pickIndividual()
                    for (j in 0 until childrenPerParent) {
                        val secondary = pickIndividual()
                        synchronized(newPop) {
                            newPop.add(primary.makeChildWith(secondary))
                        }
                    }
                })
            }

            futures.forEach { it.get() }

            return Generation(newPop)
        }
    }


    var activeGeneration = Generation(emptyList())

    override fun initialize(world: World) {

        val scale = srcImg.width / targetSize
        val tr = Image.ofSize(targetSize, targetSize).withPixels { x, y, r ->
            srcImg.pixel(x * scale, y * scale, r)
        }

        val ws = world[WindowingSystem]
        val originalWidget = ws.createWidget().apply {
            attachData(
                ImageWidget(
                    image = bindable(srcImg),
                    scale = ImageScale.Absolute(256, 256)
                )
            )

            width = WidgetDimensions.Intrinsic()
            height = WidgetDimensions.Intrinsic()
        }

        val contrastImg = Image.ofSize(srcImg.width, srcImg.height).withPixels { x, y, r ->
            val c = ((255 - sqrt(srcContrastRaw[x * srcImg.height + y].toDouble()).toInt()).clamp(0, 255)).toUByte()
            r(c, c, c, 255u)
        }

//        val smallContrastImg = Image.ofSize(targetSize, targetSize).withPixels { x, y, r ->
//            val c = (255 - srcContrast[x * targetSize + y].clamp(0, 255)).toUByte()
//            r(c,c,c,255u)
//        }

        val contrastWidget = ws.createWidget().apply {
            identifier = "ContrastWidget"
            attachData(
                ImageWidget(
                    image = bindable(contrastImg),
                    scale = ImageScale.Absolute(256, 256)
                )
            )

            width = WidgetDimensions.Intrinsic()
            height = WidgetDimensions.Intrinsic()

            x = WidgetPosition.Fixed(0, WidgetOrientation.TopRight)
            y = WidgetPosition.Fixed(0, WidgetOrientation.TopRight)
        }

        val bestWidget = ws.createWidget().apply {
            identifier = "BestWidget"
            attachData(
                ImageWidget(
                    image = bindable { bestImg },
                    scale = ImageScale.Absolute(256, 256)
                )
            )

            width = WidgetDimensions.Intrinsic()
            height = WidgetDimensions.Intrinsic()

            x = WidgetPosition.Fixed(0, WidgetOrientation.BottomLeft)
            y = WidgetPosition.Fixed(0, WidgetOrientation.BottomRight)
        }

        val actualSize = ws.createWidget().apply {
            identifier = "BestWidgetActualSize"
            attachData(
                ImageWidget(
                    image = bindable { bestImg },
                    scale = ImageScale.Absolute(targetSize, targetSize)
                )
            )

            width = WidgetDimensions.Intrinsic()
            height = WidgetDimensions.Intrinsic()

            x = WidgetPosition.Relative(WidgetNameIdentifier("ContrastWidget"), 0, WidgetOrientation.BottomLeft)
            y = WidgetPosition.Relative(WidgetNameIdentifier("BestWidget"), 0, WidgetOrientation.TopRight)
        }

        val naive = ws.createWidget().apply {
            identifier = "Naive"
            attachData(
                ImageWidget(
                    image = bindable { naiveImage },
                    scale = ImageScale.Absolute(targetSize, targetSize)
                )
            )

            width = WidgetDimensions.Intrinsic()
            height = WidgetDimensions.Intrinsic()

            x = WidgetPosition.Relative(WidgetNameIdentifier("BestWidgetActualSize"), 5, WidgetOrientation.TopRight)
            y = WidgetPosition.Relative(WidgetNameIdentifier("BestWidgetActualSize"), 0, WidgetOrientation.TopRight)
        }

    }


    var runGenetic = false
    var kMeansPalette : List<RGBA> = listOf()
    var naive : Individual? = null

    override fun update(world: World): Boolean {
        if (kMeansPalette.isEmpty()) {
            kMeansPalette = extractKMeansPalette(srcImg)
            naive = createNaiveImage(kMeansPalette)
            bestImg = naiveImage

            val ws = world[WindowingSystem]
            ws.markForUpdate(ws.desktop.descendantWithIdentifier("BestWidget")!!, RecalculationFlag.Contents)
            ws.markForUpdate(ws.desktop.descendantWithIdentifier("BestWidgetActualSize")!!, RecalculationFlag.Contents)
        }

        if (runGenetic) {
            if (activeGeneration.population.isEmpty()) {

                val rand = ThreadLocalRandom.current()
                val scale = srcImg.width / targetSize

                val initialPop = mutableListOf<Individual>()
                for (pi in 0 until populationSize) {
                    val palette = if (pi < populationSize / 2) {
                        kMeansPalette
                    } else {
                        val palette = mutableListOf<RGBA>()
                        for (i in 0 until PaletteSize) {
                            palette.add(srcImg[rand.nextInt(srcImg.width), rand.nextInt(srcImg.height)])
                        }
                        palette
                    }

                    val data = ByteBuffer.allocate(targetSize * targetSize)
                    if (pi < populationSize / 8) {
                        for (i in 0 until data.capacity()) {
                            data.put(i, naive!!.data.get(i))
                        }
                    } else {
                        for (x in 0 until targetSize) {
                            for (y in 0 until targetSize) {
                                var closestColorIndex = 0
                                var minimumColorDist = 10000.0

                                val c = srcImg[x * scale + rand.nextInt(scale), y * scale + rand.nextInt(scale)]
                                for (k in 0 until PaletteSize) {
                                    val paletteColor = palette[k]
                                    val d = colorDistance(c, paletteColor)
                                    if (d < minimumColorDist) {
                                        closestColorIndex = k
                                        minimumColorDist = d
                                    }
                                    //                            var avgDist = 0.0
                                    //                            for (dx in 0 until scale) {
                                    //                                for (dy in 0 until scale) {
                                    //                                    val srcColor = srcImg[x*scale + dx, y*scale + dy]
                                    //                                    avgDist += colorDistance(srcColor, paletteColor)
                                    //                                }
                                    //                            }
                                    //                            avgDist /= (scale * scale)
                                    //                            if (avgDist < minimumColorDist) {
                                    //                                minimumColorDist = avgDist
                                    //                                closestColorIndex = k
                                    //                            }
                                }

                                data.put(closestColorIndex.toByte())
                            }
                        }
                    }

                    initialPop.add(Individual(palette, data))
                }

                activeGeneration = Generation(initialPop)
            } else {
                activeGeneration = activeGeneration.createNewGeneration()
            }


            val ind = activeGeneration.population[0]
            bestImg.withPixels { x, y, v ->
                val c = ind.palette[ind.data[x * targetSize + y].toInt()]
                v.r = c.r
                v.g = c.g
                v.b = c.b
                v.a = c.a
                //                val py = y.toFloat() / targetSize.toFloat()
                //                val pi = (py * PaletteSize).toInt()
                //                v.r = kMeansPalette[pi].r
                //                v.g = kMeansPalette[pi].g
                //                v.b = kMeansPalette[pi].b
                //                v.a = 255u
            }
            bestImg.revision += 1
            val ws = world[WindowingSystem]
            ws.markForUpdate(ws.desktop.descendantWithIdentifier("BestWidget")!!, RecalculationFlag.Contents)
            ws.markForUpdate(ws.desktop.descendantWithIdentifier("BestWidgetActualSize")!!, RecalculationFlag.Contents)
        }

        return super.update(world)
    }

    private fun createNaiveImage(palette: List<RGBA>) : Individual {
        val scale = srcImg.width / targetSize
        val error = DoubleArray(palette.size)

        val naiveData = ByteBuffer.allocate(targetSize * targetSize)
        naiveData.position(0)

        var maxContrast = 0
        var avgContrast = 0L
        var maxColorDistance = 0
        var avgColorDistance = 0L
        naiveImage.withPixels { x,y,v ->

            for (dx in 0 until scale) {
                for (dy in 0 until scale) {
                    val ax = x * scale + dx
                    val ay = y * scale + dy

                    val contrast = srcContrastRaw[ay * srcImg.width + ax]
                    avgContrast += contrast
                    maxContrast = max(contrast, maxContrast)
                    val c = srcImg[ax, ay]
                    for (i in palette.indices) {
                        val d = colorDistance2(c, palette[i])
                        avgColorDistance += d
                        maxColorDistance = max(d, maxColorDistance)
                        error[i] += d.toDouble()// * sqrt(contrast.toDouble())
                    }
                }
            }

            var bestPI = 0
            var bestError = Double.MAX_VALUE
            for (i in palette.indices) {
                if (error[i] < bestError) {
                    bestError = error[i]
                    bestPI = i
                }
                error[i] = 0.0
            }

            naiveData.put(x * targetSize + y, bestPI.toByte())
            val p = palette[bestPI]
            v(p.r, p.g, p.b, p.a)
        }

        val divisor = srcImg.width * srcImg.height * palette.size
        println("contrast ${avgContrast / (srcImg.width * srcImg.height)} : $maxContrast, color ${avgColorDistance / divisor} : $maxColorDistance")


        return Individual(palette, naiveData)
    }

    private fun extractKMeansPalette(srcImg: Image): List<RGBA> {
        val k = PaletteSize
        val rand = ThreadLocalRandom.current()

        val colorCounts = mutableMapOf<RGBA, Int>()

        fun downsampleRGBA(v: RGBA): RGBA {
            return RGBA(((v.r / 2u) * 2u + 1u).toUByte(), ((v.g / 2u) * 2u + 1u).toUByte(), ((v.b / 2u + 1u) * 2u).toUByte(), ((v.a / 2u) * 2u + 1u).toUByte())
        }
        srcImg.forEachPixel { _, _, v -> colorCounts.compute(downsampleRGBA(v)) { _, a -> (a ?: 0) + 1 } }

        val centers = mutableListOf<RGBA>()
        var members = Array<MutableList<Pair<RGBA, Int>>>(PaletteSize) { mutableListOf() }

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

        for (iteration in 0 until 10) {
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

    override fun handleEvent(world: World, event: Event) {
        when (event) {
            is KeyPressEvent -> if (event.key == Key.Space) {
                bestImg.writeToFile("/tmp/pixelator.png")
            } else if (event.key == Key.E) {
                runGenetic = ! runGenetic
            }
        }
    }
}

fun main() {


    Application(800, 800).run(Engine(mutableListOf(), mutableListOf(WindowingSystemComponent, EvolutionComponent)))
}
