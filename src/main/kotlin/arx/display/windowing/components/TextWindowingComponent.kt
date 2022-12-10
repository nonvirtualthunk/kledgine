package arx.display.windowing.components

import arx.core.*
import arx.display.core.Key
import arx.display.windowing.*
import arx.engine.*
import com.typesafe.config.ConfigValue
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.typeOf


internal val TextTypes = setOf("textdisplay", "textinput")

data class TextDisplay(
    var text: Bindable<RichText> = bindable(RichText()),
    var multiLine: Boolean = false,
    var color: Bindable<RGBA?> = ValueBindable.Null(),
    var typeface: ArxTypeface = TextLayout.DefaultTypeface,
    var fontSize: Int? = null,
    var horizontalAlignment: HorizontalTextAlignment = HorizontalTextAlignment.Left,
) : DisplayData {
    companion object : DataType<TextDisplay>(TextDisplay()), FromConfigCreator<TextDisplay> {

        fun extractTypeface(cv: ConfigValue?) : ArxTypeface? {
            return if (cv.isStr()) {
                cv.asStr()?.split(':')?.get(0)?.let { Resources.typeface(it) }
            } else {
                null
            }
        }

        fun extractFontSize(cv: ConfigValue?) : Int? {
            return cv.asInt() ?: cv.asStr()?.split(':')?.getOrNull(1)?.toIntOrNull()
        }
        override fun createFromConfig(cv: ConfigValue?): TextDisplay? {
            return if (cv["text"] != null || TextTypes.contains(cv["type"]?.asStr())) {
                TextDisplay(
                    text = cv["text"].ifLet { bindableRichText(it) }.orElse { bindable(RichText()) },
                    multiLine = cv["mutliLine"]?.asBool() ?: false,
                    color = cv["color"].ifLet { bindableRGBAOpt(it) }.orElse { bindable(Black) },
                    typeface = (cv["font"] ?: cv["typeface"])?.let { extractTypeface(it) } ?: TextLayout.DefaultTypeface,
                    fontSize = (cv["fontSize"])?.let { it.asInt() } ?: cv["font"]?.let { extractFontSize(it) },
                    horizontalAlignment = HorizontalTextAlignment(cv["horizontalTextAlignment"] ?: cv["horizontalAlignment"]) ?: HorizontalTextAlignment.Left
                )
            } else {
                null
            }
        }
    }

    val font: ArxFont get() { return typeface.withSize(fontSize ?: typeface.baseSize) }

    override fun dataType(): DataType<*> {
        return TextDisplay
    }

    fun copy() : TextDisplay {
        return TextDisplay(
            text = text.copyBindable(),
            multiLine = multiLine,
            color = color.copyBindable(),
            typeface = typeface,
            fontSize = fontSize,
            horizontalAlignment = horizontalAlignment
        )
    }
}


sealed interface EditorOperation {
    data class Append(val string: String, val position : Int) : EditorOperation
    data class Delete(val deleted: String, val position : Int) : EditorOperation
    data class Replace(val previousText: String, val newText: String, val position : Int) : EditorOperation
}

data class TextDataChanged(val operation : EditorOperation, val src: DisplayEvent) : WidgetEvent(src)

data class TextInput(
    var textData: StringBuilder = StringBuilder(),
    var textBinding: String? = null,
    var cursorPosition: Int = 0,
    var toRichTextTransformer: (String) -> RichText = { s -> RichText(s) },
    var selectedRange: IntRange? = null,
    var undoStack : MutableList<EditorOperation> = mutableListOf(),
    var twoWayBinding : Boolean = false,
    var onChangeSet : (String) -> Unit = { _ -> },
    internal var lastBoundValue : Any? = null
) : DisplayData {
    companion object : DataType<TextInput>(TextInput()), FromConfigCreator<TextInput> {
        override fun createFromConfig(cv: ConfigValue?): TextInput? {
            return if (cv["type"].asStr()?.lowercase() == "textinput") {
                var textData : String = ""
                var bindingPattern : String? = null
                cv["text"].asStr()?.let { t ->
                    stringBindingPattern.match(t).ifLet { (pattern) ->
                        bindingPattern = pattern
                    }.orElse {
                        textData = t
                    }
                }


                TextInput(
                    textData = StringBuilder(textData),
                    textBinding = bindingPattern,
                    twoWayBinding = cv["twoWayBinding"].asBool() ?: false
                )
            } else {
                null
            }
        }
    }

    override fun dataType(): DataType<*> {
        return TextInput
    }
}


object TextWindowingComponent : WindowingComponent {

    val layoutCache = LRULoadingCache<TextLayout.Params, TextLayout>(500, 0.5f) { TextLayout.layout(it) }

