package arx.core

import experiment.pixelator.Pixelator
import java.lang.Math.pow
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


data class KMeansParams<T>(
    val colorCounts : Map<T, Float>,
    val k : Int,
    val iterations : Int,
    val distFn : (T,T) -> Double,
    val centroidFn : (List<Pair<T,Float>>) -> T,
    val startingCenters : List<T> = emptyList(),
    val errorPower : Float = 2.0f
)

fun <T> kMeans(colorCounts : List<T>, k : Int, iterations : Int, distFn: (T, T) -> Double, centroidFn: (List<Pair<T,Float>>) -> T): List<T> {
    return kMeans(KMeansParams(
        colorCounts = colorCounts.map { it to 1.0f }.toMap(),
        k = k,
        iterations = iterations,
        distFn = distFn,
        centroidFn = centroidFn
    ))
}

fun <T> kMeans(colorCounts : Map<T, Float>, k : Int, iterations : Int, distFn: (T, T) -> Double, centroidFn: (List<Pair<T,Float>>) -> T): List<T> {
    return kMeans(KMeansParams(
        colorCounts = colorCounts,
        k = k,
        iterations = iterations,
        distFn = distFn,
        centroidFn = centroidFn
    ))
}

fun <T> kMeans(params : KMeansParams<T>) : List<T> {
    with(params) {
        if (k == 0) {
            return emptyList()
        }

        val rand = ThreadLocalRandom.current()

        val centers = mutableListOf<T>()
        centers.addAll(startingCenters)

        val members = Array<MutableList<Pair<T, Float>>>(k) { mutableListOf() }

        // todo: this should probably actually be chosen randomly
        centers.add(colorCounts.keys.first())
        val start = centers.size
        for (i in start until k) {
            val colorsAndWeights = mutableListOf<Pair<T, Double>>()
            var sumDist = 0.0
            for ((p, count) in colorCounts) {
                var minDist = 100000.0
                for (j in 0 until i) {
                    minDist = min(minDist, distFn(p, centers[j]))
                }
                val weight = (minDist.pow(errorPower.toDouble())) * count
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

        var lastWorstDistance = 1.0
        for (iteration in 0 until iterations) {
            var worstDistance = 0.0
            // assign
            members.forEach { it.clear() }
            for ((p, count) in colorCounts) {
                var bestK = 0
                var bestDist = 10000.0
                for (i in 0 until k) {
                    val dist = distFn(centers[i], p)
                    if (dist < bestDist) {
                        bestDist = dist
                        bestK = i
                    }
                }
                worstDistance = max(worstDistance, bestDist)
                members[bestK].add(p to count)
            }

            // recenter
            for (i in 0 until k) {
                centers[i] = centroidFn(members[i])
            }

//            if ((worstDistance - lastWorstDistance) / lastWorstDistance < 0.05) {
//                println("Breaking kMeans, minimal improvement at iteration $iteration")
//                return centers
//            }
        }

        return centers
    }
}


data class MedianCutParams(
    val colorCounts : Map<RGBA, Float>,
    val k : Int
)

internal data class MedianCutGroup(val colorCounts : Map<RGBA, Float>) {
    val min = RGBA(255,255,255,255)
    val max = RGBA(0,0,0,0)
    val range = RGBA(0,0,0,0)
    var maxRange = 0
    init {
        colorCounts.forEach {
            min.minWith(it.key)
            max.maxWith(it.key)
        }
        RGBAChannel.values().forEach {
            if (max[it] > min[it]) {
                range[it] = (max[it] - min[it]).toUByte()
            }
            maxRange = max(range[it].toInt(), maxRange)
        }
    }
}

internal data class MedianCutHSLGroup(val colorCounts : Map<HSL, Float>) {
    val min = HSL(1.0f,1.0f,1.0f,1.0f)
    val max = HSL(0.0f,0.0f,0.0f,0.0f)
    val range = HSL(0f,0f,0f,0f)
    var maxRange = 0.0f
    init {
        colorCounts.forEach {
            min.minWith(it.key)
            max.maxWith(it.key)
        }
        HSLChannel.values().forEach {
            if (max[it] > min[it]) {
                range[it] = (max[it] - min[it])
            }
            maxRange = range[it].max(maxRange)
        }
    }
}

fun medianCutPalette(params : MedianCutParams) : List<RGBA> {
    var groups = listOf(MedianCutGroup(params.colorCounts))
    while (groups.size < params.k) {
        val highestRangeGroup = groups.maxBy { it.maxRange }
        groups = groups - highestRangeGroup
        val (low, high) = medianCut(highestRangeGroup)
        groups = groups + low
        groups = groups + high
    }

    return groups.mapNotNull { group ->
        val v = Vec4f()
        var countTotal = 0.0f

        for ((color, count) in group.colorCounts) {
            val colorf = color.toFloat()
            v.r += colorf.r * count
            v.g += colorf.g * count
            v.b += colorf.b * count
            v.a += colorf.a * count
            countTotal += count
        }

        if (countTotal > 0.0f) {
            RGBAf(v.r / countTotal, v.g / countTotal, v.b / countTotal, v.a / countTotal)
        } else {
            Noto.warn("median cut palette extraction encountered a null grouping")
            null
        }
    }
}

internal fun medianCut(group: MedianCutGroup) : Pair<MedianCutGroup, MedianCutGroup> {
    val min = RGBA(255,255,255,255)
    val max = RGBA(0,0,0,0)
    var sum = 0.0f

    group.colorCounts.forEach {
        min.minWith(it.key)
        max.maxWith(it.key)
        sum += it.value
    }

    val highestRangeChannel = RGBAChannel.values().maxBy { if (max[it] > min[it]) { max[it] - min[it] } else { 0u } }

    val sortedColors = group.colorCounts.toList().sortedBy { it.first[highestRangeChannel] }
    var medianRemaining = sum / 2.0f

    val low = mutableMapOf<RGBA, Float>()
    val high = mutableMapOf<RGBA, Float>()

    for ((k,v) in sortedColors) {
        if (medianRemaining > 0.0f) {
            low[k] = v
        } else {
            high[k] = v
        }
        medianRemaining -= v
    }

    return MedianCutGroup(low) to MedianCutGroup(high)
}

//internal fun medianCut(group: MedianCutHSLGroup) : Pair<MedianCutHSLGroup, MedianCutHSLGroup> {
//    val min = RGBA(255,255,255,255)
//    val max = RGBA(0,0,0,0)
//    var sum = 0.0f
//
//    group.colorCounts.forEach {
//        min.minWith(it.key)
//        max.maxWith(it.key)
//        sum += it.value
//    }
//
//    val highestRangeChannel = RGBAChannel.values().maxBy { if (max[it] > min[it]) { max[it] - min[it] } else { 0u } }
//
//    val sortedColors = group.colorCounts.toList().sortedBy { it.first[highestRangeChannel] }
//    var medianRemaining = sum / 2.0f
//
//    val low = mutableMapOf<RGBA, Float>()
//    val high = mutableMapOf<RGBA, Float>()
//
//    for ((k,v) in sortedColors) {
//        if (medianRemaining > 0.0f) {
//            low[k] = v
//        } else {
//            high[k] = v
//        }
//        medianRemaining -= v
//    }
//
//    return MedianCutGroup(low) to MedianCutGroup(high)
//}