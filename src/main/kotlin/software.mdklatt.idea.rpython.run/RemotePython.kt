package software.mdklatt.idea.rpython.run.software.mdklatt.idea.rpython.run

import com.intellij.execution.configurations.*
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import software.mdklatt.idea.rpython.run.*
import javax.swing.JTextField


/**
 * Handle persistence of run configuration options.
 *
 * This base class defines options common to all Remote Python configurations.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
abstract class RemotePythonOptions : RunConfigurationOptions() {
    internal var targetType by string()
    internal var targetName by string()
    internal var targetParams by string()
    internal var pythonExe by string()
    internal var pythonOpts by string()
    internal var remoteWorkDir by string()
    internal var localWorkDir by string()
}


/**
 * Base class for remote Python execution run configurations.
 *
 * This base class defines options common to all Remote Python configurations.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
abstract class RemotePythonRunConfiguration<Options : RemotePythonOptions>(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) :
    RunConfigurationBase<Options>(project, factory, name) {

    final override fun getOptions(): Options {
        // Kotlin considers this an unsafe cast because generics do not have
        // runtime type information unless they are reified, which is not
        // supported for class parameters.
        @Suppress("UNCHECKED_CAST")
        return super.getOptions() as Options
    }

    // TODO: Why can't options.<property> be used as a delegate?

    var targetType: TargetType
        get() = software.mdklatt.idea.rpython.run.TargetType.valueOf(options.targetType ?: "MODULE")
        set(value) {
            options.targetType = value.name
        }
    var targetName: String
        get() = options.targetName ?: ""
        set(value) {
            options.targetName = value
        }
    var targetParams: String
        get() = options.targetParams ?: ""
        set(value) {
            options.targetParams = value
        }
    var pythonExe: String
        get() = options.pythonExe ?: "python3"
        set(value) {
            options.pythonExe = value.ifBlank { "python3" }
        }
    var pythonOpts: String
        get() = options.pythonOpts ?: ""
        set(value) {
            options.pythonOpts = value
        }
    var remoteWorkDir: String
        get() = options.remoteWorkDir ?: ""
        set(value) {
            options.remoteWorkDir = value
        }
    var localWorkDir: String
        get() = options.localWorkDir ?: ""
        set(value) {
            options.localWorkDir = value
        }
}


/**
 * Base class for run configuration UI
 *
 * @param project: the project in which the run configuration will be used
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
abstract class RemotePythonEditor<Options : RemotePythonOptions, Config : RemotePythonRunConfiguration<Options>> protected constructor(
    project: Project
) :
    SettingsEditor<Config>() {

    protected companion object {
        val targetTypeOptions = mapOf(
            TargetType.SCRIPT to "Script path:",
            TargetType.MODULE to "Module name:",
        )
        val fileChooser: FileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    }

    protected var targetType = ComboBox(targetTypeOptions.values.toTypedArray())
    protected var targetName = JTextField()
    protected var targetParams = RawCommandLineEditor()
    protected var pythonExe = JTextField()
    protected var pythonOpts = RawCommandLineEditor()
    protected var remoteWorkDir = JTextField()
    protected var localWorkDir = TextFieldWithBrowseButton().also {
        it.addBrowseFolderListener("Local Working Directory", "", project, fileChooser)
    }

    /**
     * Update UI component with options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: Config) {
        config.let {
            targetType.selectedItem = targetTypeOptions[it.targetType]
            targetName.text = it.targetName
            targetParams.text = it.targetParams
            pythonExe.text = it.pythonExe
            pythonOpts.text = it.pythonOpts
            remoteWorkDir.text = it.remoteWorkDir
            localWorkDir.text = it.localWorkDir
        }
    }

    /**
     * Update configuration with options from UI.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: Config) {
        config.let {
            it.targetName = targetName.text
            it.targetType = targetTypeOptions.getKey(targetType.selectedItem)
            it.targetParams = targetParams.text
            it.pythonExe = pythonExe.text
            it.pythonOpts = pythonOpts.text
            it.remoteWorkDir = remoteWorkDir.text
            it.localWorkDir = localWorkDir.text
        }
    }
}
