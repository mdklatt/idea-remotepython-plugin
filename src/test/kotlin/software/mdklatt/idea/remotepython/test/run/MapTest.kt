package software.mdklatt.idea.remotepython.test.run

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.remotepython.run.getKey
import kotlin.test.assertFailsWith


/**
 * Unit tests for Map extensions.
 */
internal class MapTest {

    private val map = mapOf("key1" to "value1")

    /**
     * Test the getKey() extension method.
     */
    @Test
    fun testGetKey() {
        assertEquals("key1", map.getKey("value1"))
        assertFailsWith<RuntimeException>(
            block = { map.getKey("value2") }
        )
    }
}
