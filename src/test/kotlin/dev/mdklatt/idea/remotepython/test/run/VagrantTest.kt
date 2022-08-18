/**
 * Unit tests for the Vagrant module.
 */
package dev.mdklatt.idea.remotepython.test.run

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.remotepython.run.*
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
            assertEquals(TargetType.MODULE, it.targetType)
            assertEquals("", it.targetName)
            assertEquals("", it.targetArgs)
            assertEquals("python3", it.pythonExe)
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