    override fun dataTypes() : List<DataType<EntityData>> {
        return listOf(TextDisplay, TextInput)
    }

    override fun initializeWidget(w: Widget) {
        if (w[TextInput] != null && w[FocusSettings] == null) {
            w.attachData(FocusSettings(acceptsFocus = true))
        }
    }

    fun params(td: TextDisplay, region: Recti, alignment : HorizontalTextAlignment) : TextLayout.Params {
        return TextLayout.Params(
            text = td.text(),
            defaultFont = td.font,
            region = region,
            defaultColor = td.color(),
            horizontalAlignment = alignment,
            position = region.position
        )
    }


    fun intrinsicSizeFor(params : TextLayout.Params, axis : Axis2D) : Int {
        val layout = if (params.text.isEmpty()) {
            layoutCache.getOrPut(params.copy(text = RichText("|")))
        } else {
            layoutCache.getOrPut(params)
        }

        val raw = layout.max[axis] - layout.min[axis]
        return when(axis) {
            Axis2D.X -> raw + 1
            Axis2D.Y -> raw
        }
    }

    override fun intrinsicSize(w: Widget, axis: Axis2D, minSize: Vec2i, maxSize: Vec2i): Int? {
        val td = w[TextDisplay] ?: return null

        val region = Recti(0, 1, maxSize.x, maxSize.y)

        val params = params(td, region, HorizontalTextAlignment.Left)
        return intrinsicSizeFor(params, axis)
    }

    override fun render(ws: WindowingSystem, w: Widget, bounds: Recti, quadsOut: MutableList<WQuad>) {
        val td = w[TextDisplay] ?: return

        val region = Recti(w.resClientX, w.resClientY + 1, w.resClientWidth + 2, w.resClientHeight + 1)
        val layout = layoutCache.getOrPut(params(td, region, td.horizontalAlignment))

        for (squad in layout.quads) {
            quadsOut.add(WQuad(
                position = Vec3i(squad.position.x.toInt(), squad.position.y.toInt(), 0),
                dimensions = Vec2i(squad.dimensions.x.toInt(), squad.dimensions.y.toInt()),
                image = squad.image,
                color = squad.color ?: Black,
                beforeChildren = true,
                subRect = Rectf(0.0f, 1.0f, 1.0f, -1.0f)
            ))
        }
    }

    override fun updateBindings(ws: WindowingSystem, w: Widget, ctx: BindingContext) {
        val td = w[TextDisplay] ?: return

        if (td.text.update(ctx)) {
            w.markForUpdate(RecalculationFlag.Contents)
            if (w.dimensions(Axis2D.X).isIntrinsic() ) {
                w.markForUpdate(RecalculationFlag.DimensionsX)
            }
            if( w.dimensions(Axis2D.Y).isIntrinsic()) {
                w.markForUpdate(RecalculationFlag.DimensionsY)
            }
        }

        val ti = w[TextInput] ?: return
        ti.textBinding?.let { binding ->
            if (ti.textData.isEmpty()) {
                ctx.resolve(binding)?.let { bound ->
                    ti.textData = StringBuilder(bound.toString())
                    ti.cursorPosition = ti.textData.length
                    syncDisplayToData(w, ti, td)
                }
            }
            if (ti.twoWayBinding) {
                // we're grabbing the parent object so that we can call the
                // setter on the final element
                if (! binding.contains('.')) {
                    Noto.warn("Two way bindings currently require a A.B style binding with a parent and field part")
                } else {
                    val bound = ctx.resolve(binding.substringBeforeLast('.'))
                    val finalSection = binding.substringAfterLast('.')
                    if (bound !== ti.lastBoundValue) {
                        ti.lastBoundValue = bound
                        if (bound != null) {
                            val mutProp = bound::class.declaredMemberProperties.find { it.name == "value" } as? KMutableProperty<*>
                            if (mutProp != null) {
                                val setter = mutProp.setter
                                val instanceParam = setter.instanceParameter
                                if (instanceParam != null) {
                                    when (setter.parameters[1].type) {
                                        typeOf<Int>() -> {
                                            ti.onChangeSet = { str ->
                                                str.toIntOrNull()?.let { v ->
                                                    mutProp.setter.callBy(mapOf(instanceParam to bound, setter.parameters[1] to v))
                                                }
                                            }
                                        }
                                        typeOf<Float>() -> {
                                            ti.onChangeSet = { str ->
                                                str.toFloatOrNull()?.let { v ->
                                                    mutProp.setter.callBy(mapOf(instanceParam to bound, setter.parameters[1] to v))
                                                }
                                            }
                                        }
                                        typeOf<Boolean>() -> {
                                            ti.onChangeSet = { str ->
                                                str.lowercase().toBooleanStrictOrNull()?.let { v ->
                                                    mutProp.setter.callBy(mapOf(instanceParam to bound, setter.parameters[1] to v))
                                                }
                                            }
                                        }
                                        typeOf<String>() -> {
                                            ti.onChangeSet = { str ->
                                                mutProp.setter.callBy(mapOf(instanceParam to bound, setter.parameters[1] to str))
                                            }
                                        }
                                        else -> {
                                            if (w.showing()) {
                                                Noto.warn("unsupported type for two way binding on text input : ${setter.parameters[1].type}")
                                            }
                                        }
                                    }

                                    ti.onChangeSet(ti.textData.toString())

                                } else {
                                    Noto.warn("two way binding encountered a null instance param?")
                                }
                            } else {
                                Noto.warn("two way binding could not find appropriate property: ${ti.textBinding}")
                            }
                        }
                    }
                }
            }
        }
    }

