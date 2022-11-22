package arx.display.components

import arx.display.core.Camera
import arx.engine.*


interface CameraID {
    val startingCamera : Camera
}

data class Cameras (
    val cameras : MutableMap<CameraID, Camera> = mutableMapOf()
) : DisplayData {
    companion object : DataType<Cameras>( Cameras(), sparse = true )
    override fun dataType() : DataType<*> { return Cameras }
}

operator fun Cameras?.get (c: CameraID) : Camera {
    return this?.cameras?.getOrPut(c) { c.startingCamera } ?: c.startingCamera
}

object CameraComponent : DisplayComponent() {
    init {
        eventPriority = Priority.Last
    }

    override fun initialize(world: World) {
        world.register(Cameras)
        world.attachData(Cameras(mutableMapOf()))
    }

    override fun handleEvent(world: World, event: Event) {
        world.global(Cameras)?.apply {
            if (event is DisplayEvent) {
                for (camera in cameras.values) {
                    camera.handleEvent(event)
                }
            }
        }
    }

    override fun update(world: World): Boolean {
        var ret = false
        world.global(Cameras)?.apply {
            for (camera in cameras.values) {
                if (camera.update()) {
                    ret = true
                }
            }
        }
        return ret
    }
}