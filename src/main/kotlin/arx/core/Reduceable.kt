package arx.core

import com.typesafe.config.ConfigValue
import java.lang.Integer.max
import java.lang.Integer.min

class Reduceable(val maxValue : Int, val reducedBy : Int = 0) {
    companion object : FromConfigCreator<Reduceable> {
        override fun createFromConfig(cv: ConfigValue?): Reduceable? {
            return cv.asInt()?.let { Reduceable(it) }
        }
    }

    fun reducedBy(n : Int) : Reduceable {
        return Reduceable(maxValue, min(reducedBy + n, maxValue))
    }

    fun recoveredBy(n : Int) : Reduceable {
        return Reduceable(maxValue, max(reducedBy - n, 0))
    }

    val currentValue : Int get() { return maxValue - reducedBy }

    operator fun invoke() : Int {
        return currentValue
    }

    override fun toString(): String {
        return "Reduceable($currentValue/$maxValue)"
    }


}