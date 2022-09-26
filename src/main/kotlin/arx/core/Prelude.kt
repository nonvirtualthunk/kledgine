@file:OptIn(ExperimentalContracts::class)

package arx.core

import com.typesafe.config.*
import io.github.config4k.extract
import java.util.concurrent.ThreadLocalRandom
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


enum class Axis {
    X,Y,Z;

    fun is2D() : Boolean {
        return when (this) {
            X -> true
            Y -> true
            Z -> false
        }
    }

    fun to2D() : Axis2D? {
        return when (this) {
            X -> Axis2D.X
            Y -> Axis2D.Y
            Z -> null
        }
    }
}

fun axis(i: Int) : Axis {
    return when (i) {
        0 -> Axis.X
        1 -> Axis.Y
        2 -> Axis.Z
        else -> error("Axis only supports 3 dimensions: $i")
    }
}

enum class Axis2D {
    X,Y;

    fun to3D() : Axis {
        return when(this) {
            X -> Axis.X
            Y -> Axis.Y
        }
    }
}


enum class Cardinals2D(val vector: Vec2i) {
    Left(Vec2i(-1,0)), Right(Vec2i(1,0)), Up(Vec2i(0,1)), Down(Vec2i(0,-1));
}

val UnitSquare2D = arrayOf(Vec2f(0.0f, 0.0f), Vec2f(1.0f, 0.0f), Vec2f(1.0f, 1.0f), Vec2f(0.0f, 1.0f))
val UnitSquare3D = arrayOf(Vec3f(0.0f, 0.0f, 0.0f), Vec3f(1.0f, 0.0f, 0.0f), Vec3f(1.0f, 1.0f, 0.0f), Vec3f(0.0f, 1.0f, 0.0f))
val CenteredUnitSquare3D = arrayOf(Vec3f(-0.5f, -0.5f, 0.0f), Vec3f(0.5f, -0.5f, 0.0f), Vec3f(0.5f, 0.5f, 0.0f), Vec3f(-0.5f, 0.5f, 0.0f))
val UnitSquare2Di = arrayOf(Vec2i(0, 0), Vec2i(1, 0), Vec2i(1, 1), Vec2i(0, 1))

/**
 * swaps the given element with the last element of the
 * list then deletes that. Does not preserve ordering of
 * the list as a result, but doesn't require linear time
 */
fun <T> MutableList<T>.swapAndPop(i: Int) {
    this[i] = this[size - 1]
    this.removeAt(size-1)
}



sealed interface GuardLet<U> {
    infix fun orElse(block : () -> U) : U
}


@JvmInline
value class ValueGuardLet(val resultValue : Any?) : GuardLet<Any?> {
    override fun orElse(block: () -> Any?): Any? {
        return resultValue
    }
}

object NullGuardLet : GuardLet<Any> {
    override fun orElse(block: () -> Any): Any {
        return block()
    }
}

@Suppress("UNCHECKED_CAST")
inline infix fun <T, U> T?.ifLet(block : (T) -> U) : GuardLet<U> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (this == null) {
        NullGuardLet as GuardLet<U>
    } else {
        ValueGuardLet(block(this) as Any?) as GuardLet<U>
    }
}

inline infix fun <T> T?.ifPresent(block : (T) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this != null) {
        block(this)
    }
}

fun main() {

    var x : Int? = null
    if (ThreadLocalRandom.current().nextBoolean()) {
        x = 3
    }

    val y = x ifLet {
        it * 3
    } orElse {
        1
    }

    println("$x -> $y")
}