/**
 * Unit tests for the Docker module.
 */
package software.mdklatt.idea.rpython.test.run

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jdom.Element
import org.junit.jupiter.api.Test
import software.mdklatt.idea.rpython.run.*


/**
 * Unit tests for the DockerConfigurationFactory class.
 */
class DockerConfigurationFactoryTest {

    private val factory = DockerConfigurationFactory(RemotePythonConfigurationType())

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
 * Unit tests for the DockerRunSettings class.
 */
class DockerRunSettingsTest {

    private var settings = DockerRunSettings().apply {
        targetType = PythonTargetType.MODULE
        targetName = "pymod"
        targetParams = "-w INFO"
        pythonExe = "python3.7"
        pythonOpts = "one \"two\""
        remoteWorkDir = "abc/"
        dockerExe = "/usr/local/bin/docker"
        dockerOpts = "one \"two\""
        dockerCompose = "docker-compose.yml"
        hostName = "ubuntu:20.04"
        hostType = DockerHostType.IMAGE
        localWorkDir = "/home/ubuntu"
    }

    /**
     * Test the primary constructor.
     */
    @Test
    fun testCtor() {
        DockerRunSettings().apply {
            assertEquals(PythonTargetType.SCRIPT, targetType)
            assertEquals("", targetName)
            assertEquals("", targetParams)
            assertEquals("python3", pythonExe)
            assertEquals("", pythonOpts)
            assertEquals("", remoteWorkDir)
            assertEquals("docker", dockerExe)
            assertEquals("", dockerOpts)
            assertEquals("", dockerCompose)
            assertEquals("", hostName)
            assertEquals(DockerHostType.IMAGE, hostType)
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
        DockerRunSettings(element).apply {
            assertEquals(targetType, settings.targetType)
            assertEquals(targetName, settings.targetName)
            assertEquals(targetParams, settings.targetParams)
            assertEquals(pythonExe, settings.pythonExe)
            assertEquals(pythonOpts, settings.pythonOpts)
            assertEquals(remoteWorkDir, settings.remoteWorkDir)
            assertEquals(dockerExe, settings.dockerExe)
            assertEquals(dockerOpts, settings.dockerOpts)
            assertEquals(dockerCompose, settings.dockerCompose)
            assertEquals(hostName, settings.hostName)
            assertEquals(hostType, settings.hostType)
            assertEquals(localWorkDir, settings.localWorkDir)
        }
    }

    /**
     * Test the python property.
     */
    @Test
    fun testPython() {
        DockerRunSettings().apply {
            pythonExe = ""
            assertEquals("python3", pythonExe)
            pythonExe = "abc"
            assertEquals("abc", pythonExe)
        }
    }

    /**
     * Test the vagrant property.
     */
    @Test
    fun testDocker() {
        DockerRunSettings().apply {
            dockerExe = ""
            assertEquals("docker", dockerExe)
            dockerExe = "abc"
            assertEquals("abc", dockerExe)
        }
    }
}