    fun syncDisplayToData(w: Widget, ti: TextInput, td: TextDisplay) {
        val transformed = ti.toRichTextTransformer(ti.textData.toString())
        if (transformed != td.text()) {
            td.text = bindable(transformed)
            w.markForUpdate(RecalculationFlag.Contents)
            if (w.width.isIntrinsic()) {
                w.markForUpdate(RecalculationFlag.DimensionsX)
            }
            if (w.height.isIntrinsic()) {
                w.markForUpdate(RecalculationFlag.DimensionsY)
            }
        }
    }

    fun textDataChanged(w : Widget, ti: TextInput, td: TextDisplay, op : EditorOperation, src : DisplayEvent) {
        w.windowingSystem.fireEvent(TextDataChanged(op, src).withWidget(w))
        ti.undoStack.add(op)
        syncDisplayToData(w, ti, td)

        ti.onChangeSet(ti.textData.toString())
    }

    fun insertText(w : Widget, ti: TextInput, td: TextDisplay, str: String?, char : Char?, src : DisplayEvent) {
        ti.selectedRange.ifLet { sr ->
            val oldStr = ti.textData.substring(sr)
            val effStr = str ?: char.toString()
            ti.textData.removeRange(sr)
            ti.textData.insert(sr.first, effStr)
            textDataChanged(w, ti, td, EditorOperation.Replace(oldStr, effStr, sr.first), src)
            ti.cursorPosition = sr.first + effStr.length
            ti.selectedRange = null
        }.orElse {
            if (char != null) {
                ti.textData.insert(ti.cursorPosition, char)
                textDataChanged(w, ti, td, EditorOperation.Append(char.toString(), ti.cursorPosition), src)
                ti.cursorPosition += 1
            } else if (str != null) {
                ti.textData.insert(ti.cursorPosition, str)
                textDataChanged(w, ti, td, EditorOperation.Append(str, ti.cursorPosition), src)
                ti.cursorPosition += str.length
            } else {
                Noto.err("insertText called with neither str nor char?")
            }

        }
    }

    fun performDelete(w : Widget, ti: TextInput, td: TextDisplay, src: DisplayEvent) {
        ti.selectedRange.ifLet { sr ->
            val oldStr = ti.textData.substring(sr)
            ti.textData.delete(sr.first, sr.last)
            textDataChanged(w, ti, td, EditorOperation.Delete(oldStr, sr.first), src)
            ti.cursorPosition = sr.first
            ti.selectedRange = null
        }.orElse {
            val index = ti.cursorPosition - 1
            if (index >= 0) {
                val oldStr = ti.textData[index].toString()
                ti.textData.delete(index, index + 1)
                textDataChanged(w, ti, td, EditorOperation.Delete(oldStr, index), src)
                ti.cursorPosition -= 1
            }
        }
    }

    override fun handleEvent(w: Widget, event: DisplayEvent): Boolean {
        val ti = w[TextInput] ?: return false
        val td = w[TextDisplay] ?: return false

        return when(event) {
            is CharInputEvent -> {
                insertText(w, ti, td, null, event.char, event)
                true
            }
            is WidgetKeyReleaseEvent -> {
                when (event.key) {
                    Key.Backspace -> performDelete(w, ti, td, event)
                    Key.Left -> {
                        ti.cursorPosition = (ti.cursorPosition - 1).clamp(0, ti.textData.length)
                    }
                    Key.Right -> {
                        ti.cursorPosition = (ti.cursorPosition + 1).clamp(0, ti.textData.length)
                    }
                    else -> return false
                }
                true
            }
            else -> false
        }
    }
}