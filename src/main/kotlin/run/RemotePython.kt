/**
 * IDEA run configurations for Remote Python execution.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import dev.mdklatt.idea.common.exec.CommandLine
import dev.mdklatt.idea.common.exec.PosixCommandLine
import dev.mdklatt.idea.common.map.findFirstKey
import org.jdom.Element
import java.lang.RuntimeException
import java.util.UUID


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
    internal var uid by string()
    internal var targetType by string()
    internal var targetName by string()
    internal var targetArgs by string()
    internal var pythonExe by string("python3")
    internal var pythonVenv by string()
    internal var pythonOpts by string()
    internal var pythonWorkDir by string()
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

    protected val logger = Logger.getInstance(this::class.java)

    /**
     * Get the persistent options for this instance.
     */
    final override fun getOptions(): Options {
        // Kotlin considers this an unsafe cast because generics do not have
        // runtime type information unless they are reified, which is not
        // supported for class parameters.
        @Suppress("UNCHECKED_CAST")
        return super.getOptions() as Options
    }

    /**
     * Read settings from XML.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        if (options.uid == null) {
            options.uid = UUID.randomUUID().toString()
        }
    }

    /**
     * Write settings to XML.
     */
    override fun writeExternal(element: Element) {
        val default = element.getAttributeValue("default")?.toBoolean() ?: false
        if (default) {
            // Do not save UID with configuration template.
            options.uid = null
        }
        super.writeExternal(element)
    }

    // TODO: Why can't options.<property> be used as a delegate?

    internal var uid: String
        get() {
            if (options.uid == null) {
                options.uid = UUID.randomUUID().toString()
            }
            return options.uid ?: throw RuntimeException("null UID")
        }
        set(value) {
            options.uid = value
        }

    internal var targetType: TargetType
        get() = TargetType.valueOf(options.targetType ?: "MODULE")
        set(value) {
            options.targetType = value.name
        }
    internal var targetName: String
        get() = options.targetName ?: ""
        set(value) {
            options.targetName = value
        }
    internal var targetArgs: String
        get() = options.targetArgs ?: ""
        set(value) {
            options.targetArgs = value
        }
    internal var pythonExe: String
        get() = options.pythonExe ?: ""
        set(value) {
            options.pythonExe = value.ifBlank { "python3" }
        }
    internal var pythonVenv: String
        get() = options.pythonVenv ?: ""
        set(value) {
            options.pythonVenv = value
        }
    internal var pythonOpts: String
        get() = options.pythonOpts ?: ""
        set(value) {
            options.pythonOpts = value
        }
    internal var pythonWorkDir: String
        get() = options.pythonWorkDir ?: ""
        set(value) {
            options.pythonWorkDir = value
        }
}


