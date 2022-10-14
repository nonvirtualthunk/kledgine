package arx.game.engine

import arx.engine.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class WorldTest {

    data class PhysicalDisplay (
        var xd: Float = 0.0f,
        var yd: Float = 0.0f
    ) : DisplayData {
        companion object : DataType<PhysicalDisplay>(PhysicalDisplay(), versioned = true)
        override fun dataType(): DataType<*> { return PhysicalDisplay }
    }

    fun WorldView.PhysicalDisplay(e : Entity, version: Long = DataContainer.LatestVersion) : PhysicalDisplay? {
        return this.data(e, PhysicalDisplay, version)
    }



    data class Physical (
        var x : Float = 0.0f,
        var y: Float = 0.0f
    ) : GameData {
        companion object : DataType<Physical>(Physical())
        override fun dataType(): DataType<*> { return Physical }
    }

    fun GameWorldView.Physical(e : Entity, version: Long = DataContainer.LatestVersion): Physical? {
        return this.data(e, Physical, version)
    }


    public inline fun withWorld(w: World, block: World.() -> Unit): Unit {
        w.block()
    }


    @Test
    fun testWorldBasics () {
        val w = World()
        val gw: GameWorld = w

        w.register(Physical)
        w.register(PhysicalDisplay)


        with(gw) {
            val entity = createEntity()

            gw.attachData(entity, Physical(-1.0f, -1.0f))
            // these are the same, the game world is just a restricted view into the overall world
            assertEquals(gw[Physical, entity], w[Physical, entity])
            assertEquals(entity[Physical], w[Physical, entity])
        }

        with(w) {
            val entity = createEntity()

            attachData(entity, Physical(1.0f, 1.0f))

            val pd = Physical(entity) ?: fail("no physical data")
            assert(pd.x == 1.0f)
            assert(PhysicalDisplay(entity) == null)

            attachData(entity, PhysicalDisplay(pd.x, pd.y), 1L)
            assert(PhysicalDisplay(entity) == PhysicalDisplay(1.0f, 1.0f))

            pd.x = 2.0f
            pd.y = 2.0f

            attachData(entity, PhysicalDisplay(pd.x, pd.y), 2L)
            assertEquals(PhysicalDisplay(2.0f, 2.0f), PhysicalDisplay(entity))
            assertEquals(PhysicalDisplay(1.0f, 1.0f), PhysicalDisplay(entity, 1L))
            assertEquals(PhysicalDisplay(1.0f, 1.0f), PhysicalDisplay(entity, 0L))
            assertEquals(PhysicalDisplay(2.0f, 2.0f), PhysicalDisplay(entity, 2L))
            assertEquals(PhysicalDisplay(2.0f, 2.0f), PhysicalDisplay(entity, 100L))

            val oldView = VersionedWorldView(w, 1)
            val newView = VersionedWorldView(w, 2)

            assertEquals(PhysicalDisplay(1.0f, 1.0f), oldView[PhysicalDisplay, entity])
            assertEquals(PhysicalDisplay(2.0f, 2.0f), newView[PhysicalDisplay, entity])

            advanceMinVersion(2)

            // advancing the min version is a hint, not a command
            assertEquals(PhysicalDisplay(1.0f, 1.0f), oldView[PhysicalDisplay, entity])
            assertEquals(PhysicalDisplay(2.0f, 2.0f), newView[PhysicalDisplay, entity])

            pd.x = 3.0f
            pd.y = 3.0f
            // but when we add new values the pervious version should get discarded internally
            attachData(entity, PhysicalDisplay(pd.x, pd.y), 3L)

            // so now the version < 2 has been discarded and we just get the oldest remaining version
            assertEquals(PhysicalDisplay(2.0f, 2.0f), oldView[PhysicalDisplay, entity])
            assertEquals(PhysicalDisplay(2.0f, 2.0f), newView[PhysicalDisplay, entity])
            assertEquals(PhysicalDisplay(3.0f, 3.0f), PhysicalDisplay(entity, 3L))

            for (i in 0 until 10) {
                attachData(entity, PhysicalDisplay(i.toFloat(), i.toFloat()), i.toLong())
            }

            // adding additional data shouldn't change the result of the views further
            // though the internal storage will be resized
            assertEquals(PhysicalDisplay(2.0f, 2.0f), oldView[PhysicalDisplay, entity])
            assertEquals(PhysicalDisplay(2.0f, 2.0f), newView[PhysicalDisplay, entity])
            assertEquals(PhysicalDisplay(3.0f, 3.0f), PhysicalDisplay(entity, 3L))
            // and the new values should be appropriately accessible
            assertEquals(PhysicalDisplay(6.0f, 6.0f), PhysicalDisplay(entity, 6L))
        }
    }
}