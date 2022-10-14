package hfcom.display

import arx.core.*
import arx.core.RGBA
import arx.core.Resources.image
import arx.display.components.Cameras
import arx.display.components.get
import arx.display.core.*
import arx.display.windowing.Widget
import arx.display.windowing.WidgetOrientation
import arx.display.windowing.WidgetPosition
import arx.display.windowing.WindowingSystem
import arx.engine.*
import arx.engine.Event
import hfcom.application.MainCamera
import hfcom.display.TacticalMapComponent.updateMousedOverTile
import hfcom.game.*
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.roundToInt

object TacticalMapComponent : DisplayComponent() {
    val vao = VAO(MinimalVertex())
    val tb = TextureBlock(2048)
    val shader = Resources.shader("hfcom/shaders/world")

    var mousedOverTile: MapCoord? = null
    var selectedCharacter: Entity? = null
        set(selc) {
            field = selc
            clearPreviewState()
        }
    var forceRedraw = false
    val activeActionWatcher: Watcher1<World, ActionIdentifier?> = Watcher1 {
        selectedCharacter?.get(CharacterData)?.activeAction
    }

    var previewPath: Pathfinder.Path<MapCoord>? = null
    var previewPathTiles: Set<MapCoord> = setOf()
    var chosenTargets: List<EffectTarget> = listOf()
    var previewTarget: EffectTarget? = null

    var previewUpdatePending = false

    var previewWidgets : MutableMap<Pair<EffectTarget, Effect>, Widget> = mutableMapOf()
    var noPreviewWidgets : Boolean = false

    val actionsWidget = initWithWorld { this[WindowingSystem].createWidget("TacticalWidgets.ActionsWidget") }

    fun clearPreviewState(clearPathing: Boolean = true, clearActionState: Boolean = true) {
        forceRedraw = true
        if (clearActionState) {
            chosenTargets = emptyList()
            previewTarget = null
        }
        if (clearPathing) {
            previewPath = null
            previewPathTiles = setOf()
        }
    }

    fun renderQuad(p: Vec3f, image: ImageRef, dimensions: Vec2f, drawColor : RGBA = White) {
        val tc = tb.getOrUpdate(image.toImage())
        for (q in 0 until 4) {
            vao.addV().apply {
                vertex = p + (CenteredUnitSquare3D[q] * Vec3f(dimensions.x, dimensions.y, 1.0f))
                texCoord = tc[q]
                color = drawColor
            }
        }
        vao.addIQuad()
    }

    fun renderUIQuad(p: Vec3f, image: ImageRef) {
        val img = image.toImage()
        val dim = Vec2f(img.width.toFloat() / 32.0f, img.height.toFloat() / 32.0f)
        renderQuad(p, image, dim)
    }

    fun World.renderEntity(entity: Entity, animContext: AnimationContext, p: Vec3f) {
        val cd = entity[CharacterData] ?: return
        val pd = entity[Physical] ?: return
        val cc = CharacterClasses[cd.characterClass] ?: return


        val deadness = if (cd.dead) {
            animContext.animationsForEntityAndType(entity, CharacterDeathAnimation::class)?.f ?: 1.0f
        } else {
            0.0f
        }

        val allColor = ((1.0f - deadness) * 255).toUInt().clamp(0u, 255u).let { RGBA(it,it,it,it) }
        val mainColor = (animContext.animationsForEntityAndType(entity, TintAnimation::class)?.currentColor() ?: White) * allColor

        if (cd.faction == t("Factions.Player")) {
            renderQuad(p, image("hfcom/display/ui/friendly_marker.png"), Vec2f(1.0f, 1.0f), allColor)
        } else {
            renderQuad(p, image("hfcom/display/ui/enemy_marker.png"), Vec2f(1.0f, 1.0f), allColor)
        }
        if (previewTarget == EffectTarget.Entity(entity)) {
            val outline = Sprites.imageOutline(cc.image, listOf(RGBA(123u, 14u, 14u, 255u), RGBA(170u, 29u, 29u, 255u), RGBA(193u, 62u, 62u, 255u)))
            renderQuad(p, outline, Vec2f(1.0f, 1.0f), allColor)
        }
        renderQuad(p, cc.image, Vec2f(1.0f, 1.0f), mainColor)

        if (selectedCharacter == entity) {
            renderQuad(p + Vec3f(0.0f, 0.5f, 0.0f), image("hfcom/display/ui/selection_arrow.png"), Vec2f(0.25f, 0.25f))
        }

        val maxHP = effectiveCombatStats(entity).maxHP
        val hpLost = animContext.animationsForEntityAndType(entity, DamageAnimation::class)?.currentHPLost() ?: cd.hpLost
        val curHP = maxHP - hpLost
        val hpImg = image("hfcom/display/ui/hp_point_small.png")
        val lostHpImg = image("hfcom/display/ui/hp_point_small_frame.png")
        val hpDim = hpImg.dimensions.toFloat() / 48.0f
        val hpOffset = (hpImg.dimensions.x - 1).toFloat() / 48.0f
        for (i in 0 until maxHP) {
            val img = if (i < curHP) {
                hpImg
            } else {
                lostHpImg
            }
            renderQuad(p + MapCoord.project(0, 0, 1) + Vec3f((hpOffset * maxHP) * -0.5f + hpOffset * i.toFloat(), 0.15f, 0.0f), img, hpDim, allColor)
        }
    }

