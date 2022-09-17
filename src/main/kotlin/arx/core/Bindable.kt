package arx.core

import java.lang.reflect.Field


class BindingContext(val mappings: Map<String, Any>, val parent: BindingContext?) {

    fun resolve(bindingPattern: String) : Any? {
        return mappings[bindingPattern] ?: parent?.resolve(bindingPattern)
    }

}

interface Bindable<T> {
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
}

inline fun <reified T>patternBindable(defaultValue: T, pattern: String) : Bindable<T> {
    return object : Bindable<T> {
        var currentValue: T = defaultValue
        override fun invoke(): T {
            return currentValue
        }

        override fun update(ctx: BindingContext) : Boolean {
            val bound = ctx.resolve(pattern)
            if (bound != null) {
                return if (T::class.java.isAssignableFrom(bound.javaClass)) {
                    if (currentValue != bound) {
                        currentValue = bound as T
                        true
                    } else {
                        false
                    }
                } else {
                    Noto.recordError("bound value of incorrect type", mapOf("desiredType" to T::class.simpleName, "actualType" to bound.javaClass.simpleName))
                    false
                }
            } else {
                return if (currentValue != null) {
                    currentValue = defaultValue
                    true
                } else {
                    false
                }
            }
        }

    }
}

fun <T>bindable(b: T) : Bindable<T>{
    return ValueBindable(b)
}

data class ValueBindable<T>(val value: T) : Bindable<T> {
    companion object {
        val True = bindable(true)
        val Zero = bindable(0)
        private val NullRef = bindable(null)

        @Suppress("UNCHECKED_CAST")
        fun<T> Null() : ValueBindable<T?> {
            return NullRef as ValueBindable<T?>
        }
    }

    override fun invoke(): T {
        return value
    }

    override fun update(ctx: BindingContext) : Boolean {
        // do nothing, we have a fixed value
        return false
    }
}



//data class PatternBindable<T>(val pattern: String) : Bindable<T> {
//    var currentValue: T? = null
//
//    override fun invoke(): T? {
//        return currentValue
//    }
//
//    override fun update(ctx: BindingContext) {
//        currentValue = ctx.resolve(pattern)
//    }
//}