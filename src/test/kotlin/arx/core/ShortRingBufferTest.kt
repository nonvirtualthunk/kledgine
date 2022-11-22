package arx.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ShortRingBufferTest {


    @Test
    fun basicFunctionalityTest() {
        val buff = ShortRingBuffer(4)
        buff.enqueue(1)
        assertEquals(buff.dequeue(), 1.toShort())

        buff.enqueue(1)
        buff.enqueue(2)
        buff.enqueue(3)
        assertEquals(buff.dequeue(), 1.toShort())
        assertEquals(buff.size, 2)
        buff.enqueue(4)
        buff.enqueue(5)
        buff.enqueue(6)
        assertEquals(buff.size, 5)

        for (i in 2 .. 6) {
            assertEquals(buff.dequeue(), i.toShort())
        }
        assertThrows(IllegalStateException::class.java) { buff.dequeue() }
        assertEquals(buff.size, 0)
    }
}