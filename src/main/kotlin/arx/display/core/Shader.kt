package arx.display.core

import arx.core.Noto
import arx.core.Vec2f
import arx.core.Vec3f
import arx.core.Vec4f
import dev.romainguy.kotlin.math.Mat4
import org.lwjgl.opengl.GL20
import java.io.File

class Shader(val path: String) {
    var program: Int = 0
    var vertexShader: Int = 0
    var fragmentShader: Int = 0

    var uniformLocations = mutableMapOf<String,Int>()

    fun bind() {
        if (program == 0) {
            load()
        }
        GL20.glUseProgram(program)
    }

    fun load() {
        program = GL20.glCreateProgram()
        GL.checkError()
        vertexShader = createShaderFromPath("$path.vertex", GL20.GL_VERTEX_SHADER)
        fragmentShader = createShaderFromPath("$path.fragment", GL20.GL_FRAGMENT_SHADER)
        GL.checkError()

        GL20.glAttachShader(program, vertexShader)
        GL20.glAttachShader(program, fragmentShader)
        GL20.glLinkProgram(program)
        GL.checkError()

        val linked = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS)
        val programLog = GL20.glGetProgramInfoLog(program)
        if (programLog.trim { it <= ' ' }.isNotEmpty()) System.err.println(programLog)
        if (linked == 0) throw AssertionError("Could not link program")
        GL.checkError()
    }

    fun uniformLocation(name: String): Int =
        uniformLocations.getOrPut(name) {
            val loc = GL20.glGetUniformLocation(program, name)
            if (loc == -1) {
                Noto.err("Invalid uniform provided : $name")
            }
            loc
        }

    fun setUniform(name: String, value: Vec2f) {
        GL20.glUniform2fv(uniformLocation(name), floatArrayOf(value.x, value.y))
    }
    fun setUniform(name: String, value: Vec3f) {
        GL20.glUniform3fv(uniformLocation(name), floatArrayOf(value.x, value.y, value.z))
    }
    fun setUniform(name: String, value: Vec4f) {
        GL20.glUniform4fv(uniformLocation(name), floatArrayOf(value.x, value.y, value.z, value.w))
    }

    fun setUniform(name: String, value: Float) {
        GL20.glUniform1f(uniformLocation(name), value)
    }
    fun setUniform(name: String, value: Int) {
        GL20.glUniform1i(uniformLocation(name), value)
    }
    fun setUniform(name: String, value: Mat4, transpose: Boolean = true) {
        GL20.glUniformMatrix4fv(uniformLocation(name), transpose, value.toFloatArray())
    }

    companion object {
        private fun createShaderFromPath(path: String, type: Int): Int {
            val shader: Int = GL20.glCreateShader(type)
            val src = File(path).readText()
            GL20.glShaderSource(shader, src)
            GL20.glCompileShader(shader)

            val compiled: Int = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS)
            val log: String = GL20.glGetShaderInfoLog(shader)
            if (log.trim { it <= ' ' }.isNotEmpty()) {
                System.err.println("Shader log:")
                System.err.println(log)
            }

            GL.checkError()
            if (compiled == 0) {
                System.err.println("Shader source\n${GL20.glGetShaderSource(shader)}")
                throw AssertionError("Could not compile shader: $path")
            }
            return shader
        }
    }
}

