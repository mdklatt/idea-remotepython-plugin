/**
 * Unit tests for the Python module.
 */
package software.mdklatt.idea.rpython.test.run

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import software.mdklatt.idea.rpython.run.RPythonConfigurationType
import kotlin.test.assertNotNull


/**
 * Unit tests for the PythonConfigurationType class.
 */
class RPythonConfigurationTypeTest {

    private var type = RPythonConfigurationType()

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
        // TODO: type.configurationFactories.isNotEmpty()
    }
}
