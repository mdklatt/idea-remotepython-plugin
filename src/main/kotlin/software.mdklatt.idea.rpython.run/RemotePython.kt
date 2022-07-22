/**
 * IDEA run configurations for Remote Python execution.
 */
package software.mdklatt.idea.rpython.run

import com.intellij.execution.configurations.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel


/**
 * Run configuration type for remote Python execution.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configuration-management.html?from=jetbrains.org#configuration-type">Configuration Type</a>
 */
class RemotePythonConfigurationType : ConfigurationType {
    /**
     * The ID of the configuration type. Should be camel-cased without dashes, underscores, spaces and quotation marks.
     * The ID is used to store run configuration settings in a project or workspace file and
     * must not change between plugin versions.
     */
    override fun getId() = "RemotePythonConfigurationType"

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
 * Python execution target types.
 */
enum class TargetType { MODULE, SCRIPT }  // TODO: internal



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
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
abstract class RemotePythonEditor<Options : RemotePythonOptions, Config : RemotePythonRunConfiguration<Options>> protected constructor() :
    SettingsEditor<Config>() {

    protected companion object {
        val targetTypeOptions = mapOf(
            TargetType.SCRIPT to "Script path:",
            TargetType.MODULE to "Module name:",
        )
    }

    protected var targetType = TargetType.MODULE
    protected var targetName = ""
    protected var targetParams = ""
    protected var pythonExe = ""
    protected var pythonOpts = ""
    protected var remoteWorkDir = ""
    protected var localWorkDir = ""

    /**
     * Update UI component with options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: Config) {
        config.let {
            targetType = it.targetType
            targetName = it.targetName
            targetParams = it.targetParams
            pythonExe = it.pythonExe
            pythonOpts = it.pythonOpts
            remoteWorkDir = it.remoteWorkDir
            localWorkDir = it.localWorkDir
        }
    }

    /**
     * Update configuration with options from UI.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: Config) {
        (this.component as DialogPanel).apply()
        config.let {
            it.targetType = targetType
            it.targetName = targetName
            it.targetParams = targetParams
            it.pythonExe = pythonExe
            it.pythonOpts = pythonOpts
            it.remoteWorkDir = remoteWorkDir
            it.localWorkDir = localWorkDir
        }
    }
}
