@file:OptIn(ExperimentalContracts::class)

package arx.core

import java.lang.Integer.max
import java.lang.Integer.min
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

enum class Axis2D(val vecf : Vec2f, val vec : Vec2i) {
    X(Vec2f(1.0f,0.0f), Vec2i(1,0)),Y(Vec2f(0.0f,1.0f), Vec2i(0,1));

    fun to3D() : Axis {
        return when(this) {
            X -> Axis.X
            Y -> Axis.Y
        }
    }
}


//enum class Cardinals2D(val vector: Vec2i) {
//    Left(Vec2i(-1,0)), Right(Vec2i(1,0)), Up(Vec2i(0,1)), Down(Vec2i(0,-1));
//}

val Cardinals2D = arrayOf(Vec2i(-1,0), Vec2i(1,0), Vec2i(0,1), Vec2i(0, -1))

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

fun <T> MutableList<T>.pop() : T {
    val ret = this[size - 1]
    this.removeAt(size-1)
    return ret
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


fun <T, U> Iterator<T>.map(fn : (T) -> U) : Iterator<U> {
    val iter = this
    return object : Iterator<U> {
        override fun hasNext(): Boolean {
            return iter.hasNext()
        }

        override fun next(): U {
            return fn(iter.next())
        }
    }
}

fun <T> Iterator<T>.toList() : List<T> {
    val ret = mutableListOf<T>()
    while (this.hasNext()) {
        ret.add(this.next())
    }
    return ret
}

fun <T> Iterator<T>.toSet() : Set<T> {
    val ret = mutableSetOf<T>()
    while (this.hasNext()) {
        ret.add(this.next())
    }
    return ret
}

object NullGuardLet : GuardLet<Any> {
    override fun orElse(block: () -> Any): Any {
        return block()
    }
}

fun Int.clamp(a : Int, b: Int) : Int {
    return max(min(this, b), a)
}

fun UInt.clamp(a : UInt, b: UInt) : UInt {
    if (this < a) {
        return a
    }
    if (this > b) {
        return b
    }
    return this
}

fun Double.clamp(a : Double, b: Double) : Double {
    return kotlin.math.max(kotlin.math.min(this, b), a)
}

fun maxu(a : UInt, b : UInt) : UInt {
    return if (a > b) { a } else { b }
}

fun minu(a : UInt, b : UInt) : UInt {
    return if (a < b) { a } else { b }
}

fun maxu(a : UByte, b : UByte) : UByte {
    return if (a > b) { a } else { b }
}

fun minu(a : UByte, b : UByte) : UByte {
    return if (a < b) { a } else { b }
}

@OptIn(ExperimentalContracts::class)
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

@OptIn(ExperimentalContracts::class)
inline infix fun <T, R> T?.expectLet(block : (T) -> R) : R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (this != null) {
        block(this)
    } else {
        Noto.recordError("expected non-null value in expectLet")
        null
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