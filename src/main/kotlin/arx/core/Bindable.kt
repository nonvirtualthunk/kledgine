package arx.core

import arx.display.core.Image
import arx.display.core.ImageRef
import arx.display.core.SentinelImageRef
import com.typesafe.config.ConfigValue
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.internal.impl.load.java.structure.JavaClass
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf


val stringBindingPattern = Regex("%\\([?!]?[?!]?\\s*([a-zA-Z\\d.]*\\s*)\\)")

class BindingContext(val mappings: Map<String, Any>, val parent: BindingContext?) {

    fun resolveRaw(bindingPattern: String) : Any? {
        return mappings[bindingPattern] ?: parent?.resolve(bindingPattern)
    }

    fun resolve(bindingPattern: String) : Any? {
        val patternSections = bindingPattern.split('.')
        var value = resolveRaw(patternSections[0])
        for (i in 1 until patternSections.size) {
            val curValue = value ?: return null
            value = if (curValue is Map<*, *>) {
                curValue[patternSections[i]]
            } else {
                val prop = curValue.javaClass.kotlin.declaredMemberProperties
                                .find { it.name == patternSections[i] }
                if (prop != null) {
                    prop.get(curValue)
                } else {
                    curValue::class.functions.find { it.name == patternSections[i] }?.let { fn ->
                        fn.instanceParameter?.let { instParam ->
                            fn.callBy(mapOf(instParam to curValue))
                        }
                    }
                }
            }
        }
        return value
    }
}

val EmptyBindingContext = BindingContext(mapOf(), null)


interface Bindable<out T> {
    companion object {
        val updateFunctionsByType = mutableMapOf<Class<*>, (Any, BindingContext) -> Boolean>()

        fun updateBindableFields(v: Any, ctx: BindingContext) : Boolean {
            val fn = updateFunctionsByType.getOrPut(v.javaClass) {
                val relevantFields = (v::class.memberProperties
                    .filter { p -> p.getter.returnType.isSubtypeOf(Bindable::class.starProjectedType) })

                { a, c ->
                    var anyChanged = false
                    for (field in relevantFields) {
                        val bindable = field.getter.call(a) as Bindable<*>
                        if (bindable.update(c)) {
                            anyChanged = true
                        }

                    }
                    anyChanged
                }
            }

            return fn(v, ctx)
        }
    }

    operator fun invoke() : T

    fun update(ctx: BindingContext) : Boolean

    fun copyBindable() : Bindable<T>
}


fun <T : Any> bindableAny(cv : ConfigValue?, defaultValue : T) : Bindable<T> {
    return if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        stringBindingPattern.match(str).ifLet { (pattern) ->
            AnyPatternBindable(pattern, defaultValue) as Bindable<T>
        }.orElse {
            ValueBindable(defaultValue)
        }
    } else {
        ValueBindable(defaultValue)
    }
}

fun bindableInt(cv : ConfigValue?) : Bindable<Int> {
    return if (cv.isNum()) {
        ValueBindable(cv.asInt() ?: 0)
    } else if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        stringBindingPattern.match(str).ifLet { (pattern) ->
            IntPatternBindable(pattern, 0) as Bindable<Int>
        }.orElse {
            str.toIntOrNull().ifLet {
                ValueBindable(it) as Bindable<Int>
            }.orElse {
                Noto.warn("Invalid string representation for int: $cv")
                ValueBindable.Zero
            }
        }
    } else {
        Noto.warn("Invalid config for bindable int: $cv")
        ValueBindable.Zero
    }
}

fun bindableFloat(cv : ConfigValue?) : Bindable<Float> {
    return if (cv.isNum()) {
        ValueBindable(cv.asFloat() ?: 0.0f)
    } else if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        stringBindingPattern.match(str).ifLet { (pattern) ->
            FloatPatternBindable(pattern, 0.0f) as Bindable<Float>
        }.orElse {
            str.toFloatOrNull().ifLet {
                ValueBindable(it) as Bindable<Float>
            }.orElse {
                Noto.warn("Invalid string representation for float: $cv")
                ValueBindable.Zerof
            }
        }
    } else {
        Noto.warn("Invalid config for bindable float: $cv")
        ValueBindable.Zerof
    }
}

fun bindableRGBA(cv : ConfigValue?) : Bindable<RGBA> {
    val directParsed = RGBA(cv)
    return if (directParsed != null) {
        ValueBindable(directParsed)
    } else {
        if (cv.isStr()) {
            val str = cv.asStr() ?: ""
            stringBindingPattern.match(str).ifLet { (pattern) ->
                RGBAPatternBindable(pattern, White) as Bindable<RGBA>
            }.orElse {
                Noto.warn("Invalid string representation for rgba: $cv")
                ValueBindable.White
            }
        } else {
            Noto.warn("Invalid config for bindable rgba: $cv")
            ValueBindable.White
        }
    }
}

