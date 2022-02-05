/**
 * Unit tests for the Vagrant module.
 */
package software.mdklatt.idea.rpython.test.run

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jdom.Element
import org.junit.jupiter.api.Test
import software.mdklatt.idea.rpython.run.RemotePythonConfigurationType
import software.mdklatt.idea.rpython.run.PythonTargetType
import software.mdklatt.idea.rpython.run.VagrantConfigurationFactory
import software.mdklatt.idea.rpython.run.VagrantRunSettings


/**
 * Unit tests for the VagrantConfigurationFactory class.
 */
class VagrantConfigurationFactoryTest {

    private val factory = VagrantConfigurationFactory(RemotePythonConfigurationType())

    /**
     * Test the id property.
     */
    @Test
    fun testId() {
        assertTrue(factory.id.isNotBlank())
    }

    /**
     * Test the name property.
     */
    @Test
    fun testName() {
        assertTrue(factory.name.isNotBlank())
    }
}


/**
 * Unit tests for the VagrantRunSettings class.
 */
class VagrantRunSettingsTest {

    private var settings = VagrantRunSettings().apply {
        targetType = PythonTargetType.MODULE
        target = "pymod"
        targetParams = "-w INFO"
        python = "python3.7"
        pythonOpts = "one \"two\""
        remoteWorkDir = "abc/"
        vagrant = "/usr/local/bin/vagrant"
        vagrantHost = "ubuntu"
        localWorkDir = "/vagrant"
    }

    /**
     * Test the primary constructor.
     */
    @Test
    fun testCtor() {
        VagrantRunSettings().apply {
            assertEquals(PythonTargetType.SCRIPT, targetType)
            assertEquals("", target)
            assertEquals("", targetParams)
            assertEquals("python3", python)
            assertEquals("", pythonOpts)
            assertEquals("", remoteWorkDir)
            assertEquals("vagrant", vagrant)
            assertEquals("", vagrantHost)
            assertEquals("", localWorkDir)
        }
    }

    /**
     * Test round-trip write/read with a JDOM Element.
     */
    @Test
    fun testJdomElement() {
        val element = Element("configuration")
        settings.write(element)
        VagrantRunSettings(element).apply {
            assertEquals(targetType, settings.targetType)
            assertEquals(target, settings.target)
            assertEquals(targetParams, settings.targetParams)
            assertEquals(python, settings.python)
            assertEquals(pythonOpts, settings.pythonOpts)
            assertEquals(remoteWorkDir, settings.remoteWorkDir)
            assertEquals(vagrant, settings.vagrant)
            assertEquals(vagrantHost, settings.vagrantHost)
            assertEquals(localWorkDir, settings.localWorkDir)
        }
    }

    /**
     * Test the python property.
     */
    @Test
    fun testPython() {
        VagrantRunSettings().apply {
            python = ""
            assertEquals("python3", python)
            python = "abc"
            assertEquals("abc", python)
        }
    }

    /**
     * Test the vagrant property.
     */
    @Test
    fun testVagrant() {
        VagrantRunSettings().apply {
            vagrant = ""
            assertEquals("vagrant", vagrant)
            vagrant = "abc"
            assertEquals("abc", vagrant)
        }
    }
}
