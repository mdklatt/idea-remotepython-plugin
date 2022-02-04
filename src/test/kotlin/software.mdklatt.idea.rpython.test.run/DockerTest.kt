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

    private val factory = DockerConfigurationFactory(RPythonConfigurationType())

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
        target = "pymod"
        targetParams = "-w INFO"
        python = "python3.7"
        pythonOpts = "one \"two\""
        remoteWorkDir = "abc/"
        docker = "/usr/local/bin/docker"
        dockerOpts = "one \"two\""
        dockerCompose = "docker-compose.yml"
        dockerHost = "ubuntu:20.04"
        dockerHostType = DockerHostType.IMAGE
        localWorkDir = "/home/ubuntu"
    }

    /**
     * Test the primary constructor.
     */
    @Test
    fun testCtor() {
        DockerRunSettings().apply {
            assertEquals(PythonTargetType.SCRIPT, targetType)
            assertEquals("", target)
            assertEquals("", targetParams)
            assertEquals("python3", python)
            assertEquals("", pythonOpts)
            assertEquals("", remoteWorkDir)
            assertEquals("docker", docker)
            assertEquals("", dockerOpts)
            assertEquals("", dockerCompose)
            assertEquals("", dockerHost)
            assertEquals(DockerHostType.IMAGE, dockerHostType)
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
            assertEquals(target, settings.target)
            assertEquals(targetParams, settings.targetParams)
            assertEquals(python, settings.python)
            assertEquals(pythonOpts, settings.pythonOpts)
            assertEquals(remoteWorkDir, settings.remoteWorkDir)
            assertEquals(docker, settings.docker)
            assertEquals(dockerOpts, settings.dockerOpts)
            assertEquals(dockerCompose, settings.dockerCompose)
            assertEquals(dockerHost, settings.dockerHost)
            assertEquals(dockerHostType, settings.dockerHostType)
            assertEquals(localWorkDir, settings.localWorkDir)
        }
    }

    /**
     * Test the python property.
     */
    @Test
    fun testPython() {
        DockerRunSettings().apply {
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
    fun testDocker() {
        DockerRunSettings().apply {
            docker = ""
            assertEquals("docker", docker)
            docker = "abc"
            assertEquals("abc", docker)
        }
    }
}
