/**
 * Unit tests for the Vagrant module.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jdom.Element


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the VagrantConfigurationFactory class.
 */
internal class VagrantConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: VagrantConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = VagrantConfigurationFactory(RemotePythonConfigurationType())
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
            assertTrue(it.vagrantExe.isNotBlank())
        }
    }
}


/**
 * Unit tests for the VagrantRunConfiguration class.
 */
internal class VagrantRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var factory: VagrantConfigurationFactory
    private lateinit var config: VagrantRunConfiguration

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = VagrantConfigurationFactory(RemotePythonConfigurationType())
        config = VagrantRunConfiguration(project, factory, "Vagrant Python Test")
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
            assertEquals("", it.pythonVenv)
            assertEquals("", it.pythonOpts)
            assertEquals("", it.localWorkDir)
            assertEquals("", it.pythonWorkDir)
            assertEquals("", it.hostName)
            assertEquals("vagrant", it.vagrantExe)
            assertEquals("", it.vagrantOpts)
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
            it.pythonVenv = "venv"
            it.pythonOpts = "-v"
            it.localWorkDir = "./"
            it.pythonWorkDir = "/tmp"
            it.hostName = "app"
            it.vagrantExe = "/bin/vagrant"
            it.vagrantOpts = "-v"
            it.writeExternal(element)
        }
        VagrantRunConfiguration(project, factory, "Persistence Test").let {
            it.readExternal(element)
            assertTrue(it.uid.isNotBlank())
            assertEquals(config.targetType, it.targetType)
            assertEquals(config.targetName, it.targetName)
            assertEquals(config.targetArgs, it.targetArgs)
            assertEquals(config.pythonExe, it.pythonExe)
            assertEquals(config.pythonOpts, it.pythonOpts)
            assertEquals(config.localWorkDir, it.localWorkDir)
            assertEquals(config.pythonWorkDir, it.pythonWorkDir)
            assertEquals(config.hostName, it.hostName)
            assertEquals(config.vagrantExe, it.vagrantExe)
            assertEquals(config.vagrantOpts, it.vagrantOpts)
        }
    }
}


/**
 * Unit tests for the VagrantEditor class.
 */
internal class VagrantEditorTest : BasePlatformTestCase() {

    private lateinit var editor: VagrantEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        editor = VagrantEditor()
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
 * Unit tests for the VagrantState class.
 */
internal class VagrantStateTest : BasePlatformTestCase() {

    private lateinit var state: VagrantState

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = VagrantConfigurationFactory(RemotePythonConfigurationType())
        val runConfig = RunManager.getInstance(project).createConfiguration("Vagrant Test", factory)
        (runConfig.configuration as VagrantRunConfiguration).also {
            it.hostName = "box"
            it.targetType = TargetType.MODULE
            it.targetName = "platform"
            it.pythonOpts = "-a -b"
            it.pythonVenv = "venv"
        }
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, runConfig).build()
        state = VagrantState(environment)
    }

    /**
     * Test the getCommand() method.
     */
    fun testGetCommand() {
        val command = "vagrant ssh --command \". venv/bin/activate && python3 -a -b -m platform\" box"
        assertEquals(command, state.getCommand().commandLineString)
    }
}
