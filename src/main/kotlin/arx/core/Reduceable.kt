package arx.core

import java.lang.Integer.max
import java.lang.Integer.min

class Reduceable(val maxValue : Int) {
    var reducedBy : Int = 0

    fun reduceBy(n : Int) {
        reducedBy = min(reducedBy + n, maxValue)
    }

    fun recoverBy(n : Int) {
        reducedBy = max(reducedBy - n, 0)
    }

    val currentValue : Int get() { return maxValue - reducedBy }

    operator fun invoke() : Int {
        return currentValue
    }
}