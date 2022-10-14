package arx.core

data class VecType(val type: String, val suffix: String, val integral: Boolean, val nameOverride: String? = null)

data class Operator(val operatorName: String, val operatorChar: String)



fun main() {
    val dataTypes = arrayOf(
        VecType("Float","f", false),
        VecType("Int", "i", true),
        VecType("Short", "s", true),
        VecType("Byte", "b", true),
        VecType("UByte", "ub", true),
        VecType("Double", "d", false))
    val arities = arrayOf(2,3,4)

    val variablesByArity = arrayOf(
        arrayOf(),
        arrayOf(),
        arrayOf(arrayOf("x", "y")),
        arrayOf(arrayOf("x", "y", "z"), arrayOf("r","g","b")),
        arrayOf(arrayOf("x", "y", "z", "w"), arrayOf("r", "g", "b", "a"))
    )

    val elementWiseOperators = arrayOf(Operator("plus","+"), Operator("minus","-"), Operator("times","*"), Operator("div","/"))
//    val scalarOperators = arrayOf(Operator("plus","+"), Operator("minus","-"), Operator("times","*"), Operator("div","/"))


    var classString = "import arx.core.Axis\nimport arx.core.Axis2D\n"

    for (dt in dataTypes) {
        for (arity in arities) {
            val baseTypeName = "Vec$arity${dt.suffix}"
//            val typeNames = if (arity == 4 && dt.type == "UByte") { listOf("RGBA", baseTypeName) } else { listOf(baseTypeName) }
            val typeNames = listOf(baseTypeName)
            for (typeName in typeNames) {

                val (floatType, floatSuffix) = if (dt.type == "Double") {
                    Pair("Double", "")
                } else {
                    Pair("Float", "f")
                }

                val scalarType = if (dt.type == "Short" || dt.type == "Byte") {
                    "Int"
                } else if (dt.type == "UByte") {
                    "UInt"
                } else {
                    dt.type
                }
                val castSuffix = if (dt.type == "Short" || dt.type == "Byte" || dt.type == "UByte") {
                    ".to${dt.type}()"
                } else {
                    ""
                }

                val decl = (0 until arity).map { "var elem$it : ${dt.type} = 0.to${dt.type}()" }.joinToString(", ")

                val accessors = variablesByArity[arity].map { varArr ->
                    varArr.indices.map { i ->
                        val varName = varArr[i]
                        """
        inline var $varName: ${dt.type}
            get() = elem$i
            set(value) {
                elem$i = value
            }
                        """
                    }.joinToString("\n")
                }.joinToString("\n")

                val operations = elementWiseOperators.map { op ->
                    val piecewise = (0 until arity).map { i -> "elem$i ${op.operatorChar} other.elem$i" }.joinToString(",")

                    val piecewiseAssign = (0 until arity).map { i -> "elem$i = (elem$i ${op.operatorChar} other.elem$i)$castSuffix" }.joinToString("\n\t\t")

                    val scalar = (0 until arity).map { i -> "elem$i ${op.operatorChar} scalar" }.joinToString(",")

                    val scalarAssign = (0 until arity).map { i -> "elem$i = (elem$i ${op.operatorChar} scalar)$castSuffix" }.joinToString("\n\t\t")

                    """
        operator fun ${op.operatorName}(other : $typeName) : $typeName {
            return $typeName($piecewise)
        }
        
        operator fun ${op.operatorName}Assign(other : $typeName) {
            $piecewiseAssign
        }
        
        operator fun ${op.operatorName}(scalar : $scalarType) : $typeName {
            return $typeName($scalar)
        }
        
        operator fun ${op.operatorName}Assign(scalar : $scalarType) {
            $scalarAssign
        }
                    """
                }.joinToString("\n")


                val dotOps = (0 until arity).map { i -> "elem$i * other.elem$i" }.joinToString(" + ")

                val normalizeOps = (0 until arity).map { i -> "elem$i = elem$i / mag" }.joinToString("\n\t\t")

                val crossFunction = if (arity == 3) {
                    "fun cross(other : $typeName) : $typeName = $typeName(elem1*other.elem2-other.elem1*elem2,elem2*other.elem0-other.elem2*elem0,elem0*other.elem1-other.elem0*elem1)"
                } else {
                    ""
                }

                val mag2Function = (0 until arity).map { i -> "elem$i * elem$i" }.joinToString(" + ")

                val normalizeFunctions = if (dt.integral) {
                    ""
                } else {
                    """
        fun normalize() {
            val mag = magnitude()
            $normalizeOps
        }
        
        fun normalizeSafe() {
            val mag2 = magnitude2()
            if (mag2 == 0.0$floatSuffix) { return }
            val mag = kotlin.math.sqrt(mag2)
            $normalizeOps
        }
                    """
                }

                val extraConstructors = if (dt.type != scalarType) {
                    val altDecl = (0 until arity).map { "arg$it : $scalarType" }.joinToString(", ")
                    val assign = (0 until arity).map { "arg$it.to${dt.type}()" }.joinToString(", ")
                    "constructor($altDecl) : this($assign) {}"
                } else {
                    ""
                }


                val invokeArgs = (0 until arity).map { "arg$it : ${dt.type}" }.joinToString(", ")
                val invokeAssign = (0 until arity).map { "elem$it = arg$it" }.joinToString("\n")
                val invoke = """
        operator fun invoke($invokeArgs) {
            $invokeAssign
        }
                """

                val setterCases = (0 until arity).joinToString("\n") { "\t\t\t\t$it -> elem$it = t"}

                val getterCases = (0 until arity).joinToString("\n") { "\t\t\t\t$it -> elem$it" }
                val getter = """
        operator fun get(i: Int) : ${dt.type} {
            return when(i) {
                $getterCases
                else -> error("Attempted to retrieve invalid element from $arity dimension vector")
            }
        }
        
        operator fun set(i: Int, t: ${dt.type}) {
            when(i) {
                $setterCases
                else -> error("Attempted to set invalid element from $arity dimension vector")
            }
        }
        
        operator fun get(axis: Axis) : ${dt.type} {
            return get(axis.ordinal)            
        }
        
        operator fun get(axis: Axis2D) : ${dt.type} {
            return get(axis.ordinal)            
        }
        
        operator fun set(axis: Axis, t: ${dt.type}) {
            return set(axis.ordinal, t)            
        }
        
        operator fun get(axis: Axis2D, t : ${dt.type}) {
            return set(axis.ordinal, t)            
        }
                """

                //            if (dt.type == "UByte") {
                //                val altDecl = (0 until arity).map { "arg$it : Int" }.joinToString(", ")
                //                val assign = (0 until arity).map { "arg$it.to${dt.type}()" }.joinToString(", ")
                //                extraConstructors += """
                //                   constructor($altDecl) : this($assign)Z
                //                """
                //            }


                val toFloatConverter = if (dt.integral) {
                    val conv = (0 until arity).map { "elem$it.to$floatType()" }.joinToString(", ")
                    "fun toFloat() : Vec${arity}f { return Vec${arity}f($conv) }"
                } else {
                    ""
                }


                val separatedElems = (0 until arity).map { "\$elem$it" }.joinToString(",")
                val toString = """override fun toString(): String { return "$typeName($separatedElems)" }"""

                val piecewiseEquality = (0 until arity).map { "elem$it == other.elem$it" }.joinToString(" && ")
                val equality = """override fun equals(other : Any?) : Boolean {
                    |    if (this === other) return true
                    |    if (javaClass != other?.javaClass) return false
                    |
                    |    other as $typeName
                    |    
                    |    return $piecewiseEquality
                    |}
                """.trimMargin()

                val piecewiseHashCode = (1 until arity).map { "result = result * 31 + elem$it.hashCode()" }.joinToString("\n")
                val hashCode = """override fun hashCode() : Int {
                    |    var result = elem0.hashCode()
                    |    $piecewiseHashCode
                    |    return result
                    |}
                """.trimMargin()

                classString += """
    open class $typeName($decl) {
        $extraConstructors
    
        $invoke
    
        $accessors
        
        $operations
        
        $getter
        
        fun dot(other: $typeName) : $scalarType = $dotOps
        
        fun magnitude2() : $scalarType = $mag2Function
        
        fun magnitude() : $floatType = kotlin.math.sqrt(($mag2Function).to$floatType())
        
        $normalizeFunctions
        
        $crossFunction
        
        $toFloatConverter
        
        $toString
        
        $equality
        
        $hashCode
    }
                    
                """
            }
        }
    }


    for (arity in arities) {
        val decl = (0 until arity).joinToString(", ") { "var elem$it : T" }

        val accessors = variablesByArity[arity].map { varArr ->
            varArr.indices.map { i ->
                val varName = varArr[i]
                """
        inline var $varName: T
            get() = elem$i
            set(value) {
                elem$i = value
            }
                        """
            }.joinToString("\n")
        }.joinToString("\n")


        val setterCases = (0 until arity).joinToString("\n") { "\t\t\t\t$it -> elem$it = t"}

        val getterCases = (0 until arity).joinToString("\n") { "\t\t\t\t$it -> elem$it" }
        val getter = """
        operator fun get(i: Int) : T {
            return when(i) {
                $getterCases
                else -> error("Attempted to retrieve invalid element from $arity dimension vector")
            }
        }
        
        operator fun set(i: Int, t: T) {
            when(i) {
                $setterCases
                else -> error("Attempted to set invalid element from $arity dimension vector")
            }
        }
        
        operator fun get(axis: Axis) : T {
            return get(axis.ordinal)            
        }
        
        operator fun get(axis: Axis2D) : T {
            return get(axis.ordinal)            
        }
        
        operator fun set(axis: Axis, t : T) {
            return set(axis.ordinal, t)            
        }
        
        operator fun set(axis: Axis2D, t : T) {
            return set(axis.ordinal, t)            
        }
                """


        val invokeArgs = (0 until arity).map { "arg$it : T" }.joinToString(", ")
        val invokeAssign = (0 until arity).map { "elem$it = arg$it" }.joinToString("\n")
        val invoke = """
        operator fun invoke($invokeArgs) {
            $invokeAssign
        }
        
        fun set($invokeArgs) {
            $invokeAssign
        }
                """

        val separatedElems = (0 until arity).map { "\$elem$it" }.joinToString(",")
        val toString = """override fun toString(): String { return "Vec$arity($separatedElems)" }"""


        classString += """
    data class Vec$arity<T> ($decl) {
        $accessors
        
        $getter
        
        $invoke
        
        $toString
    }
        """
    }

    println(classString)
}