/**
 * Unit tests for the Python module.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.testcontainers.images.builder.ImageFromDockerfile
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertNotNull


/**
 * Unit tests for the PythonConfigurationType class.
 */
internal class RemotePythonConfigurationTypeTest {

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


internal abstract class RemotePythonStateTest: BasePlatformTestCase() {

    protected companion object {
        var pythonImage = ImageFromDockerfile().apply {
            val venv = "/opt/venv"
            withDockerfileFromBuilder {
                it.from("linuxserver/openssh-server:version-9.3_p2-r0")
                it.run("apk add python3")
                it.run("python3 -m venv $venv")
                it.run(". ${venv}/bin/activate && python3 -m pip install cowsay")
                it.build()
            }
        }
    }


}