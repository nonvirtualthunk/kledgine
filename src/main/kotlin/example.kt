import arx.core.*
import arx.display.core.*
import dev.romainguy.kotlin.math.ortho
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.IntBuffer

class HelloWorld {
    // The window handle
    private var window: Long = 0
    fun run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!")
        init()
        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        kotlin.check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(500, 500, "Hello World!", NULL, NULL)
        if (window == NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (key === GLFW_KEY_ESCAPE && action === GLFW_RELEASE) glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop
        }
        stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)
    }


    class MinimalVertex : VertexDefinition() {
        private val vertexOffset = offsetFor(MinimalVertex::vertex)
        private val colorOffset = offsetFor(MinimalVertex::color)
        private val texCoordOffset = offsetFor(MinimalVertex::texCoord)

        @Attribute(location = 0)
        var vertex: Vec3f
            get() {
                return getInternal(vertexOffset, Vec3f())
            }
            set(v) {
                setInternal(vertexOffset, v)
            }

        @Attribute(location = 1)
        @Normalize
        var color: Vec4ub
            get() {
                return getInternal(colorOffset, Vec4ub())
            }
            set(v) {
                setInternal(colorOffset, v)
            }

        @Attribute(location = 2)
        var texCoord: Vec2f
            get() {
                return getInternal(texCoordOffset, Vec2f())
            }
            set(v) {
                setInternal(texCoordOffset, v)
            }

        override fun toString(): String {
            return "MinimalVertex(vertex=$vertex, color=$color, texCoord=$texCoord)"
        }


    }


    var i = 0

    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)


        glViewport(0, 0, 1000, 1000)
        arx.display.core.GL.checkError()


        val vao = VAO(MinimalVertex())

        val size = 500f
        val depth = 0f


        glDisable(GL_DEPTH_TEST)
        glDisable(GL_CULL_FACE)
        arx.display.core.GL.checkError()


        val shader = Resources.shader("arx/shaders/minimal")
        shader.bind()

        val image = Resources.image("arx/lion.png")
        val image2 = Resources.image("arx/attempted_self_portrait.png")

        val texBlock = TextureBlock(2048)
        texBlock.getOrUpdate(image)
        texBlock.getOrUpdate(image2)


        val textLayout = TextLayout.layout(RichText("Simple Text"), Vec2i(0,200), Recti(0,0,500,500), Resources.font("arx/fonts/ChevyRayExpress.ttf", 36))

        val td2 = texBlock.getOrUpdate(textLayout.quads[0].image)

        val orthoMatrix = ortho(0.0f, 1000.0f, 0.0f, 1000.0f, 0.0f, 100.0f)
        shader.setUniform("ProjectionMatrix", orthoMatrix)


        val dv = arrayOf(Vec2f(0.0f,1.0f), Vec2f(1.0f,1.0f), Vec2f(1.0f,0.0f), Vec2f(0.0f,0.0f))

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer

            vao.reset()

            for (quad in textLayout.quads) {
                val origin = Vec2f(quad.position.x, quad.position.y)
                for (q in 0 until 4) {
                    val p = origin + quad.dimensions * dv[q]
                    val td = texBlock.getOrUpdate(quad.image)
                    val res = vao.addV().apply {
                        vertex = Vec3f(p.x, 1000.0f - p.y - 1.0f, 0.0f)
                        color = Vec4ub(255u,255u,255u,255u)
                        texCoord = td[q]
                    }

                }
                vao.addIQuad()
            }
//
//            texBlock.data.writeToFile("/tmp/tb.png")
//            System.exit(0)


//            vao.addV().apply {
//                vertex = Vec3f(-size + (i / 500.0f) * size, 0.0f, depth)
//                color = Vec4ub(255u, 255u, 255u, 255u)
////                texCoord = Vec2f(0.0f, 0.0f)
//                texCoord = td2[0]
//            }
//            vao.addV().apply {
//                vertex = Vec3f(size + (i / 500.0f) * size, 0.0f, depth)
//                color = Vec4ub(255u, 255u, 255u, 255u)
////                texCoord = Vec2f(1.0f, 0.0f)
//                texCoord = td2[1]
//            }
//            vao.addV().apply {
//                vertex = Vec3f(size + (i / 500.0f) * size, size*2.0f, depth)
//                color = Vec4ub(255u, 255u, 255u, 255u)
////                texCoord = Vec2f(1.0f, 1.0f)
//                texCoord = td2[2]
//            }
//            vao.addV().apply {
//                vertex = Vec3f(-size + (i / 500.0f) * size, size*2.0f, depth)
//                color = Vec4ub(255u, 255u, 255u, 255u)
////                texCoord = Vec2f(0.0f, 1.0f)
//                texCoord = td2[3]
//            }
//            vao.addIQuad()

            vao.sync()

            i++

            texBlock.bind()
            vao.draw()

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    companion object {
        @kotlin.jvm.JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("java.awt.headless", "true")
            HelloWorld().run()
        }
    }
}