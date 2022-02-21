package software.mdklatt.idea.rpython.run

import com.intellij.execution.configurations.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.getOrCreate
import org.jdom.Element
import java.util.*
import javax.swing.JComponent
import javax.swing.JTextField


/**
 * TODO
 */
class RPythonConfigurationType : ConfigurationType {
    /**
     * The ID of the configuration type. Should be camel-cased without dashes, underscores, spaces and quotation marks.
     * The ID is used to store run configuration settings in a project or workspace file and
     * must not change between plugin versions.
     */
    override fun getId(): String = this::class.java.simpleName

    /**
     * Returns the 16x16 icon used to represent the configuration type.
     *
     * @return: the icon
     */
    override fun getIcon() = AllIcons.RunConfigurations.Remote  // TODO: custom icon

    /**
     * Returns the description of the configuration type. You may return the same text as the display name of the configuration type.
     *
     * @return the description of the configuration type.
     */
    override fun getConfigurationTypeDescription() = "Run a remote Python command"

    /**
     * Returns the display name of the configuration type. This is used, for example, to represent the configuration type in the run
     * configurations tree, and also as the name of the action used to create the configuration.
     *
     * @return the display name of the configuration type.
     */
    override fun getDisplayName() = "Remote Python"

    /**
     * Returns the configuration factories used by this configuration type. Normally each configuration type provides just a single factory.
     * You can return multiple factories if your configurations can be created in multiple variants (for example, local and remote for an
     * application server).
     *
     * @return the run configuration factories.
     */
    override fun getConfigurationFactories() = arrayOf(
        SecureShellConfigurationFactory(this),
        VagrantConfigurationFactory(this),
        DockerConfigurationFactory(this),
    )
}


/**
 * Run Configuration for executing Python in a <a href="https://docs.docker.com/">Docker</a> container.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
abstract class RPythonRunConfiguration protected constructor(project: Project, configFactory: ConfigurationFactory, internal val settingsFactory: RPythonSettingsFactory, name: String) :
    RunConfigurationBase<RunProfileState>(project, configFactory, name) {

    internal var settings = settingsFactory.createSettings()

    /**
     * Read settings from an XML element.
     *
     * This is part of the RunConfiguration persistence API.
     *
     * @param element: input element.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        settings.load(element)
        return
    }

    /**
     * Write settings to an XML element.
     *
     * This is part of the RunConfiguration persistence API.

     * @param element: output element.
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        settings.save(element)
        return
    }
}


/**
 * Python execution target types.
 */
enum class TargetType { MODULE, SCRIPT }  // TODO: internal


/**
 * Manage common configuration settings.
 */
internal abstract class RPythonSettings protected constructor() {

    internal abstract val xmlTagName: String

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    private var id: UUID? = null

    var targetName = ""
    var targetType = TargetType.SCRIPT
    var targetParams = ""
    var pythonExe = ""
        get() = field.ifBlank { "python3" }
    var pythonOpts = ""
    var remoteWorkDir = ""
    var localWorkDir = ""

    /**
     * Load stored settings.
     *
     * @param element: JDOM element
     */
    internal open fun load(element: Element) {
        element.getOrCreate(xmlTagName).let {
            val str = JDOMExternalizerUtil.readField(it, "id", "")
            id = if (str.isEmpty()) UUID.randomUUID() else UUID.fromString(str)
            logger.debug("loading settings for configuration ${id.toString()}")
            targetName = JDOMExternalizerUtil.readField(it, "targetName", "")
            targetType = TargetType.valueOf(JDOMExternalizerUtil.readField(it, "targetType", "SCRIPT"))
            targetParams = JDOMExternalizerUtil.readField(it, "targetParams", "")
            pythonExe = JDOMExternalizerUtil.readField(it, "pythonExe", "")
            pythonOpts = JDOMExternalizerUtil.readField(it, "pythonOpts", "")
            remoteWorkDir = JDOMExternalizerUtil.readField(it, "remoteWorkDir", "")
            localWorkDir = JDOMExternalizerUtil.readField(it, "localWorkDir", "")
        }
        return
    }

