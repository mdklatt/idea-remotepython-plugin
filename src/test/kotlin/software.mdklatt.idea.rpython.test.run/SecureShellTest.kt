/**
 * Unit tests for the Remote module.
 */
package software.mdklatt.idea.rpython.test.run

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jdom.Element
import org.junit.jupiter.api.Test
import software.mdklatt.idea.rpython.run.RPythonConfigurationType
import software.mdklatt.idea.rpython.run.TargetType
import software.mdklatt.idea.rpython.run.SecureShellConfigurationFactory
import software.mdklatt.idea.rpython.run.SecureShellRunSettings


/**
 * Unit tests for the RemoteConfigurationFactory class.
 */
class SecureShellConfigurationFactoryTest {

    private val factory = SecureShellConfigurationFactory(RPythonConfigurationType())

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
 * Unit tests for the RemoteRunSettings class.
 */
class SecureShellRunSettingsTest {

    private var settings = SecureShellRunSettings().apply {
        targetType = TargetType.MODULE
        target = "pymod"
        targetParams = "-w INFO"
        python = "python3.7"
        pythonOpts = "one \"two\""
        remoteWorkDir = "abc/"
        ssh = "/bin/ssh"
        sshUser = "user"
        sshHost = "example.com"
        localWorkDir = "/home/user"
    }

    /**
     * Test the primary constructor.
     */
    @Test
    fun testCtor() {
        SecureShellRunSettings().apply {
            assertEquals(TargetType.SCRIPT, targetType)
            assertEquals("", target)
            assertEquals("", targetParams)
            assertEquals("python3", python)
            assertEquals("", pythonOpts)
            assertEquals("", remoteWorkDir)
            assertEquals("ssh", ssh)
            assertEquals("", sshUser)
            assertEquals("", sshHost)
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
        SecureShellRunSettings(element).apply {
            assertEquals(targetType, settings.targetType)
            assertEquals(target, settings.target)
            assertEquals(targetParams, settings.targetParams)
            assertEquals(python, settings.python)
            assertEquals(pythonOpts, settings.pythonOpts)
            assertEquals(remoteWorkDir, settings.remoteWorkDir)
            assertEquals(ssh, settings.ssh)
            assertEquals(sshHost, settings.sshHost)
            assertEquals(sshUser, settings.sshUser)
            assertEquals(localWorkDir, settings.localWorkDir)
        }
    }

    /**
     * Test the python property.
     */
    @Test
    fun testPython() {
        SecureShellRunSettings().apply {
            python = ""
            assertEquals("python3", python)
            python = "abc"
            assertEquals("abc", python)
        }
    }

    /**
     * Test the ssh property.
     */
    @Test
    fun testSsh() {
        SecureShellRunSettings().apply {
            ssh = ""
            assertEquals("ssh", ssh)
            ssh = "abc"
            assertEquals("abc", ssh)
        }
    }
}