fun bindableRGBAOpt(cv : ConfigValue?) : Bindable<RGBA?> {
    if (cv == null) {
        return ValueBindable.Null()
    }
    val directParsed = RGBA(cv)
    return if (directParsed != null) {
        ValueBindable(directParsed)
    } else {
        if (cv.isStr()) {
            val str = cv.asStr() ?: ""
            stringBindingPattern.match(str).ifLet { (pattern) ->
                RGBAOptPatternBindable(pattern, null) as Bindable<RGBA?>
            }.orElse {
                Noto.warn("Invalid string representation for rgba?: $cv")
                ValueBindable.Null()
            }
        } else {
            Noto.warn("Invalid config for bindable rgba?: $cv")
            ValueBindable.Null()
        }
    }
}

fun bindableString(cv : ConfigValue?) : Bindable<String> {
    return if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        if (stringBindingPattern.containsMatchIn(str)) {
            StringPatternBindable(str, str)
        } else {
            ValueBindable(str)
        }
    } else {
        Noto.warn("Invalid config for bindable string : $cv")
        ValueBindable("")
    }
}

fun bindableRichText(cv : ConfigValue?) : Bindable<RichText> {
    return if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        if (stringBindingPattern.containsMatchIn(str)) {
            RichTextPatternBindable(str)
        } else {
            ValueBindable(RichText(str))
        }
    } else {
        Noto.warn("Invalid config for bindable string : $cv")
        ValueBindable(RichText(""))
    }
}

fun bindableImage(cv : ConfigValue?) : Bindable<Image> {
    return if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        stringBindingPattern.match(str).ifLet { (pattern) ->
            ImagePattern(pattern) as Bindable<Image>
        }.orElse {
            ValueBindable(Resources.image(str))
        }
    } else {
        Noto.warn("Invalid config for bindable image : $cv")
        ValueBindable(SentinelImageRef.toImage())
    }
}

fun bindableBool(cv : ConfigValue?) : Bindable<Boolean> {
    return if (cv.isBool()) {
        ValueBindable(cv.asBool()!!)
    } else if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        stringBindingPattern.match(str).ifLet { (pattern) ->
            BooleanPatternBindable(pattern, str.contains('?'), str.contains('!'), false) as Bindable<Boolean>
        }.orElse {
            if (str.toBoolean()) {
                ValueBindable.True
            } else {
                ValueBindable.False
            }
        }

    } else {
        Noto.warn("Invalid config for bindable boolean : $cv")
        ValueBindable(false)
    }
}




//inline fun <reified T>patternBindable(defaultValue: T, pattern: String) : Bindable<T> {
//    return object : Bindable<T> {
//        var currentValue: T = defaultValue
//        override fun invoke(): T {
//            return currentValue
//        }
//
//        override fun update(ctx: BindingContext) : Boolean {
//            val bound = ctx.resolve(pattern)
//            if (bound != null) {
//                return if (T::class.java.isAssignableFrom(bound.javaClass)) {
//                    if (currentValue != bound) {
//                        currentValue = bound as T
//                        true
//                    } else {
//                        false
//                    }
//                } else {
//                    Noto.recordError("bound value of incorrect type", mapOf("desiredType" to T::class.simpleName, "actualType" to bound.javaClass.simpleName))
//                    false
//                }
//            } else {
//                return if (currentValue != null) {
//                    currentValue = defaultValue
//                    true
//                } else {
//                    false
//                }
//            }
//        }
//
//    }
//}


fun <T>bindable(b: () -> T) : Bindable<T>{
    return FunctionBindable(b)
}
fun <T>bindable(b: T) : Bindable<T>{
    return ValueBindable(b)
}

data class ConstantBindable<T>(val value : T) : Bindable<T> {
    override fun invoke(): T {
        return value
    }

    override fun update(ctx: BindingContext): Boolean {
        return false
    }

    override fun copyBindable(): Bindable<T> {
        return this
    }
}

data class ValueBindable<T>(val value: T) : Bindable<T> {
    var first = true
    companion object {
        val True = ConstantBindable(true)
        val False = ConstantBindable(false)
        val Zero = ConstantBindable(0)
        val Zerof = ConstantBindable(0.0f)
        private val NullRef = ConstantBindable(null)
        val White = ConstantBindable(RGBA(255,255,255,255))

        @Suppress("UNCHECKED_CAST")
        fun<T> Null() : Bindable<T?> {
            return NullRef
        }
    }