    /**
     * Save settings.
     *
     * @param element: JDOM element
     */
    internal open fun save(element: Element) {
        val default = element.getAttributeValue("default")?.toBoolean() ?: false
        element.getOrCreate(xmlTagName).let {
            if (!default) {
                id = id ?: UUID.randomUUID()
                logger.debug("saving settings for configuration ${id.toString()}")
                JDOMExternalizerUtil.writeField(it, "id", id.toString())
            } else {
                logger.debug("saving settings for default configuration")
            }
            JDOMExternalizerUtil.writeField(it, "targetName", targetName)
            JDOMExternalizerUtil.writeField(it, "targetType", targetType.name)
            JDOMExternalizerUtil.writeField(it, "targetParams", targetParams)
            JDOMExternalizerUtil.writeField(it, "pythonExe", pythonExe)
            JDOMExternalizerUtil.writeField(it, "pythonOpts", pythonOpts)
            JDOMExternalizerUtil.writeField(it, "remoteWorkDir", remoteWorkDir)
            JDOMExternalizerUtil.writeField(it, "localWorkDir", localWorkDir)
        }
        return
    }
}


abstract class RPythonSettingsFactory {

    internal abstract fun createSettings(): RPythonSettings

}


/**
 * UI component for Docker Run Configuration settings.
 *
 * @param project: parent project
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
abstract class RPythonSettingsEditor<Config: RPythonRunConfiguration> internal constructor(project: Project) :
    com.intellij.openapi.options.SettingsEditor<Config>() {

    companion object {
        val targetTypes = listOf(
            Pair(TargetType.SCRIPT, "Script path:"),
            Pair(TargetType.MODULE, "Module name:"),
        )
        val fileChooser: FileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    }

    protected var targetName = JTextField()
    protected var targetType = ComboBox(targetTypes.map{ it.second }.toTypedArray())
    protected var targetParams = RawCommandLineEditor()
    protected var pythonExe = JTextField()
    protected var pythonOpts = RawCommandLineEditor()
    protected var remoteWorkDir = JTextField()
    protected var localWorkDir = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Local Working Directory", "", project, fileChooser)
    }

    /**
     * Add host fields to the editor UI.
     *
     * @param layout: editor layout
     */
    protected abstract fun addHostFields(layout: LayoutBuilder)

    /**
     * Add executor fields to the editor UI.
     *
     * @param layout: editor layout
     */
    protected abstract fun addExecutorFields(layout: LayoutBuilder)

    /**
     * Reset host fields from a configuration state.
     *
     * @param config input configuration
     */
    protected abstract fun resetHostFields(config: Config)

    /**
     * Apply host fields to a configuration state.
     *
     * @param config: output configuration
     */
    protected abstract fun applyHostFields(config: Config)

    /**
     * Reset host fields from a configuration state.
     *
     * @param config: input configuration
     */
    protected abstract fun resetExecutorFields(config: Config)

    /**
     * Apply host fields to a configuration state.
     *
     * @param config: output configuration.
     */
    protected abstract fun applyExecutorFields(config: Config)

    /**
     * Create the widget for this editor.
     *
     * @return UI widget
     */
    override fun createEditor(): JComponent  {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel {
            row() {
                targetType()
                targetName()
            }
            row("Parameters:") { targetParams() }
            titledRow("Remote Environment") {}
            addHostFields(this)
            row("Python interpreter:") { pythonExe() }
            row("Python options:") { pythonOpts() }
            row("Remote working directory:") { remoteWorkDir() }
            titledRow("Local Environment") {}
            addExecutorFields(this)
            row("Local working directory:") { localWorkDir() }
        }
    }

    /**
     * Reset editor fields from the configuration state.
     *
     * @param config: run configuration
     */
    final override fun resetEditorFrom(config: Config) {
        config.settings.let {
            targetName.text = it.targetName
            targetType.selectedIndex = targetTypes.map{ it.first }.indexOf(it.targetType)
            targetParams.text = it.targetParams
            pythonExe.text = it.pythonExe
            pythonOpts.text = it.pythonOpts
            remoteWorkDir.text = it.remoteWorkDir
            localWorkDir.text = it.localWorkDir
        }
        resetHostFields(config)
        resetExecutorFields(config)
    }

    /**
     * Apply editor fields to the configuration state.
     *
     * @param config: run configuration
     */
    final override fun applyEditorTo(config: Config) {
        // This apparently gets called for every key press, so performance is
        // critical.
        config.settings = config.settingsFactory.createSettings()
        config.settings.let {
            it.targetName = targetName.text
            it.targetType = targetTypes[targetType.selectedIndex].first
            it.targetParams = targetParams.text
            it.pythonExe = pythonExe.text
            it.pythonOpts = pythonOpts.text
            it.remoteWorkDir = remoteWorkDir.text
            it.localWorkDir = localWorkDir.text
        }
        applyHostFields(config)
        applyExecutorFields(config)
    }
}