    override fun update(world: World): Boolean {
        with(world) {
            if (activeActionWatcher.hasChanged(world) || previewUpdatePending) {
                clearPreviewState()
                updatePreviewedSelection()
            }

            this[WindowingSystem].desktop.bind("selectedCharacter", selectedCharacter?.let { it[CharacterData] })
            updateActionsWidget()

            val AD = world.global(AnimationData)!!
            val animContext = AD.animationContext
            if (vao.modificationCounter == 0 || animContext.nonEmpty() || forceRedraw) {
                forceRedraw = false
                vao.reset()

                val tm = world.global(TacticalMap)!!

                val p = Vec3f()

                for (x in (tm.tiles.dimensions.x - 1) downTo 0) {
                    for (y in (tm.tiles.dimensions.y - 1) downTo 0) {

                        val tile = tm.tiles[x, y]


                        val startZ = if (x == 0 || y == 0) {
                            0
                        } else {
                            val thisTerrStart = tile.terrains.size - 1
                            val adjXTerrStart = tm.tiles[x - 1, y].terrains.size
                            val adjYTerrStart = tm.tiles[x, y - 1].terrains.size
                            max(min(min(thisTerrStart, adjXTerrStart), adjYTerrStart), 0)
                        }

                        for (z in startZ until tile.terrains.size + 2) {
                            MapCoord.project(x, y, z, p)

                            tile.terrains.getOrNull(z)?.ifPresent { terrain ->
                                renderQuad(p, Terrains[terrain]!!.images[0].toImage(), Vec2f(1.0f, 1.0f))

                                if (previewPathTiles.contains(MapCoord(x, y, z + 1))) {
                                    renderQuad(p, image("hfcom/display/ui/tile_selection_blue.png"), Vec2f(1.0f, 1.0f))
                                }
                            }

                            if (mousedOverTile != null && mousedOverTile!!.x == x && mousedOverTile!!.y == y && mousedOverTile!!.z == z) {
                                val tc = tb.getOrUpdate(image("hfcom/display/ui/tile_cursor.png"))

                                for (q in 0 until 4) {
                                    vao.addV().apply {
                                        vertex = p + CenteredUnitSquare3D[q]
                                        texCoord = tc[q]
                                        color = RGBA(255u, 255u, 255u, 255u)
                                    }
                                }
                                vao.addIQuad()
                            }

                            for ((e, c) in animContext.positionAnimatedEntitiesAt(MapCoord(x, y, z))) {
                                renderEntity(e, animContext, c.project())
                            }

                            for (entity in tile.entities) {
                                entity[Physical]?.let { pd ->
                                    if (pd.position.z == z) {
                                        pd.position.project(p)
                                        if (!animContext.hasPositionAnimation(entity)) {
                                            renderEntity(entity, animContext, p)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                return true
            } else {
                return false
            }
        }
    }

    fun World.updateActionsWidget() {
        val selc = selectedCharacter ?: return
        actionsWidget().bind("actions", mapOf("activeAction" to activeAction(selc)))
    }

    override fun draw(world: World) {
        shader.bind()
        tb.bind()

        val matrix = world[Cameras][MainCamera].run { projectionMatrix() * modelviewMatrix() }

        shader.setUniform("Matrix", matrix)

        vao.sync()
        vao.draw()
    }

    fun pixelToMapCoord(world: World, v: Vec2f): MapCoord2D? {
        val tm = world[TacticalMap] ?: return null
        val mapCoord = MapCoord2D.unproject(world[Cameras][MainCamera].unproject(v) + Vec2f(0.0f, (tm.groundLevel - 1) * -0.25f))
        return if (mapCoord.x >= 0 && mapCoord.y >= 0 && mapCoord.x < tm.tiles.dimensions.x && mapCoord.y < tm.tiles.dimensions.y) {
            mapCoord
        } else {
            null
        }
    }

    fun mapCoordToPixel(world: World, m : MapCoord) : Vec2f {
        return world[Cameras][MainCamera].project(m.project())
    }


    fun World.updatePreviewedSelection() {
        if (global(AnimationData)?.animationContext?.nonEmpty() == true) {
            previewUpdatePending = true
            return
        }
        previewUpdatePending = false

        val mc = mousedOverTile
        val tm = global(TacticalMap) ?: return

        if (mc != null) {
            previewTarget = null
            previewPathTiles = setOf()
            previewPath = null

            val tile = tm.tiles[mc.xy]
            selectedCharacter?.let { selc ->


                val pd = selc[Physical] ?: return Noto.err("selected character must be a physical entity")
                val tileCoords = tile.occupiableZLevels(pd.size).map { z -> MapCoord(mc.xy, z) }.toList()
                val tileTargets = tileCoords.map { EffectTarget.Tile(it) }
                val entityTargets = tile.entities.map { EffectTarget.Entity(it) }
                val possibleTargets = entityTargets + tileTargets


                // if there's an active action then deal with that, otherwise deal with the implicit
                // movement case
                activeAction(selc).ifLet { action ->
                    if (action.effects.isEmpty()) {
                        // do nothing
                    } else if (action.effects.size <= chosenTargets.size) {
                        Noto.warn("action already has sufficient chosen targets, this seems like a bug")
                    } else {
                        val nextTargeting = action.effects[chosenTargets.size]
                        for (target in possibleTargets) {
                            if (nextTargeting.isValidTarget(this, selc, target)) {
                                previewTarget = target
                                break
                            }
                        }
                    }
                }.orElse {
                    val pathfinder = pathfinder(this, selc)
                    val paths = tileCoords.mapNotNull { to ->
                        val targets = if (entityTargets.isEmpty()) {
                            setOf(to)
                        } else {
                            to.adjacent2D().toSet()
                                .flatMap { tm.tiles[it].occupiableZLevels(pd.size).map { z -> MapCoord(it, z) }.toList() }
                                .filter { tm.tiles[it.xy].entities.none { e -> e[Physical]?.position == it } }
                        }
                        pathfinder.findPath(selc[Physical]!!.position, { targets.contains(it) }, to)
                    }
                    if (paths.isNotEmpty()) {
                        val shortestPath = paths.minBy { it.totalCost }
                        val trimmedPath = shortestPath.subPath(maxCost = selc[CharacterData]?.ap ?: 0.0)
                        previewTarget = EffectTarget.Path(trimmedPath)

                        previewPath = trimmedPath
                        previewPathTiles = previewPath?.steps?.drop(1)?.toSet() ?: emptySet()
                    }
                }
            }
        } else {
            previewPath = null
            previewPathTiles = setOf()
        }

        updatePreviewWidget()


        forceRedraw = true
    }

    fun World.updatePreviewWidget() {
        val selc = selectedCharacter
        val action = selc?.let { activeAction(it) }
        val ws = this[WindowingSystem]
        val retainedKeys = mutableSetOf<Pair<EffectTarget, Effect>>()

        if (selc != null && action != null) {
            for (i in action.effects.indices) {
                val target = if (chosenTargets.size > i) {
                    chosenTargets[i]
                } else if (chosenTargets.size == i) {
                    previewTarget
                } else {
                    null
                }

                if (target != null) {
                    val eff = action.effects[i]

                    val arch = when (eff.effect) {
                        is Effect.AttackEffect -> "TacticalWidgets.AttackPreview"
                        Effect.Move -> null
                        Effect.NoOp -> null
                        is Effect.StatsEffect -> "TacticalWidgets.StatsPreview"
                    } ?: continue

                    val w = previewWidgets.getOrPut(target to eff.effect) { ws.createWidget(arch) }
                    retainedKeys.add(target to eff.effect)

                    val pos = when (target) {
                        is EffectTarget.Entity -> {
                            val pd = target.entity[Physical] ?: return
                            pd.position + MapCoord(0,0,2)
                        }
                        is EffectTarget.Path -> {
                            val middleStep = target.path.steps.getOrNull(target.path.steps.size / 2)
                            middleStep?.plus(MapCoord(0,0,2)) ?: MapCoord(0,0,0)
                        }
                        is EffectTarget.Tile -> {
                            target.at
                        }
                    }

                    val pixelPos = mapCoordToPixel(this, pos)
                    w.x = WidgetPosition.Pixel(Vec2i(pixelPos.x.roundToInt(), pixelPos.y.roundToInt()), WidgetOrientation.BottomLeft)
                    w.y = WidgetPosition.Pixel(Vec2i(pixelPos.x.roundToInt(), pixelPos.y.roundToInt()), WidgetOrientation.BottomLeft)

                    when (eff.effect) {
                        is Effect.AttackEffect -> attackSummary(selc, target, eff.effect.attack)
                        Effect.Move -> null
                        Effect.NoOp -> null
                        is Effect.StatsEffect -> null
                    }?.let {
                        w.bind("preview", it)
                    }
                }
            }
        }

        val toRemove = previewWidgets.keys.filter { ! retainedKeys.contains(it) }
        for (k in toRemove) {
            previewWidgets.remove(k)?.destroy()
        }
    }

    fun World.updateMousedOverTile(mc2d: MapCoord2D?) {
        val tm = this[TacticalMap]!!

        var mc: MapCoord? = null
        if (mc2d != null) {
            val size = selectedCharacter?.get(Physical)?.size ?: 1
            for (z in tm.tiles[mc2d].occupiableZLevels(size)) {
                mc = MapCoord(mc2d, z - 1)
                break
            }
        }

        if (mousedOverTile != mc) {
            mousedOverTile = mc

            updatePreviewedSelection()
        }
    }

    fun World.performPreviewedAction(position: MapCoord2D) {
        val map = global(TacticalMap) ?: return

        val tile = map.tiles[position.x, position.y]

        selectedCharacter.ifLet { selc ->
            val action = activeAction(selc) ?: MoveAction

            previewTarget?.let { target ->
                chosenTargets = chosenTargets + target
                if (chosenTargets.size == action.effects.size) {
                    performAction(selc, action, chosenTargets)
                    chosenTargets = emptyList()
                    selc[CharacterData]?.let { cd -> cd.activeAction = null }
                }

                previewTarget = null
                previewPath = null
                previewPathTiles = setOf()
            }
        }.orElse {
            tile.entities.find { it[CharacterData]?.faction == map.activeFaction }?.let {
                selectedCharacter = it
            }
        }
    }

    fun World.activeAnimations() : Boolean {
        return global(AnimationData)?.animationContext?.nonEmpty() ?: false
    }


    override fun handleEvent(world: World, event: Event) {
        with(world) {
            when (event) {
                is MouseReleaseEvent -> {
                    pixelToMapCoord(world, event.position).ifPresent {
                        if (! activeAnimations()) {
                            fireEvent(TMMouseReleaseEvent(it, event.button, event))
                        }
                    }
                }
                is MousePressEvent -> {
                    pixelToMapCoord(world, event.position).ifPresent {
                        if (! activeAnimations()) {
                            fireEvent(TMMousePressEvent(it, event.button, event))
                        }
                    }
                }
                is MouseMoveEvent -> {
                    if (! activeAnimations()) {
                        val mc = pixelToMapCoord(world, event.position)
                        if (mc != null) {
                            fireEvent(TMMouseMoveEvent(mc, event))
                        } else {
                            updateMousedOverTile(null)
                        }
                    } else {}
                }
                is TMMouseReleaseEvent -> {
                    world.performPreviewedAction(event.position)
                }
                is TMMouseMoveEvent -> {
                    updateMousedOverTile(event.position)
                }
                is KeyReleaseEvent -> {
                    when (event.key) {
                        Key.Space -> {
                            forceRedraw = true
                            for (ent in entitiesWithData(CharacterData)) {
                                startTurn(ent)
                            }
                        }
                        Key.Key1 -> {
                            selectedCharacter?.ifPresent { selc ->
                                selc[CharacterData]?.ifPresent { cd ->
                                    cd.activeAction = possibleActions(selc).keys.iterator().next()
                                    println("Selected action: ${cd.activeAction}")
                                }
                            }
                        }
                        Key.Escape -> {
                            selectedCharacter?.get(CharacterData)?.ifPresent { cd ->
                                cd.activeAction = null
                            }
                        }
                        else -> {
                            // do nothing
                        }
                    }
                }
                is AnimationEnded -> {
                    forceRedraw = true
                }
                else -> {

                }
            }
        }
    }
}