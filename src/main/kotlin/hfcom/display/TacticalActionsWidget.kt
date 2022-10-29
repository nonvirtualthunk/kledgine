package hfcom.display

import arx.core.RGBA
import arx.core.Resources
import arx.core.expectLet
import arx.core.ifPresent
import arx.display.core.Image
import arx.display.windowing.WindowingSystem
import arx.display.windowing.components.ListItemSelected
import arx.display.windowing.onEventDo
import arx.engine.World
import hfcom.game.*

class TacticalActionsWidget(val world : World) {
    val widget = world[WindowingSystem].createWidget("TacticalWidgets.ActionsWidget").apply {
        onEventDo<ListItemSelected> {
            (it.data as? ActionOption).expectLet {
                selectIndex(it.index)
            }
        }
    }

    var actionOptions : List<ActionOption> = emptyList()


    fun overallActionOrdering(ai : ActionIdentifier) : Int {
        return when(ai) {
            is ActionIdentifier.Attack -> 1
            is ActionIdentifier.CastSpell -> 3
            is ActionIdentifier.Skill -> 2
            is ActionIdentifier.Move -> 0
        }
    }

    fun tieBreaker(ai : ActionIdentifier) : String {
        return when(ai) {
            is ActionIdentifier.Attack -> ai.name
            is ActionIdentifier.CastSpell -> ai.spell.name
            is ActionIdentifier.Skill -> ai.kind.name
            is ActionIdentifier.Move -> "move"
        }
    }

    fun icon(ai: ActionIdentifier, action : Action): Image {
        return when(ai) {
            is ActionIdentifier.Attack ->
                if ((action.effects.firstNotNullOfOrNull { it.effect as? Effect.AttackEffect }
                        ?.attack?.range ?: 0) > 1
                ) {
                    Resources.image("display/ui/ranged_attack_icon.png")
                } else {
                    Resources.image("display/ui/melee_attack_icon.png")
                }
            is ActionIdentifier.CastSpell -> Resources.image("display/ui/magic_icon_1.png")
            is ActionIdentifier.Skill -> (Skills[ai.kind]?.icon ?: Resources.image("display/ui/misc_skill_icon_1.png")).toImage()
            is ActionIdentifier.Move -> Resources.image("display/ui/move_icon_2.png")
        }
    }

    data class ActionOption (
        val identifier : ActionIdentifier,
        val icon : Image,
        val name : String,
        val index : Int,
        val backgroundColor : RGBA,
        val edgeColor : RGBA,
    )

    fun update() {
        with(world) {
            val selc = TacticalMapComponent.selectedCharacter ?: return
            widget.bind("actions", mapOf("activeAction" to (activeAction(selc) ?: MoveAction)))

            val moveIdent = ActionIdentifier.Move(selc)
            val sortedPossibles = (listOf(moveIdent to MoveAction) + possibleActions(selc).toList()).sortedWith { a, b ->
                if (overallActionOrdering(a.first) < overallActionOrdering(b.first)) {
                    -1
                } else if (overallActionOrdering(a.first) > overallActionOrdering(b.first)) {
                    1
                } else {
                    tieBreaker(a.first).compareTo(tieBreaker(b.first))
                }
            }

            val activeIdent = selc[CharacterData]!!.activeActionIdentifier ?: moveIdent

            actionOptions = sortedPossibles.withIndex().map {
                val (index, v) = it
                val (ai, act) = v
                ActionOption(
                    identifier = ai,
                    icon = icon(ai, act),
                    name = act.name,
                    index = index + 1,
                    backgroundColor = if (ai == activeIdent) {
                        RGBA(128,128,128,255)
                    } else {
                        RGBA(50,50,75,255)
                    },
                    edgeColor = if (ai == activeIdent) {
                        RGBA(200,200,200,255)
                    } else {
                        RGBA(20,20,25,255)
                    },
                )
            }

            widget.bind("possibleActions", actionOptions)
        }
    }

    fun selectIndex(index : Int) {
        with(world) {
            TacticalMapComponent.selectedCharacter?.ifPresent { selc ->
                selc[CharacterData]?.ifPresent { cd ->
                    TacticalMapComponent.actionsWidget().actionOptions.firstOrNull { opt -> opt.index == index }?.ifPresent { opt ->
                        cd.activeActionIdentifier = opt.identifier
                        println("Selected action: ${cd.activeActionIdentifier}")
                    }
                }
            }
        }
    }
}