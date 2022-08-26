/**
 * Unit tests for the Docker module.
 */
package dev.mdklatt.idea.remotepython.run.test

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.remotepython.run.*
import org.jdom.Element


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the DockerConfigurationFactory class.
 */
internal class DockerConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: DockerConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = DockerConfigurationFactory(RemotePythonConfigurationType())
    }

    /**
     * Test the testCreateTemplateConfiguration() method.
     */
    fun testCreateTemplateConfiguration() {
        // Just a smoke test to ensure that the expected RunConfiguration type
        // is returned.
        factory.createTemplateConfiguration(project).let {
            assertTrue(it.dockerExe.isNotBlank())
        }
    }
}


/**
 * Unit tests for the DockerRunConfiguration class.
 */
internal class DockerRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var factory: DockerConfigurationFactory
    private lateinit var config: DockerRunConfiguration

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = DockerConfigurationFactory(RemotePythonConfigurationType())
        config = DockerRunConfiguration(project, factory, "Docker Python Test")
    }

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        config.let {
            assertTrue(it.uid.isNotBlank())
            assertEquals(TargetType.MODULE, it.targetType)
            assertEquals("", it.targetName)
            assertEquals("", it.targetArgs)
            assertEquals("python3", it.pythonExe)
            assertEquals("", it.pythonOpts)
            assertEquals("", it.localWorkDir)
            assertEquals("", it.pythonWorkDir)
            assertEquals(DockerHostType.IMAGE, it.hostType)
            assertEquals("", it.hostName)
            assertEquals("docker", it.dockerExe)
            assertEquals("", it.dockerOpts)
            assertEquals("", it.dockerCompose)
        }
    }

    /**
     * Test round-trip write/read of configuration settings.
     */
    fun testPersistence() {
        val element = Element("configuration")
        config.let {
            it.targetType = TargetType.SCRIPT
            it.targetName = "app.py"
            it.targetArgs = "-h"
            it.pythonExe = "/bin/python"
            it.pythonOpts = "-v"
            it.localWorkDir = "./"
            it.pythonWorkDir = "/opt/app"
            it.hostType = DockerHostType.SERVICE
            it.hostName = "app"
            it.dockerExe = "/bin/docker"
            it.dockerOpts = "--rm"
            it.dockerCompose = "compose.yml"
            it.writeExternal(element)
        }
        DockerRunConfiguration(project, factory, "Persistence Test").let {
            it.readExternal(element)
            assertTrue(it.uid.isNotBlank())
            assertEquals(config.targetType, it.targetType)
            assertEquals(config.targetName, it.targetName)
            assertEquals(config.targetArgs, it.targetArgs)
            assertEquals(config.pythonExe, it.pythonExe)
            assertEquals(config.pythonOpts, it.pythonOpts)
            assertEquals(config.localWorkDir, it.localWorkDir)
            assertEquals(config.pythonWorkDir, it.pythonWorkDir)
            assertEquals(config.hostType, it.hostType)
            assertEquals(config.hostName, it.hostName)
            assertEquals(config.dockerExe, it.dockerExe)
            assertEquals(config.dockerOpts, it.dockerOpts)
            assertEquals(config.dockerCompose, it.dockerCompose)
        }
    }
}


/**
 * Unit tests for the DockerEditor class.
 */
internal class DockerEditorTest : BasePlatformTestCase() {

    private lateinit var editor: DockerEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        editor = DockerEditor()
    }

    // TODO: https://github.com/JetBrains/intellij-ui-test-robot

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        // Just a smoke test.
        assertNotNull(editor.component)
    }
}