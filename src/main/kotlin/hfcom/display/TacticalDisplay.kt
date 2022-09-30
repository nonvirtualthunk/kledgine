package hfcom.display

import arx.core.*
import arx.core.RGBA
import arx.core.Resources.image
import arx.display.components.Cameras
import arx.display.components.get
import arx.display.core.*
import arx.engine.*
import arx.engine.Event
import hfcom.application.MainCamera
import hfcom.game.*
import java.lang.Integer.max
import java.lang.Integer.min

object TacticalMapComponent : DisplayComponent() {
    val vao = VAO(MinimalVertex())
    val tb = TextureBlock(2048)
    val shader = Resources.shader("hfcom/shaders/world")

    var mousedOverTile : MapCoord2D? = null
    var selectedCharacter : Entity? = null
    var forceRedraw = false


    var previewPath : Pathfinder.Path<MapCoord>? = null
    var previewPathTiles : Set<MapCoord> = setOf()

    fun renderQuad(p: Vec3f, image: ImageRef, dimensions : Vec2f) {
        val tc = tb.getOrUpdate(image.toImage())
        for (q in 0 until 4) {
            vao.addV().apply {
                vertex = p + (CenteredUnitSquare3D[q] * Vec3f(dimensions.x, dimensions.y, 1.0f))
                texCoord = tc[q]
                color = RGBA(255u, 255u, 255u, 255u)
            }
        }
        vao.addIQuad()
    }

    fun World.renderEntity(entity: Entity, animContext: AnimationContext, p: Vec3f) {
        val cd = entity[CharacterData]  ?: return
        val pd = entity[Physical] ?: return
        val cc = CharacterClasses[cd.characterClass] ?: return

        renderQuad(p, cc.image, Vec2f(1.0f, 1.0f))

        if (selectedCharacter == entity) {
            renderQuad(p + Vec3f(0.0f,0.5f,0.0f), image("hfcom/display/ui/selection_arrow.png"), Vec2f(0.25f, 0.25f))
        }
    }

    override fun update(world: World): Boolean {
        val AD = world.global(AnimationData)!!
        val animContext = AD.animationContext
        if (vao.modificationCounter == 0 || animContext.nonEmpty() || forceRedraw) {
            forceRedraw = false
            with(world) {
                vao.reset()

                val tm = world.global(TacticalMap)!!

                val p = Vec3f()
                for (x in (tm.tiles.dimensions.x - 1) downTo 0) {
                    for (y in (tm.tiles.dimensions.y - 1) downTo 0) {
                        val tile = tm.tiles[x, y]


                        val startZ = if (x == 0 || y == 0) {
                            0
                        } else {
                            max(min(tile.terrains.size-1, min(tm.tiles[x - 1, y].terrains.size, tm.tiles[x, y - 1].terrains.size)), 0)
                        }
                        for (z in startZ until tile.terrains.size) {
                            MapCoord.project(x, y, z, p)

                            tile.terrains[z]?.ifPresent { terrain ->
                                renderQuad(p, Terrains[terrain]!!.images[0].toImage(), Vec2f(1.0f, 1.0f))

                                if (previewPathTiles.contains(MapCoord(x,y,z+1))) {
                                    renderQuad(p, image("hfcom/display/ui/tile_selection_blue.png"), Vec2f(1.0f, 1.0f))
                                }
                            }
                        }

                        if (mousedOverTile != null && mousedOverTile!!.x == x && mousedOverTile!!.y == y) {
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

                        for ((e, c) in animContext.positionAnimatedEntitiesAt(MapCoord2D(x,y))) {
                            renderEntity(e, animContext, c.project())
                        }

                        for (entity in tile.entities) {
                            entity[Physical]?.position?.project(p)
                            if (! animContext.hasPositionAnimation(entity)) {
                                renderEntity(entity, animContext, p)
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

    override fun draw(world: World) {
        shader.bind()
        tb.bind()

        val matrix = world[Cameras][MainCamera].run { projectionMatrix() * modelviewMatrix() }

        shader.setUniform("Matrix", matrix)

        vao.sync()
        vao.draw()
    }

    fun pixelToMapCoord(world: World, v : Vec2f) : MapCoord2D? {
        val tm = world[TacticalMap] ?: return null
        val mapCoord = MapCoord2D.unproject(world[Cameras][MainCamera].unproject(v) + Vec2f(0.0f, (tm.groundLevel - 1) * -0.25f))
        return if (mapCoord.x >= 0 && mapCoord.y >= 0 && mapCoord.x < tm.tiles.dimensions.x && mapCoord.y < tm.tiles.dimensions.y) {
            mapCoord
        } else {
            null
        }
    }

    fun World.updateMousedOverTile(mc: MapCoord2D?) {
        if (mousedOverTile != mc) {
            mousedOverTile = mc

            if (mc != null) {
                val tm = this[TacticalMap]!!
                selectedCharacter?.let {selc ->
                    val mc3d = MapCoord(mc, tm.tiles[mc].occupiableZLevels(2).next())

                    if (tm.tiles[mc].entities.any { e -> isEnemy(e, selc) }) {
                        previewPath = pathfinder(this, selc)
                            .findPath(selc[Physical]?.position ?: MapCoord(0, 0, 0), { t -> t.distanceTo(mc3d) <= 1.0f }, mc3d)
                        previewPathTiles = previewPath?.steps?.drop(1)?.toSet() ?: emptySet()
                    } else {
                        previewPath = pathfinder(this, selc)
                            .findPath(selc[Physical]?.position ?: MapCoord(0, 0, 0), Pathfinder.SingleDestination(mc3d), mc3d)
                        previewPathTiles = previewPath?.steps?.drop(1)?.toSet() ?: emptySet()
                    }
                }
            } else {
                previewPath = null
                previewPathTiles = setOf()
            }

            forceRedraw = true
        }
    }

    override fun handleEvent(world: World, event: Event) {
        with(world) {
            when (event) {
                is MouseReleaseEvent -> {
                    pixelToMapCoord(world, event.position).ifPresent {
                        fireEvent(TMMouseReleaseEvent(it, event.button, event))
                    }
                }
                is MousePressEvent -> {
                    pixelToMapCoord(world, event.position).ifPresent {
                        fireEvent(TMMousePressEvent(it, event.button, event))
                    }
                }
                is MouseMoveEvent -> {
                    val mc = pixelToMapCoord(world, event.position)
                    if (mc != null) {
                        fireEvent(TMMouseMoveEvent(mc, event))
                    } else {
                        updateMousedOverTile(null)
                    }
                }
                is TMMouseReleaseEvent -> {
                    val map = world[TacticalMap] ?: return

                    val targetTileEntities = map.tiles[event.position.x, event.position.y].entities
                    if (targetTileEntities.isNotEmpty()) {
                        selectedCharacter = targetTileEntities[0]
                        forceRedraw = true
                    } else {
                        selectedCharacter.ifPresent { ent ->
                            val pd = +ent[Physical]
                            previewPath.ifPresent { path ->
                                moveCharacter(ent, path)
                                forceRedraw = true
                            }
                        }
                    }
                }
                is TMMouseMoveEvent -> {
                    updateMousedOverTile(event.position)
                }
                is KeyReleaseEvent -> {
                    if (event.key == Key.Space) {
                        forceRedraw = true
                    }
                }
                is AnimationEnded -> {
                    forceRedraw = true
                }
            }
        }
    }
}