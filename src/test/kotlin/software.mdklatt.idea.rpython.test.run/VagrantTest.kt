/**
 * Unit tests for the Vagrant module.
 */
package software.mdklatt.idea.rpython.test.run

import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.getOrCreate
import org.jdom.Element
import software.mdklatt.idea.rpython.run.*


/**
 * Unit tests for the VagrantConfigurationFactory class.
 */
class VagrantConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: VagrantConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = VagrantConfigurationFactory(RPythonConfigurationType())
    }

    /**
     * Test the name property.
     */
    fun testName() {
        assertEquals("Vagrant Host", factory.name)
    }

}


/**
 * Unit tests for the VagrantRunSettings class.
 */
class VagrantSettingsTest : BasePlatformTestCase() {

    private lateinit var settings: VagrantSettings
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        settings = VagrantSettings().apply {
            targetType = TargetType.MODULE
            targetName= "pymod"
            targetParams = "-w INFO"
            pythonExe = "python3.7"
            pythonOpts = "one \"two\""
            remoteWorkDir = "abc/"
            vagrantExe = "/usr/local/bin/vagrant"
            vagrantHost = "ubuntu"
            localWorkDir = "/vagrant"
        }
        element = Element("configuration")
    }

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        VagrantSettings().apply {
            assertEquals(TargetType.SCRIPT, targetType)
            assertEquals("", targetName)
            assertEquals("", targetParams)
            assertEquals("python3", pythonExe)
            assertEquals("", pythonOpts)
            assertEquals("", remoteWorkDir)
            assertEquals("vagrant", vagrantExe)
            assertEquals("", vagrantHost)
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
        VagrantSettings().apply {
            load(element)
            assertEquals(targetType, settings.targetType)
            assertEquals(targetName, settings.targetName)
            assertEquals(targetParams, settings.targetParams)
            assertEquals(pythonExe, settings.pythonExe)
            assertEquals(pythonOpts, settings.pythonOpts)
            assertEquals(remoteWorkDir, settings.remoteWorkDir)
            assertEquals(vagrantExe, settings.vagrantExe)
            assertEquals(vagrantHost, settings.vagrantHost)
            assertEquals(localWorkDir, settings.localWorkDir)
        }
    }

    /**
     * Test the pythonExe property.
     */
    fun testPythonExe() {
        VagrantSettings().apply {
            pythonExe = ""
            assertEquals("python3", pythonExe)
            pythonExe = "abc"
            assertEquals("abc", pythonExe)
        }
    }

    /**
     * Test the vagrantExe property.
     */
    fun testVagrantExe() {
        VagrantSettings().apply {
            vagrantExe = ""
            assertEquals("vagrant", vagrantExe)
            vagrantExe = "abc"
            assertEquals("abc", vagrantExe)
        }
    }
}


/**
 * Unit tests for the VagrantSettingsEditor class.
 */
internal class VagrantSettingsEditorTest : BasePlatformTestCase() {

    private val factory = VagrantConfigurationFactory(RPythonConfigurationType())
    private lateinit var config: VagrantRunConfiguration
    private lateinit var editor: VagrantSettingsEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        config = factory.createTemplateConfiguration(project)
        editor = VagrantSettingsEditor(project)
        val settings = config.settings as VagrantSettings
        settings.apply {
            vagrantHost = "vagrant"
        }
    }

    /**
     * Test default editor settings
     */
    fun testDefault() {
        editor.applyTo(config)
        (config.settings as VagrantSettings).apply {
            assertEquals("python3", pythonExe)
            assertEquals("vagrant", vagrantExe)
        }

    }

    /**
     * Test round-trip set/get of editor fields.
     */
    fun testEditor() {
        editor.resetFrom(config)
        config.settings = (config.factory as VagrantConfigurationFactory).createSettings()
        editor.applyTo(config)
        (config.settings as VagrantSettings).apply {
            assertEquals("python3", pythonExe)
            assertEquals("vagrant", vagrantHost)
        }
    }
}
