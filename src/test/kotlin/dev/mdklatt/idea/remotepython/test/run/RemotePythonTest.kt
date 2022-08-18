/**
 * Unit tests for the Python module.
 */
package dev.mdklatt.idea.remotepython.test.run

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import dev.mdklatt.idea.remotepython.run.RemotePythonConfigurationType
import kotlin.test.assertNotNull


/**
 * Unit tests for the PythonConfigurationType class.
 */
class RemotePythonConfigurationTypeTest {

    private var type = RemotePythonConfigurationType()

    /**
     * Test the id property.
     */
    @Test
    fun testId() {
        assertTrue(type.id.isNotBlank())
    }

    /**
     * Test the icon property.
     */
    @Test
    fun testIcon() {
        assertNotNull(type.icon)
    }

    /**
     * Test the configurationTypeDescription property.
     */
    @Test
    fun testConfigurationTypeDescription() {
        assertTrue(type.configurationTypeDescription.isNotBlank())
    }

    /**
     * Test the displayName property.
     */
    @Test
    fun testDisplayName() {
        assertTrue(type.displayName.isNotBlank())
    }

    /**
     * Test the configurationFactories property.
     */
    @Test
    fun testConfigurationFactories() {
        type.configurationFactories.isNotEmpty()
    }
}
