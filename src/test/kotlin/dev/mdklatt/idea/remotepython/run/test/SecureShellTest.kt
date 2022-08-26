/**
 * Unit tests for the SecureShell module.
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
            it.hostUser = "dave"
            it.sshExe = "/bin/ssh"
            it.sshOpts = "-v"
            it.writeExternal(element)
        }
        SecureShellRunConfiguration(project, factory, "Persistence Test").let {
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
            assertEquals(config.hostUser, it.hostUser)
            assertEquals(config.sshExe, it.sshExe)
            assertEquals(config.sshOpts, it.sshOpts)
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