    override fun invoke(): T {
        return value
    }

    override fun update(ctx: BindingContext) : Boolean {
        if (first) {
            first = false
            return true
        }
        // do nothing, we have a fixed value
        return false
    }

    override fun copyBindable(): Bindable<T> {
        return ValueBindable(value)
    }
}

data class FunctionBindable<T>(val fn : () -> T) : Bindable<T> {
    var prevValue : T? = null
    var updated : Boolean = false
    override fun invoke(): T {
//        if (prevValue == null) {
//            prevValue = fn()
//            updated = true
//        }
//        return prevValue!!
        return fn()
    }

    override fun update(ctx: BindingContext): Boolean {
        val newValue = fn()
        val modified = if (prevValue != newValue) {
            prevValue = newValue
            true
        } else {
            false
        }
        val ret = modified || updated
        updated = false
        return ret
    }

    override fun copyBindable(): Bindable<T> {
        return copy()
    }
}


abstract class BasePatternBindable<T>(val pattern : String, var value : T) : Bindable<T> {
    override fun invoke(): T {
        return value
    }

    abstract fun transform(a : Any?) : T?

    override fun update(ctx: BindingContext): Boolean {
        val res = ctx.resolve(pattern)
        val tr = transform(res)
        return if (tr != null && tr != value) {
            value = tr
            true
        } else{
            false
        }
    }
}


//inline fun <reified T> anyPatternBindable(pattern : String, value : T) : BasePatternBindable<T> {
//    return AnyPatternBindable(pattern, value, typeOf<T>().jvmErasure.java)
//}