/**
 * Base class for run configuration UI
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
abstract class RemotePythonEditor<Options : RemotePythonOptions, Config : RemotePythonRunConfiguration<Options>> protected constructor() :
    SettingsEditor<Config>() {

    private companion object {
        val targetTypeOptions = mapOf(
            TargetType.SCRIPT to "Script path:",
            TargetType.MODULE to "Module name:",
        )
    }

    private var targetType = TargetType.MODULE
    private var targetName = ""
    private var targetArgs = ""
    private var pythonExe = ""
    private var pythonVenv = ""
    private var pythonOpts = ""
    private var pythonWorkDir = ""

    /**
     * Create the UI component.
     *
     * @return Swing component
     */
    final override fun createEditor(): DialogPanel {
        return panel {
            group("Remote Python") {
                addPythonFields(this)
            }
            group("Local Execution") {
                addExecutorFields(this)
            }
        }
    }

    /**
     * Add Python settings to the UI component.
     *
     * @param parent: parent component builder
     */
    private fun addPythonFields(parent: Panel) {
        parent.let {
            it.row {
                comboBox(targetTypeOptions.values).bindItem(
                    getter = { targetTypeOptions[targetType] },
                    setter = { targetType = targetTypeOptions.findFirstKey(it)!! }
                )
                textField().bindText(::targetName)
            }
            it.row("Parameters:") {
                expandableTextField().bindText(::targetArgs)
            }
            it.row("Python interpreter:") {
                textField().bindText(::pythonExe)
            }
            it.row("Python virtualenv:") {
                textFieldWithBrowseButton("Python Virtual Environment",
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                ).bindText(::pythonVenv)
            }
            it.row("Python options:") {
                textField().bindText(::pythonOpts)
            }
            it.row("Working directory:") {
                textField().bindText(::pythonWorkDir)
            }
        }
    }

    /**
     * Add local executor settings to the UI component.
     *
     * @param parent: parent component builder
     */
    protected abstract fun addExecutorFields(parent: Panel)


    /**
     * Update UI component with options from configuration.
     *
     * @param config: run configuration
     */
    final override fun resetEditorFrom(config: Config) {
        // Update bound properties from config value then reset UI.
        resetPythonOptions(config)
        resetExecutorOptions(config)
        (this.component as DialogPanel).reset()
    }

    /**
     * Reset UI component with remote Python options from configuration.
     *
     * @param config: run configuration
     */
    private fun resetPythonOptions(config: Config) {
        config.let {
            targetType = it.targetType
            targetName = it.targetName
            targetArgs = it.targetArgs
            pythonExe = it.pythonExe
            pythonVenv = it.pythonVenv
            pythonOpts = it.pythonOpts
            pythonWorkDir = it.pythonWorkDir
        }
    }

    /**
     * Reset UI with local executor options from configuration.
     *
     * @param config: run configuration
     */
    protected abstract fun resetExecutorOptions(config: Config)

    /**
     * Update configuration with options from UI.
     *
     * @param config: run configuration
     */
    final override fun applyEditorTo(config: Config) {
        // Apply UI to bound properties then update config values.
        (this.component as DialogPanel).apply()
        applyPythonOptions(config)
        applyExecutorOptions(config)
    }

    /**
     * Apply UI local executor options to configuration.
     *
     * @param config: run configuration
     */
    private fun applyPythonOptions(config: Config) {
        config.let {
            it.targetType = targetType
            it.targetName = targetName
            it.targetArgs = targetArgs
            it.pythonExe = pythonExe
            it.pythonVenv = pythonVenv
            it.pythonOpts = pythonOpts
            it.pythonWorkDir = pythonWorkDir
        }
    }

    /**
     * Apply UI local executor options to configuration.
     *
     * @param config: run configuration
     */
    protected abstract fun applyExecutorOptions(config: Config)
}


/**
 * Base class for run configuration command line processes.
 *
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
abstract class RemotePythonState internal constructor(environment: ExecutionEnvironment) :
    CommandLineState(environment) {

        /**
     * Start the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     *
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        val command = getCommand().also {
            if (!it.environment.contains("TERM")) {
                it.environment["TERM"] = "xterm-256color"
            }
        }
        return KillableColoredProcessHandler(command).also {
            ProcessTerminatedListener.attach(it, environment.project)
        }
    }

    /**
     * Get command to execute.
     *
     * @return command
     */
    internal abstract fun getCommand(): PosixCommandLine

    companion object {
        /**
         * Construct Python command for a POSIX shell.
         *
         * @param config: run configuration settings
         * @param exportEnv: true to export command environment as shell variables
         * @param setWorkDir: true to set working directory via the shell
         * @return: Python command line
         */
        internal fun <Options: RemotePythonOptions, Config: RemotePythonRunConfiguration<Options>>
                posixShellPython(config: Config, setWorkDir: Boolean = true): PosixCommandLine {
            val commands = mutableListOf<PosixCommandLine>()
            if (setWorkDir && config.pythonWorkDir.isNotBlank()) {
                commands.add(PosixCommandLine("cd", config.pythonWorkDir))
            }
            val params = if (config.pythonOpts.isNotBlank()) config.pythonOpts.split("\\s+".toRegex()) else emptyList()
            val pythonCommand = PosixCommandLine(config.pythonExe, *params.toTypedArray()).also {
                if (config.targetType == TargetType.MODULE) {
                    it.addParameter("-m")
                }
                it.addParameter(config.targetName)
                it.addParameters(CommandLine.splitArguments(config.targetArgs))
                it.withPythonVenv(config.pythonVenv)
            }
            pythonCommand.environment.entries.forEach {
                // Environment variables have to be set in the remote shell via
                // export.
                commands.add(PosixCommandLine("export", "${it.key}=${it.value}"))
            }
            commands.add(pythonCommand)
            return if (commands.size > 1) PosixCommandLine.andCommands(commands.asSequence()) else pythonCommand
        }
    }
}
