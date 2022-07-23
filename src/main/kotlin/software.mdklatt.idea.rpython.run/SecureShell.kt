/**
 * Run Python on an SSH host.
 */
package software.mdklatt.idea.rpython.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText


/**
 * Create a run configuration instance.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class SecureShellConfigurationFactory(type: RemotePythonConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance
     */
    override fun createTemplateConfiguration(project: Project) = SecureShellRunConfiguration(project, this, "")

    /**
     * Returns the id of the run configuration that is used for serialization. For compatibility reason the default implementation calls
     * the method {@link #getName()} and this may cause problems if {@link #getName} returns localized value. So the default implementation
     * <strong>must be overridden</strong> in all inheritors. In existing implementations you need to use the same value which is returned
     * by {@link #getName()} for compatibility but store it directly in the code instead of taking from a message bundle. For new configurations
     * you may use any unique ID; if a new {@link ConfigurationType} has a single {@link ConfigurationFactory}, use {@link SimpleConfigurationType} instead.
     */
    override fun getId() = "RemotePythonSecureShellConfiguration"

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @return: name
     */
    override fun getName() = "SSH Host"

    /**
     * Return the type of the options storage class.
     *
     * @return: options class type
     */
    override fun getOptionsClass() = SecureShellOptions::class.java
}

/**
 * Handle persistence of run configuration options.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
class SecureShellOptions : RemotePythonOptions() {
    internal var hostName by string()
    internal var hostUser by string()
    internal var sshExe by string()
    internal var sshOpts by string()
    internal var localWorkDir by string()
}


/**
 * Run Configuration for executing Python via SSH.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class SecureShellRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RemotePythonRunConfiguration<SecureShellOptions>(project, factory, name) {

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) = SecureShellState(this, environment)

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = SecureShellEditor()

    var hostName: String
        get() = options.hostName ?: ""
        set(value) {
            options.hostName = value
        }
    var hostUser: String
        get() = options.hostUser ?: ""
        set(value) {
            options.hostUser = value
        }
    var sshExe: String
        get() = options.sshExe ?: "ssh"
        set(value) {
            options.sshExe = value.ifBlank { "ssh" }
        }
    var sshOpts: String
        get() = options.sshOpts ?: ""
        set(value) {
            options.sshOpts = value
        }
    var localWorkDir: String
        get() = options.localWorkDir ?: ""
        set(value) {
            options.localWorkDir = value
        }
}


/**
 * Command line process for executing the run configuration.
 *
 * @param config: run configuration
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
class SecureShellState internal constructor(private val config: SecureShellRunConfiguration, environment: ExecutionEnvironment) :
    CommandLineState(environment) {
    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @see GeneralCommandLine
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        val command = PosixCommandLine(config.sshExe)
        if (config.sshOpts.isNotBlank()) {
            command.addParameters(PosixCommandLine.split(config.sshOpts))
        }
        var host = config.hostName
        if (config.hostUser.isNotBlank()) {
            host = config.hostUser + "@" + host
        }
        command.addParameters(host, python())
        if (config.localWorkDir.isNotBlank()) {
            command.setWorkDirectory(config.localWorkDir)
        }
        if (!command.environment.contains("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        return KillableColoredProcessHandler(command).also {
            ProcessTerminatedListener.attach(it, environment.project)
        }
    }

    /**
     * Generate the remote Python command.
     *
     * @return: Python command string
     */
    private fun python(): String {
        // TODO: Identical to VagrantState.
        val command = PosixCommandLine().apply {
            if (config.pythonWorkDir.isNotBlank()) {
                withExePath("cd")
                addParameters(config.pythonWorkDir, "&&", config.pythonExe)
            }
            else {
                withExePath(config.pythonExe)
            }
            if (config.targetType == TargetType.MODULE) {
                addParameter("-m")
            }
            addParameter(config.targetName)
            addParameters(PosixCommandLine.split(config.targetParams))
        }
        return command.commandLineString
    }
}


/**
 * UI component for setting run configuration options.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
class SecureShellEditor internal constructor() :
    RemotePythonEditor<SecureShellOptions, SecureShellRunConfiguration>() {

    private var hostName = ""
    private var hostUser = ""
    private var sshExe = ""
    private var sshOpts = ""
    private var localWorkDir = ""

    /**
     * Add local executor settings to the UI component.
     *
     * @param parent: parent component builder
     */
    override fun addExecutorFields(parent: Panel) {
        parent.run {
            row("SSH executable:") {
                textFieldWithBrowseButton("SSH Executable").bindText(::sshExe)
            }
            row("SSH options:") {
                expandableTextField().bindText(::sshOpts)
            }
            row("Local working directory") {
                // TODO: Is this necessary for SSH?
                textFieldWithBrowseButton(
                    browseDialogTitle = "Local Working Directory",
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                ).bindText(::localWorkDir)
            }

        }
    }

    /**
     * Reset UI with local executor options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetExecutorOptions(config: SecureShellRunConfiguration) {
        config.let {
            hostName = it.hostName
            hostUser = it.hostUser
            sshExe = it.sshExe
            sshOpts = it.sshOpts
            localWorkDir = it.localWorkDir
        }
    }

    /**
     * Apply UI local executor options to configuration.
     *
     * @param config: run configuration
     */
    override fun applyExecutorOptions(config: SecureShellRunConfiguration) {
        config.let {
            it.hostName = hostName
            it.hostUser = hostUser
            it.sshExe = sshExe
            it.sshOpts = sshOpts
            it.localWorkDir = localWorkDir
        }
    }
}