class AnyPatternBindable<T : Any>(pattern : String, value : T) : BasePatternBindable<T>(pattern, value) {
    val clazz = value.javaClass
    override fun copyBindable(): Bindable<T> {
        return AnyPatternBindable(pattern, value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun transform(a: Any?): T? {
        if (a == null) {
            return null
        }
        return if (clazz.isAssignableFrom(a.javaClass)) {
            a as? T?
        } else {
            null
        }
    }
}

class BooleanPatternBindable(pattern : String, val justCheckPresent : Boolean, val invert : Boolean, value : Boolean = false) : BasePatternBindable<Boolean>(pattern, value) {
    override fun transform(a: Any?): Boolean? {
        return when (a) {
            is Boolean ->
                if (!invert) {
                    a
                } else {
                    ! a
                }
            else -> {
                if (justCheckPresent) {
                    if (! invert) {
                        a != null
                    }  else {
                        a == null
                    }
                } else {
                    null
                }
            }
        }
    }

    override fun copyBindable(): Bindable<Boolean> {
        return BooleanPatternBindable(pattern, justCheckPresent, invert, value)
    }
}

class IntPatternBindable(pattern : String, value : Int = 0) : BasePatternBindable<Int>(pattern, value) {
    override fun transform(a: Any?): Int? {
        return (a as? Number)?.toInt()
    }

    override fun copyBindable(): Bindable<Int> {
        return IntPatternBindable(pattern, value)
    }
}

class ImageRefPattern(pattern : String, value : ImageRef = SentinelImageRef) : BasePatternBindable<ImageRef>(pattern, value) {
    override fun transform(a: Any?): ImageRef? {
        return a as? ImageRef
    }

    override fun copyBindable(): Bindable<ImageRef> {
        return ImageRefPattern(pattern, value)
    }
}

class ImagePattern(pattern : String, value : Image = SentinelImageRef.toImage()) : BasePatternBindable<Image>(pattern, value) {
    override fun transform(a: Any?): Image? {
        return (a as? ImageRef)?.toImage() ?: (a as? Image)
    }

    override fun copyBindable(): Bindable<Image> {
        return ImagePattern(pattern, value)
    }
}


class RGBAPatternBindable(pattern : String, value : RGBA = White) : BasePatternBindable<RGBA>(pattern, value) {
    override fun transform(a: Any?): RGBA? {
        return a as? RGBA
    }

    override fun copyBindable(): Bindable<RGBA> {
        return RGBAPatternBindable(pattern, value)
    }
}

class RGBAOptPatternBindable(pattern : String, value : RGBA? = null) : BasePatternBindable<RGBA?>(pattern, value) {
    override fun transform(a: Any?): RGBA? {
        return a as? RGBA
    }

    override fun copyBindable(): Bindable<RGBA?> {
        return RGBAOptPatternBindable(pattern, value)
    }
}


class FloatPatternBindable(pattern : String, value : Float = 0.0f) : BasePatternBindable<Float>(pattern, value) {
    override fun transform(a: Any?): Float? {
        return (a as? Number)?.toFloat()
    }

    override fun copyBindable(): Bindable<Float> {
        return FloatPatternBindable(pattern, value)
    }
}

class StringPatternBindable(val rawPattern : String, var value : String = "") : Bindable<String> {
    val patterns = stringBindingPattern.match(rawPattern)?.raw
    override fun invoke(): String {
        return value
    }

    override fun update(ctx: BindingContext): Boolean {
        if (patterns != null) {
            var cursor = 0
            var computed = ""
            for (p in patterns) {
                if (p.range.first > cursor) {
                    computed += rawPattern.substring(cursor until p.range.first)
                }
                p.groupValues.getOrNull(1)?.let { ctx.resolve(it) }?.let {
                    computed += it
                }
                cursor = p.range.last + 1
            }
            if (cursor < rawPattern.length) {
                computed += rawPattern.substring(cursor until rawPattern.length)
            }

            if (computed != value) {
                value = computed
                return true
            }
        } else {
            Noto.err("Invalid string pattern : $rawPattern")
        }
        return false
    }

    override fun copyBindable(): Bindable<String> {
        return StringPatternBindable(rawPattern, value)
    }
}


class RichTextPatternBindable(val rawPattern : String) : Bindable<RichText> {
    val patterns = stringBindingPattern.match(rawPattern)?.raw

    var value : RichText = computeRichText(EmptyBindingContext)
    override fun invoke(): RichText {
        return value
    }

    fun computeRichText(ctx : BindingContext): RichText {
        if (patterns == null) { return RichText(rawPattern) }

        var cursor = 0
        var computed = ""
        for (p in patterns) {
            if (p.range.first > cursor) {
                computed += rawPattern.substring(cursor until p.range.first)
            }
            p.groupValues.getOrNull(1)?.let { ctx.resolve(it) }?.let {
                computed += it
            }
            cursor = p.range.last + 1
        }
        if (cursor < rawPattern.length) {
            computed += rawPattern.substring(cursor until rawPattern.length)
        }

        return RichText(computed)
    }

    override fun update(ctx: BindingContext): Boolean {
        if (patterns != null) {
            val rt = computeRichText(ctx)
            if (rt != value) {
                value = rt
                return true
            }
        } else {
            Noto.err("Invalid rich text pattern : $rawPattern")
        }
        return false
    }

    override fun copyBindable(): Bindable<RichText> {
        return RichTextPatternBindable(rawPattern)
    }
}



class PropertyBinding(val pattern : String, val twoWayBinding : Boolean, var lastBoundValue : Any? = null) {
    val mainPatternSection = pattern.substringBeforeLast('.')
    val finalSection = pattern.substringAfterLast('.')
    init {
        if (twoWayBinding && ! pattern.contains('.')) {
            Noto.warn("Two way bindings currently require a A.B style binding with a parent and field part")
        }
    }
    fun update(ctx : BindingContext, receiveFn : (Any) -> Unit, newSetterFn : (KMutableProperty.Setter<*>, KParameter, Any) -> Unit) {
        ctx.resolve(pattern)?.let { bound ->
            receiveFn(bound)
        }

        if (twoWayBinding) {
            val bound = ctx.resolve(mainPatternSection)
            if (bound !== lastBoundValue) {
                lastBoundValue = bound
                if (bound != null) {
                    val mutProp = bound::class.declaredMemberProperties.find { it.name == finalSection } as? KMutableProperty<*>
                    if (mutProp != null) {
                        val setter = mutProp.setter
                        val instanceParam = setter.instanceParameter
                        if (instanceParam != null) {
                            newSetterFn(setter, instanceParam, bound)
                        } else {
                            Noto.warn("two way binding encountered a null instance param?")
                        }
                    } else {
                        Noto.warn("two way binding could not find appropriate property: $finalSection on base binding of $mainPatternSection")
                    }
                }
            }
        }
    }
}

//inline fun <reified T> patternBindable(pattern : String) : Bindable<T> {
//    return object : Bindable<T> {
//        override fun invoke(): T {
//
//            TODO("Not yet implemented")
//        }
//
//        override fun update(ctx: BindingContext): Boolean {
//            TODO("Not yet implemented")
//        }
//    }
//}



//data class PatternBindable<T>(val pattern: String, var value : T) : Bindable<T> {
//    override fun invoke(): T {
//        return value
//    }
//
//    override fun update(ctx: BindingContext) : Boolean {
//        val newValue = ctx.resolve(pattern)
//        if (value != newValue) {
//            value = newValue
//            return true
//        }
//        return false
//    }
//}