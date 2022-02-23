/**
 * Unit tests for the Docker module.
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
 * Unit tests for the DockerRunSettings class.
 */
internal class DockerSettingsTest : BasePlatformTestCase() {

    private lateinit var settings: DockerSettings
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        settings = DockerSettings().apply {
            targetType = TargetType.MODULE
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
        element = Element("configuration")
    }

    /**
     * Test the constructor.
     */
    fun testConstructor() {
        DockerSettings().apply {
            assertEquals(TargetType.SCRIPT, targetType)
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
     * Test round-trip write/read of settings.
     */
    fun testPersistence() {
        settings.save(element)
        element.getOrCreate(settings.xmlTagName).let {
            assertTrue(JDOMExternalizerUtil.readField(it, "id", "").isNotEmpty())
        }
        DockerSettings().apply {
            load(element)
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
     * Test the `pythonExe` attribute default value.
     */
    fun testPythonExeDefault() {
        DockerSettings().apply {
            pythonExe = ""
            assertEquals("python3", pythonExe)
            pythonExe = "abc"
            assertEquals("abc", pythonExe)
        }
    }

    /**
     * Test the `dockerExe` attribute default value.
     */
    fun testDockerExeDefault() {
        DockerSettings().apply {
            dockerExe = ""
            assertEquals("docker", dockerExe)
            dockerExe = "abc"
            assertEquals("abc", dockerExe)
        }
    }
}


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
        factory = DockerConfigurationFactory(RPythonConfigurationType())
    }

    /**
     * Test the `name` property.
     */
    fun testName() {
        assertEquals("Docker Host", factory.name)
    }
}


/**
 * Unit tests for the DockerRunConfiguration class.
 */
internal class DockerRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var config: DockerRunConfiguration
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = DockerConfigurationFactory(RPythonConfigurationType())
        config = DockerRunConfiguration(project, factory, "Docker Python Test")
        element = Element("configuration")
        element.getOrCreate(config.settings.xmlTagName).let {
            JDOMExternalizerUtil.writeField(it, "targetName", "script.py")
            JDOMExternalizerUtil.writeField(it, "pythonExe", "python")
        }
    }

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        assertEquals("Docker Python Test", config.name)
    }

    /**
     * Test the readExternal() method.
     */
    fun testReadExternal() {
        (config.settings as DockerSettings).let {
            config.readExternal(element)
            assertEquals("script.py", it.targetName)
            assertEquals("python", it.pythonExe)
            assertEquals("", it.pythonOpts)
            assertEquals(DockerHostType.IMAGE, it.hostType)
        }
    }

    /**
     * Test the writeExternal() method.
     */
    fun testWriteExternal() {
        config.writeExternal(element)
        element.getOrCreate(config.settings.xmlTagName).let {
            assertTrue(JDOMExternalizerUtil.readField(it, "id", "").isNotEmpty())
        }
    }
}


/**
 * Unit tests for the DockerSettingsEditor class.
 */
internal class DockerSettingsEditorTest : BasePlatformTestCase() {

    private val factory = DockerConfigurationFactory(RPythonConfigurationType())
    private lateinit var config: DockerRunConfiguration
    private lateinit var editor: DockerSettingsEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        config = factory.createTemplateConfiguration(project)
        editor = DockerSettingsEditor(project)
        val settings = config.settings as DockerSettings
        settings.apply {
            pythonExe = "/bin/python"
            hostName = "image:latest"
            hostType = DockerHostType.IMAGE
        }
    }

    /**
     * Test default editor settings
     */
    fun testDefault() {
        editor.applyTo(config)
        (config.settings as DockerSettings).apply {
            assertEquals("python3", pythonExe)
            assertEquals("docker", dockerExe)
        }

    }

    /**
     * Test round-trip set/get of editor fields.
     */
    fun testEditor() {
        editor.resetFrom(config)
        config.settings = (config.factory as DockerConfigurationFactory).createSettings()
        editor.applyTo(config)
        (config.settings as DockerSettings).apply {
            assertEquals("/bin/python", pythonExe)
            assertEquals("image:latest", hostName)
            assertEquals(DockerHostType.IMAGE, hostType)
        }
    }
}
