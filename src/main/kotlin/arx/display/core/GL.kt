package arx.display.core

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION
import java.util.*

object GL {
    val debugEnabled = true

    @Volatile private var boundArrayBuffer : Int = 0
    @Volatile private var boundIndexBuffer : Int = 0
    @Volatile private var boundVertexArray : Int = 0
    @Volatile private var textureIndex : Int = 0


    fun bindBuffer (target : Int, name : Int) {
        checkError()
        if (target == GL_ARRAY_BUFFER) {
            if (boundArrayBuffer != name) {
                glBindBuffer(target, name)
                boundArrayBuffer = name
            }
        } else if (target == GL_ELEMENT_ARRAY_BUFFER) {
            if (boundIndexBuffer != name) {
                glBindBuffer(target, name)
                boundIndexBuffer = name
            }
        } else {
            error("Unknown target for bindBuffer")
        }
        if (checkError()) {
            println("error in binding buffer: target: $target, name: $name")
        }
    }

    fun bindVertexArray(name: Int) {
        if (boundVertexArray != name) {
            GL30.glBindVertexArray(name)
            boundVertexArray = name
            if (checkError()) {
                println("error binding vertex array: $name")
            }
        }
    }

    fun errorCodeToString(err: Int) : String {
        return when (err) {
            GL_INVALID_OPERATION -> "Invalid Operation"
            GL_INVALID_ENUM -> "Invalid enum"
            GL_INVALID_VALUE -> "Invalid value"
            GL_OUT_OF_MEMORY -> "Out of Memory"
            GL_INVALID_FRAMEBUFFER_OPERATION -> "Invalid framebuffer operation"
            else -> "Unknown error"
        }
    }
    
    fun checkError() : Boolean {
        if (debugEnabled) {
            val error = glGetError()

            if (error != 0) {
                val trace = Thread.currentThread().stackTrace
                val stack = trace.sliceArray(2 until trace.size).joinToString("\n\t")
                println("<!> OpenGL Error : ${errorCodeToString(error)}")
                println("\t$stack")
            }
            return error != 0
        }
        return false
    }

    fun activeTexture(textureIndex: Int) {
        if (this.textureIndex != textureIndex) {
            glActiveTexture(GL_TEXTURE0 + textureIndex)
            this.textureIndex = textureIndex
        }
    }

    fun bindTexture(name: Int) {
        glBindTexture(GL_TEXTURE_2D, name)
    }
}