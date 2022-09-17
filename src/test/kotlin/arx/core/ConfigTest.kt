package arx.core

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ConfigTest {

    @Test
    internal fun name() {
        val conf = ConfigFactory.parseString(
            """
        {
            a : 3
            b : "test"
            c : [{ x: 1, y: 2 }]
        }
    """.trimIndent()
        )

        println(conf.extract("a", 4.0))
    }
}