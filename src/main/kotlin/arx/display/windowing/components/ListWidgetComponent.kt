package arx.display.windowing.components


import arx.core.*
import arx.display.windowing.*
import arx.engine.*
import com.typesafe.config.ConfigValue
import java.lang.Integer.max
import java.lang.Integer.min

val bindingPattern = Regex("([a-zA-Z\\d.]+)\\s?->\\s?([a-zA-Z\\d.]+)")

data class ListWidget(
    var sourceBinding : String,
    var targetBinding : String,
    var listItemArchetype : String,
    var gapSize: Int = 0,
    var separatorArchetype : String? = null,
    var selectable : Boolean = false,
    var listItemChildren : MutableList<Widget> = mutableListOf(),
    var separatorChildren : MutableList<Widget> = mutableListOf(),
    var horizontal : Boolean = false,
    var selectedIndex : Int? = null
) : DisplayData {
    companion object : DataType<ListWidget>(ListWidget("", "", "")), FromConfigCreator<ListWidget> {
        override fun createFromConfig(cv: ConfigValue?): ListWidget? {
            val bindingStr = cv["listItemBinding"].asStr()
            val listItemArchetype = cv["listItemArchetype"].asStr()

            if ((bindingStr == null || listItemArchetype == null) && (bindingStr != null || listItemArchetype != null)) {
                Noto.err("ListWidget must have both listItemBinding and listItemArchetype")
            }
            if (bindingStr == null || listItemArchetype == null) {
                return null
            }

            val (sourceBinding, targetBinding) = bindingPattern.match(bindingStr) ?: return Noto.errAndReturn("Invalid pattern binding for list widget : $bindingStr", null)

            return ListWidget(
                sourceBinding = sourceBinding,
                targetBinding = targetBinding,
                listItemArchetype = listItemArchetype,
                separatorArchetype = cv["separatorArchetype"].asStr(),
                gapSize = cv["gapSize"].asInt() ?: 0,
                selectable = cv["selectable"].asBool() ?: false,
                horizontal = cv["horizontal"].asBool() ?: false,
            )
        }
    }

    override fun dataType(): DataType<*> {
        return ListWidget
    }

    fun copy() : ListWidget {
        return ListWidget(
            sourceBinding,
            targetBinding,
            listItemArchetype,
            gapSize,
            separatorArchetype,
            selectable,
            mutableListOf(),
            mutableListOf(),
            horizontal,
            null
        )
    }
}

data class ListWidgetItem (var data : Any? = null, val index : Int = 0) : DisplayData {
    companion object : DataType<ListWidgetItem>( ListWidgetItem() )
    override fun dataType() : DataType<*> { return ListWidgetItem }
}

data class ListItemSelected(override val widgets: MutableList<Widget>, val index: Int, val data : Any?, val from : WidgetMouseReleaseEvent) : WidgetEvent(from)

data class ListItemMousedOver(override val widgets: MutableList<Widget>, val index: Int, val dat : Any?, val from : WidgetMouseEnterEvent) : WidgetEvent(from)

object ListWidgetComponent : WindowingComponent {

    override fun dataTypes() : List<DataType<EntityData>> {
        return listOf(ListWidget)
    }


    override fun updateBindings(ws: WindowingSystem, w: Widget, ctx: BindingContext) {
        val lw = w[ListWidget] ?: return

        val boundSrcValue = ctx.resolve(lw.sourceBinding) ?: return
        if (boundSrcValue is List<*>) {
            for ((i, value) in boundSrcValue.withIndex()) {
                if (lw.listItemChildren.size <= i) {
                    val newItem = ws.createWidget(w, lw.listItemArchetype)
                    newItem.attachData(ListWidgetItem(value, i))
                    newItem.identifier = "${w.identifier}.item[$i]"
                    if (lw.selectable) {
                        newItem.onEventDo<WidgetMouseReleaseEvent> { mre ->
                            lw.selectedIndex = i
                            ws.fireEvent(ListItemSelected(mutableListOf(newItem), i, newItem[ListWidgetItem]?.data, mre))
                        }
                    }
                    newItem.onEventDo<WidgetMouseEnterEvent> { mee ->
                        ws.fireEvent(ListItemMousedOver(mutableListOf(newItem), i, newItem[ListWidgetItem]?.data, mee))
                    }

                    lw.listItemChildren.add(newItem)
                    if (i != 0) {
                        val axis = if (lw.horizontal) { Axis.X } else { Axis.Y }

                        lw.separatorArchetype.ifLet {sepArch ->
                            val separator = ws.createWidget(w, sepArch)
                            separator.identifier = "${w.identifier}.separator[${i - 1}]"
                            lw.separatorChildren.add(separator)
                            separator.position[axis] = WidgetPosition.Relative(lw.listItemChildren[i - 1], lw.gapSize)
                            newItem.position[axis] = WidgetPosition.Relative(separator, lw.gapSize)
                        }.orElse {
                            newItem.position[axis] = WidgetPosition.Relative(lw.listItemChildren[i - 1], lw.gapSize)
                        }
                    }
                }

                if (value != null) {
                    val childItem = lw.listItemChildren[i]
                    childItem.bind(lw.targetBinding, value)
                }
            }

            for (i in boundSrcValue.size until lw.listItemChildren.size) {
                lw.listItemChildren[i].destroy()
                lw.separatorChildren.getOrNull(i - 1)?.destroy()
            }

            lw.listItemChildren.dropLast(lw.listItemChildren.size - boundSrcValue.size)
            lw.separatorChildren.dropLast(max(lw.separatorChildren.size - (boundSrcValue.size - 1), 0))
        } else {
            Noto.warn("non-list value provided as binding for list widget : ${lw.sourceBinding}")
        }
    }
}