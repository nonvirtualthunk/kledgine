package hfcom.game

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.roundToInt


val summedOpenD6Propabilities = mutableListOf<Float>().apply {
    var remaining = 1.0f
    var pip = 1
    var scale = 1.0f
    add(remaining)
    while (remaining > 0.0005f) {
        add(remaining)
        if (pip == 6) {
            scale *= (1.0f / 6.0f)
            pip = 1
        }
        remaining -= (1.0f / 6.0f) * scale
        pip += 1
    }
}

val summedDNRProbabilities = mutableMapOf<Int, Float>().apply {
    this[-25] = 0.999375f
    this[-24] = 0.999265f
    this[-23] = 0.999073f
    this[-22] = 0.998845f
    this[-21] = 0.998532f
    this[-20] = 0.998108f
    this[-19] = 0.997524f
    this[-18] = 0.996763f
    this[-17] = 0.995701f
    this[-16] = 0.994116f
    this[-15] = 0.992109f
    this[-14] = 0.989416f
    this[-13] = 0.985632f
    this[-12] = 0.980683f
    this[-11] = 0.973999f
    this[-10] = 0.965102f
    this[-9] = 0.953149f
    this[-8] = 0.937568f
    this[-7] = 0.917582f
    this[-6] = 0.891362f
    this[-5] = 0.857786f
    this[-4] = 0.815199f
    this[-3] = 0.761986f
    this[-2] = 0.697715f
    this[-1] = 0.62346f
    this[0] = 0.541833f
    this[1] = 0.457503f
    this[2] = 0.375021f
    this[3] = 0.300564f
    this[4] = 0.23606f
    this[5] = 0.183008f
    this[6] = 0.140768f
    this[7] = 0.1076f
    this[8] = 0.081719f
    this[9] = 0.061675f
    this[10] = 0.046047f
    this[11] = 0.034219f
    this[12] = 0.025375f
    this[13] = 0.018699f
    this[14] = 0.013753f
    this[15] = 0.010106f
    this[16] = 0.007348f
    this[17] = 0.005365f
    this[18] = 0.003806f
    this[19] = 0.002692f
    this[20] = 0.001876f
    this[21] = 0.001274f
    this[22] = 8.45E-4f
    this[23] = 4.98E-4f
    this[24] = 2.89E-4f
    this[25] = 1.17E-4f
}

fun openD6roll(rand: ThreadLocalRandom): Int {
    val v = rand.nextInt(6) + 1
    return if (v == 6) {
        v + openD6roll(rand) - 1
    } else {
        v
    }
}




fun main() {
    println(summedOpenD6Propabilities.withIndex().forEach { (i,v) -> println("$i : ${(v * 1000).roundToInt() / 10.0}%") } )

    val results = mutableMapOf<Int, Int>()


    var minV = 0
    var maxV = 0
    var maxC = 0
    val rand = ThreadLocalRandom.current()
    for (i in 0 until 1000000) {
        val d = openD6roll(rand) + openD6roll(rand) - openD6roll(rand) - openD6roll(rand)
//        for (j in 0 .. d) {
//            results[j]++
//        }

        val c = results.getOrElse(d) { 0 } + 1
        results[d] = c
        if (c > 100) {
            minV = Integer.min(minV, d)
            maxV = Integer.max(maxV, d)
            maxC = Integer.max(maxC, c)
        }
    }

    val summedCounts = mutableMapOf<Int, Int>()

    for (v in minV .. maxV) {
        val c = results.getOrElse(v) { 0 }
        val f = c / maxC.toDouble()
        var str = "$v".padEnd(4)
        for (p in 0 until (f * 100).roundToInt()) {
            str += "*"
        }
        println(str)
        for (j in minV .. v) {
            summedCounts[j] = (summedCounts[j] ?: 0) + c
        }
    }

    for (v in minV .. maxV) {
        val p = summedCounts.getOrElse(v) { 0 } / 1000000.0
        println("${v.toString().padEnd(4)} : ${(p * 1000).roundToInt() / 10.0}%")
    }

    var code = ""
    for (v in minV .. maxV) {
        val p = summedCounts.getOrElse(v) { 0 } / 1000000.0
        code += "this[$v] = $p\n"
    }
    println(code)

//    println("=========================================")
//    for (i in 0 until 50) {
//        if (results[i] == 0) {
//            break
//        }
//        val v = summedD6Propabilities.getOrNull(i) ?: 0.0f
//        println("${i - 7} : ${((results[i] / 1000000.0) * 1000).roundToInt() / 10.0}% - ${(v * 1000).roundToInt() / 10.0}%")
//    }
}
