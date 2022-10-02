/**
 * Unit tests for the SecureShell module.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertContentEquals
import org.jdom.Element


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the SecureShellConfigurationFactory class.
 */
internal class SecureShellConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: SecureShellConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = SecureShellConfigurationFactory(RemotePythonConfigurationType())
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
            assertTrue(it.sshExe.isNotBlank())
        }
    }
}


/**
 * Unit tests for the SecureShellRunConfiguration class.
 */
internal class SecureShellRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var factory: SecureShellConfigurationFactory
    private lateinit var config: SecureShellRunConfiguration

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = SecureShellConfigurationFactory(RemotePythonConfigurationType())
        config = SecureShellRunConfiguration(project, factory, "SecureShell Python Test")
    }

    /**
     * Per-test teardown.
     */
    override fun tearDown() {
        config.hostPass.value = null  // remove from credential store
        super.tearDown()
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
            assertEquals("", it.hostName)
            assertEquals("", it.hostUser)
            assertNull(it.hostPass.value)
            assertFalse(it.hostPassPrompt)
            assertEquals("ssh", it.sshExe)
            assertEquals("", it.sshOpts)
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
            it.pythonWorkDir = "/tmp"
            it.hostName = "app"
            it.hostUser = "jdoe"
            it.hostPass.value = charArrayOf('1', '2', '3', '4')
            it.sshExe = "/bin/ssh"
            it.sshOpts = "-v"
            it.writeExternal(element)
        }
        SecureShellRunConfiguration(project, factory, "Persistence Test").let {
            it.readExternal(element)
            assertEquals(config.uid, it.uid)
            assertEquals(config.targetType, it.targetType)
            assertEquals(config.targetName, it.targetName)
            assertEquals(config.targetArgs, it.targetArgs)
            assertEquals(config.pythonExe, it.pythonExe)
            assertEquals(config.pythonOpts, it.pythonOpts)
            assertEquals(config.localWorkDir, it.localWorkDir)
            assertEquals(config.pythonWorkDir, it.pythonWorkDir)
            assertEquals(config.hostName, it.hostName)
            assertEquals(config.hostUser, it.hostUser)
            assertNotNull(it.hostPass.value)
            assertContentEquals(config.hostPass.value, it.hostPass.value)
            assertEquals(config.sshExe, it.sshExe)
            assertEquals(config.sshOpts, it.sshOpts)
        }
    }

    /**
     * Test behavior of the hostPassPrompt field.
     */
    fun testHostPassPrompt() {
        val element = Element("configuration")
        config.let {
            it.hostPass.value = charArrayOf('1', '2', '3', '4')
            it.hostPassPrompt = true
            it.writeExternal(element)
        }
        SecureShellRunConfiguration(project, factory, "Password Prompt Test").let {
            // Enabling the password prompt should remove the stored password.
            it.readExternal(element)
            assertNull(config.hostPass.value)
            assertNull(it.hostPass.value)
            assertTrue(it.hostPassPrompt)
        }
    }
}


/**
 * Unit tests for the SecureShellEditor class.
 */
internal class SecureShellEditorTest : BasePlatformTestCase() {

    private lateinit var editor: SecureShellEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        editor = SecureShellEditor()
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
 * Unit tests for the SecureShellState class.
 */
internal class SecureShellStateTest : BasePlatformTestCase() {

    private lateinit var state: SecureShellState

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = SecureShellConfigurationFactory(RemotePythonConfigurationType())
        val runConfig = RunManager.getInstance(project).createConfiguration("SecureShell Test", factory)
        (runConfig.configuration as SecureShellRunConfiguration).also {
            it.hostName = "example.com"
            it.hostUser = "jdoe"
            it.targetType = TargetType.MODULE
            it.targetName = "platform"
        }
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, runConfig).build()
        state = SecureShellState(environment)
    }

    /**
     * Test the getCommand() method.
     */
    fun testGetCommand() {
        val command = "ssh jdoe@example.com \"python3 -m platform\""
        assertEquals(command, state.getCommand().commandLineString)
    }
}
