/**
 * Unit tests for the Remote module.
 */
package software.mdklatt.idea.rpython.test.run

import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.getOrCreate
import org.jdom.Element
import software.mdklatt.idea.rpython.run.*


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the RemoteConfigurationFactory class.
 */
class SecureShellConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: SecureShellConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = SecureShellConfigurationFactory(RPythonConfigurationType())
    }

    /**
     * Test the name property.
     */
    fun testName() {
        assertEquals("SSH Host", factory.name)
    }
}


/**
 * Unit tests for the RemoteRunSettings class.
 */
class SecureShellSettingsTest : BasePlatformTestCase() {

    private lateinit var settings: SecureShellSettings
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        settings = SecureShellSettings().apply {
            targetType = TargetType.MODULE
            targetName = "pymod"
            targetParams = "-w INFO"
            pythonExe = "python3.7"
            pythonOpts = "one \"two\""
            remoteWorkDir = "abc/"
            sshExe = "/bin/ssh"
            sshOpts= "-v"
            sshUser = "user"
            sshHost = "example.com"
            localWorkDir = "/home/user"
        }
        element = Element("configuration")
    }

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        SecureShellSettings().apply {
            assertEquals(TargetType.SCRIPT, targetType)
            assertEquals("", targetName)
            assertEquals("", targetParams)
            assertEquals("python3", pythonExe)
            assertEquals("", pythonOpts)
            assertEquals("", remoteWorkDir)
            assertEquals("ssh", sshExe)
            assertEquals("", sshUser)
            assertEquals("", sshHost)
            assertEquals("", localWorkDir)
        }
    }

    /**
     * Test round-trip write/read of settings.
     */
    fun testPersistence() {
        settings.save(element)
        element.getOrCreate(settings.xmlTagName).let {
            assertTrue(JDOMExternalizerUtil.readField(it, "id", "").isNotEmpty())
        }
        SecureShellSettings().apply {
            load(element)
            assertEquals(targetType, settings.targetType)
            assertEquals(targetName, settings.targetName)
            assertEquals(targetParams, settings.targetParams)
            assertEquals(pythonExe, settings.pythonExe)
            assertEquals(pythonOpts, settings.pythonOpts)
            assertEquals(remoteWorkDir, settings.remoteWorkDir)
            assertEquals(sshExe, settings.sshExe)
            assertEquals(sshOpts, settings.sshOpts)
            assertEquals(sshHost, settings.sshHost)
            assertEquals(sshUser, settings.sshUser)
            assertEquals(localWorkDir, settings.localWorkDir)
        }
    }

     /**
     * Test the pythonExe property.
     */
    fun testPythonExeDefault() {
        SecureShellSettings().apply {
            pythonExe = ""
            assertEquals("python3", pythonExe)
            pythonExe = "abc"
            assertEquals("abc", pythonExe)
        }
    }

    /**
     * Test the sshExe property.
     */
    fun testSshExeDefault() {
        SecureShellSettings().apply {
            sshExe = ""
            assertEquals("ssh", sshExe)
            sshExe = "abc"
            assertEquals("abc", sshExe)
        }
    }
}


/**
 * Unit tests for the SecureShellSettingsEditor class.
 */
internal class SecureShellSettingsEditorTest : BasePlatformTestCase() {

    private val factory = SecureShellConfigurationFactory(RPythonConfigurationType())
    private lateinit var config: SecureShellRunConfiguration
    private lateinit var editor: SecureShellSettingsEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        config = factory.createTemplateConfiguration(project)
        editor = SecureShellSettingsEditor(project)
        val settings = config.settings as SecureShellSettings
        settings.apply {
            sshHost = "example.com"
        }
    }

    /**
     * Test default editor settings
     */
    fun testDefault() {
        editor.applyTo(config)
        (config.settings as SecureShellSettings).apply {
            assertEquals("python3", pythonExe)
            assertEquals("ssh", sshExe)
        }

    }

    /**
     * Test round-trip set/get of editor fields.
     */
    fun testEditor() {
        editor.resetFrom(config)
        config.settings = (config.factory as SecureShellConfigurationFactory).createSettings()
        editor.applyTo(config)
        (config.settings as SecureShellSettings).apply {
            assertEquals("example.com", sshHost)
        }
    }
}
