package arx.core

import arx.display.core.Image
import arx.display.core.ImageRef
import arx.display.core.SentinelImage
import com.typesafe.config.ConfigValue
import java.lang.reflect.Field
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter


val stringBindingPattern = Regex("%\\(\\??\\s*([a-zA-Z\\d.]*\\s*)\\)")

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
        val updateFunctionsByType = mutableMapOf<Class<*>, (Any) -> Boolean>()

        fun updateBindableFields(v: Any, ctx: BindingContext) : Boolean {
            val fn = updateFunctionsByType.getOrPut(v.javaClass) {
                val relevantFields = mutableListOf<Field>()
                for (field in v.javaClass.declaredFields) {
                    if (Bindable::class.java.isAssignableFrom(field.type) && field.canAccess(v)) {
                        relevantFields.add(field)
                    }
                }

                { a ->
                    var anyChanged = false
                    for (field in relevantFields) {
                        val bindable = field.get(a) as Bindable<*>
                        if (bindable.update(ctx)) {
                            anyChanged = true
                        }

                    }
                    anyChanged
                }
            }

            return fn(v)
        }
    }

    operator fun invoke() : T

    fun update(ctx: BindingContext) : Boolean

    fun copyBindable() : Bindable<T>
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
        ValueBindable(SentinelImage.toImage())
    }
}

fun bindableBool(cv : ConfigValue?) : Bindable<Boolean> {
    return if (cv.isBool()) {
        ValueBindable(cv.asBool()!!)
    } else if (cv.isStr()) {
        val str = cv.asStr() ?: ""
        stringBindingPattern.match(str).ifLet { (pattern) ->
            BooleanPatternBindable(pattern, str.contains('?'), false) as Bindable<Boolean>
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


abstract class SimplePatternBindable<T>(val pattern : String, var value : T) : Bindable<T> {
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

class BooleanPatternBindable(pattern : String, val justCheckPresent : Boolean, value : Boolean = false) : SimplePatternBindable<Boolean>(pattern, value) {
    override fun transform(a: Any?): Boolean? {
        return when (a) {
            is Boolean -> a
            else -> {
                if (justCheckPresent) {
                    a != null
                } else {
                    null
                }
            }
        }
    }

    override fun copyBindable(): Bindable<Boolean> {
        return BooleanPatternBindable(pattern, justCheckPresent, value)
    }
}

class IntPatternBindable(pattern : String, value : Int = 0) : SimplePatternBindable<Int>(pattern, value) {
    override fun transform(a: Any?): Int? {
        return (a as? Number)?.toInt()
    }

    override fun copyBindable(): Bindable<Int> {
        return IntPatternBindable(pattern, value)
    }
}

class ImageRefPattern(pattern : String, value : ImageRef = SentinelImage) : SimplePatternBindable<ImageRef>(pattern, value) {
    override fun transform(a: Any?): ImageRef? {
        return a as? ImageRef
    }

    override fun copyBindable(): Bindable<ImageRef> {
        return ImageRefPattern(pattern, value)
    }
}

class ImagePattern(pattern : String, value : Image = SentinelImage.toImage()) : SimplePatternBindable<Image>(pattern, value) {
    override fun transform(a: Any?): Image? {
        return (a as? ImageRef)?.toImage() ?: (a as? Image)
    }

    override fun copyBindable(): Bindable<Image> {
        return ImagePattern(pattern, value)
    }
}


class RGBAPatternBindable(pattern : String, value : RGBA = White) : SimplePatternBindable<RGBA>(pattern, value) {
    override fun transform(a: Any?): RGBA? {
        return a as? RGBA
    }

    override fun copyBindable(): Bindable<RGBA> {
        return RGBAPatternBindable(pattern, value)
    }
}

class FloatPatternBindable(pattern : String, value : Float = 0.0f) : SimplePatternBindable<Float>(pattern, value) {
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