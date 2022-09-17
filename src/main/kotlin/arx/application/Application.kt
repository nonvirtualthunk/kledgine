package arx.application

import arx.core.Resources
import arx.core.Vec2f
import arx.core.Vec2i
import arx.display.core.Key
import arx.display.core.KeyModifiers
import arx.display.core.MouseButton
import arx.engine.*
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.IntBuffer

class Application {
    private var engine: Engine = Engine()
    private var activeModifiers: KeyModifiers = KeyModifiers(0)
    private var mousePosition: Vec2f = Vec2f(0.0f, 0.0f)

    init {
        System.setProperty("java.awt.headless", "true")
    }

    companion object {
        var frameBufferSize = Vec2i(0,0)
        var windowSize = Vec2i(0,0)
    }

    private var window: Long = 0
    fun run(engine: Engine) {
        this.engine = engine

        System.out.println("Hello LWJGL " + Version.getVersion() + "!")
        init()
        loop()

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(window)
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
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(800, 800, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_Q && action == GLFW_PRESS && (mods and GLFW_MOD_CONTROL) != 0) {
                glfwSetWindowShouldClose(window, true)
            }
            val ke = Key.fromGLFW(key)
            activeModifiers = KeyModifiers(mods)
            when (action) {
                GLFW_PRESS -> engine.handleEvent(KeyPressEvent(ke, activeModifiers))
                GLFW_RELEASE -> engine.handleEvent(KeyReleaseEvent(ke, activeModifiers))
                GLFW_REPEAT -> engine.handleEvent(KeyRepeatEvent(ke, activeModifiers))
            }
        }

        glfwSetMouseButtonCallback(window) { _, button, action, mods ->
            activeModifiers = KeyModifiers(mods)
            when (action) {
                GLFW_PRESS -> engine.handleEvent(MousePressEvent(mousePosition, MouseButton.fromGLFW(button), activeModifiers))
                GLFW_RELEASE -> engine.handleEvent(MouseReleaseEvent(mousePosition, MouseButton.fromGLFW(button), activeModifiers))
            }
        }

        glfwSetCursorPosCallback(window) { _, x, y ->
            val p = Vec2f(x.toFloat(), y.toFloat())
            val delta = p - mousePosition
            mousePosition = p
            engine.handleEvent(MouseMoveEvent(p, delta, activeModifiers))
        }

        glfwSetFramebufferSizeCallback(window) { _, width, height ->
            frameBufferSize = Vec2i(width, height)
            engine.handleEvent(FrameBufferSizeChangedEvent(frameBufferSize))
        }

        glfwSetWindowSizeCallback(window) { _, width, height ->
            windowSize = Vec2i(width, height)
            engine.handleEvent(WindowSizeChangedEvent(frameBufferSize))
        }

        MemoryStack.stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)
            windowSize = Vec2i(pWidth[0], pHeight[0])

            // Get the resolution of the primary monitor
            val vidmode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )

            glfwGetFramebufferSize(window, pWidth, pHeight)
            frameBufferSize = Vec2i(pWidth[0], pHeight[0])
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)

        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)

        engine.initialize()
    }


    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // Set the clear color
        GL20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f)
        GL20.glEnable(GL20.GL_BLEND)
        GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        GL20.glViewport(0, 0, frameBufferSize.x, frameBufferSize.y)
        arx.display.core.GL.checkError()

        GL20.glDisable(GL20.GL_DEPTH_TEST)
        GL20.glDisable(GL20.GL_CULL_FACE)
        arx.display.core.GL.checkError()

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            engine.updateGameState()

            if (engine.updateDisplayState()) {
                GL20.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT) // clear the framebuffer

                engine.draw()

                glfwSwapBuffers(window) // swap the color buffers
            } else {
                Thread.sleep(5)
            }

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }
}