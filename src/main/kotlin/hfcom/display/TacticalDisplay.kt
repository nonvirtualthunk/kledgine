package hfcom.display

import arx.core.*
import arx.display.components.Cameras
import arx.display.components.get
import arx.display.core.Key
import arx.display.core.MinimalVertex
import arx.display.core.TextureBlock
import arx.display.core.VAO
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
    var forceRedraw = false

    fun World.renderEntity(entity: Entity, animGroup: AnimationGroup, p: Vec3f) {
        val cd = entity[CharacterData]  ?: return
        val pd = entity[Physical] ?: return
        val cc = CharacterClasses[cd.characterClass] ?: return

        val tc2 = tb.getOrUpdate(cc.image.toImage())



        for (q in 0 until 4) {
            vao.addV().apply {
                vertex = p + (CenteredUnitSquare3D[q])
                texCoord = tc2[q]
                color = RGBA(255u, 255u, 255u, 255u)
            }
        }
        vao.addIQuad()
    }

    override fun update(world: World): Boolean {
        val AD = world.global(AnimationData)
        val animGroup = AD!!.activeAnimationGroup
        if (vao.modificationCounter == 0 || animGroup.animations.isNotEmpty() || forceRedraw) {
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
                            val tc = tb.getOrUpdate(Terrains[tile.terrains[z]]!!.images[0].toImage())

                            for (q in 0 until 4) {
                                vao.addV().apply {
                                    vertex = p + CenteredUnitSquare3D[q]
                                    texCoord = tc[q]
                                    color = RGBA(255u, 255u, 255u, 255u)
                                }
                            }
                            vao.addIQuad()
                        }

                        if (mousedOverTile != null && mousedOverTile!!.x == x && mousedOverTile!!.y == y) {
                            val tc = tb.getOrUpdate(Resources.image("hfcom/display/ui/tile_cursor.png"))

                            for (q in 0 until 4) {
                                vao.addV().apply {
                                    vertex = p + CenteredUnitSquare3D[q]
                                    texCoord = tc[q]
                                    color = RGBA(255u, 255u, 255u, 255u)
                                }
                            }
                            vao.addIQuad()
                        }

                        for ((e, c) in AD.positionAnimatedEntitiesAt(MapCoord2D(x,y))) {
                            renderEntity(e, animGroup, c.project())
                        }

                        for (entity in tile.entities) {
                            entity[Physical]?.position?.project(p)
                            if (! AD.hasPositionAnimation(entity)) {
                                renderEntity(entity, animGroup, p)
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
        if (mapCoord.x >= 0 && mapCoord.y >= 0 && mapCoord.x < tm.tiles.dimensions.x && mapCoord.y < tm.tiles.dimensions.y) {
            return mapCoord
        } else {
            return null
        }
    }

    fun updateMousedOverTile(mc: MapCoord2D?) {
        if (mousedOverTile != mc) {
            mousedOverTile = mc
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
                    for (ent in entitiesWithData(CharacterData)) {
                        val pd = ent[Physical] ?: continue

                        val tile = map.tiles[pd.position.x, pd.position.y]
                        tile.entities = tile.entities - ent

                        val newTile = map.tiles[event.position.x, event.position.y]
                        newTile.entities = newTile.entities + ent
                        val oldPos = pd.position
                        pd.position = MapCoord(event.position, newTile.terrains.size)

                        fireEvent(CharacterMoved(ent, oldPos, pd.position))
                        forceRedraw = true
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