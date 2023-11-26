/**
 * Unit tests for the Docker module.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jdom.Element
import org.testcontainers.containers.GenericContainer
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString


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
     * Test the `id` property.
     */
    fun testId() {
        assertTrue(factory.id.isNotBlank())
    }

    /**
     * Test the `name` property.
     */
    fun testName() {
        assertTrue(factory.name.isNotBlank())
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


/**
 * Unit tests for the DockerState class.
 */
internal class DockerStateTest : RemotePythonStateTest() {

    private lateinit var runConfig: RunnerAndConfigurationSettings
    private lateinit var config: DockerRunConfiguration

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = DockerConfigurationFactory(RemotePythonConfigurationType())
        runConfig = RunManager.getInstance(project).createConfiguration("Docker Test", factory)
        config = (runConfig.configuration as DockerRunConfiguration).also {
            it.targetName = "cowsay"
            it.targetArgs = "-t hello"
            it.pythonOpts = "-b"
        }
    }

    /**
     * Create a DockerState instance for testing.
     *
     * @return state object
     */
    private fun state(): DockerState {
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, runConfig).build()
        return DockerState(environment)
    }

    /**
     * Test execution for a Docker container.
     */
    fun testExecContainer() {
        config.let {
            it.hostType = DockerHostType.CONTAINER
            it.hostName = container.containerName
            it.targetType = TargetType.MODULE
            it.pythonExe = "bin/python3"
            it.pythonWorkDir = pythonVenv
        }
        state().let {
            val env = it.environment
            val process = it.execute(env.executor, env.runner).processHandler
            process.startNotify()
            process.waitFor()
            assertEquals(0, process.exitCode)
        }
    }

    /**
     * Test execution for a Docker container within a virtualenv environment.
     */
    fun testExecContainerVenv() {
        config.let {
            it.hostType = DockerHostType.CONTAINER
            it.hostName = container.containerName
            it.targetType = TargetType.MODULE
            it.pythonVenv = pythonVenv
        }
        state().let {
            val env = it.environment
            val process = it.execute(env.executor, env.runner).processHandler
            process.startNotify()
            process.waitFor()
            assertEquals(0, process.exitCode)
        }
    }

    /**
     * Test execution for a Docker image.
     */
    fun testExecImage() {
        config.let {
            it.hostType = DockerHostType.IMAGE
            it.hostName = pythonImage.dockerImageName
            it.targetType = TargetType.MODULE
            it.pythonExe = "bin/python3"
            it.pythonWorkDir = pythonVenv
        }
        state().let {
            val env = it.environment
            val process = it.execute(env.executor, env.runner).processHandler
            process.startNotify()
            process.waitFor()
            assertEquals(0, process.exitCode)
        }
    }

    /**
     * Test execution for a Docker image within a virtualenv environment.
     */
    fun testExecImageVenv() {
        config.let {
            it.hostType = DockerHostType.IMAGE
            it.hostName = pythonImage.dockerImageName
            it.targetType = TargetType.MODULE
            it.pythonVenv = pythonVenv
        }
        state().let {
            val env = it.environment
            val process = it.execute(env.executor, env.runner).processHandler
            process.startNotify()
            process.waitFor()
            assertEquals(0, process.exitCode)
        }
    }

    /**
     * Test execution for a Compose service.
     */
    fun testExecService() {
        config.let {
            it.hostType = DockerHostType.SERVICE
            it.hostName = "remote_python"
            it.targetType = TargetType.MODULE
            it.pythonExe = "bin/python3"
            it.pythonWorkDir = pythonVenv
            it.dockerCompose = composeFile.pathString
        }
        state().let {
            val env = it.environment
            val process = it.execute(env.executor, env.runner).processHandler
            process.startNotify()
            process.waitFor()
            assertEquals(0, process.exitCode)
        }
    }

    /**
     * Test execution for a Compose service within a virtualenv environment.
     */
    fun testExecServiceVenv() {
        config.let {
            it.hostType = DockerHostType.SERVICE
            it.hostName = "remote_python"
            it.targetType = TargetType.MODULE
            it.pythonVenv = pythonVenv
            it.dockerCompose = composeFile.pathString
        }
        state().let {
            val env = it.environment
            val process = it.execute(env.executor, env.runner).processHandler
            process.startNotify()
            process.waitFor()
            assertEquals(0, process.exitCode)
        }
    }

    companion object {

        private val container by lazy {
            GenericContainer(pythonImage).apply {
                start()
            }
        }
        private val composeFile = createTempFile().also {
            // Use container.dockerImageName to ensure that image is built
            // before it's needed here.
            val yaml = """
                version: "3.7"
                services:
                  remote_python:
                    image: "${container.dockerImageName}"
             """.trimIndent()
            it.toFile().writeText(yaml)
        }
    }
}
