/**
 * Run Python on a Vagrant machine.
 */
package software.mdklatt.idea.rpython.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextField


/**
 * Create a run configuration instance.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class VagrantConfigurationFactory(type: RemotePythonConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance
     */
    override fun createTemplateConfiguration(project: Project) = VagrantRunConfiguration(project, this, "")

    /**
     * Returns the id of the run configuration that is used for serialization. For compatibility reason the default implementation calls
     * the method {@link #getName()} and this may cause problems if {@link #getName} returns localized value. So the default implementation
     * <strong>must be overridden</strong> in all inheritors. In existing implementations you need to use the same value which is returned
     * by {@link #getName()} for compatibility but store it directly in the code instead of taking from a message bundle. For new configurations
     * you may use any unique ID; if a new {@link ConfigurationType} has a single {@link ConfigurationFactory}, use {@link SimpleConfigurationType} instead.
     */
    override fun getId() = "RemotePythonVagrantConfiguration"

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @return: name
     */
    override fun getName() = "Vagrant Machine"

    override fun getOptionsClass() = VagrantOptions::class.java
}

/**
 * Handle persistence of run configuration options.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
class VagrantOptions : RemotePythonOptions() {
    internal var hostName by string()
    internal var vagrantExe by string()
    internal var vagrantOpts by string()
}


/**
 * Run Configuration for executing Python on a <a href="https://www.vagrantup.com/">Vagrant</a> machine.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class VagrantRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RemotePythonRunConfiguration<VagrantOptions>(project, factory, name) {

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) = VagrantState(this, environment)

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = VagrantEditor(project)

    var hostName: String
        get() = options.hostName ?: ""
        set(value) {
            options.hostName = value
        }
    var vagrantExe: String
        get() = options.vagrantExe ?: "vagrant"
        set(value) {
            options.vagrantExe = value.ifBlank { "vagrant" }
        }
    var vagrantOpts: String
        get() = options.vagrantOpts ?: ""
        set(value) {
            options.vagrantOpts = value
        }
}


/**
 * Command line process for executing the run configuration.
 *
 * @param config: run configuration
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
class VagrantState internal constructor(private val config: VagrantRunConfiguration, environment: ExecutionEnvironment) :
    CommandLineState(environment) {
    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @see GeneralCommandLine
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        val command = PosixCommandLine(config.vagrantExe, listOf("ssh"))
        val options = mutableMapOf<String, Any?>(
            "command" to python()
        )
        command.addOptions(options)
        command.addParameter(config.hostName)
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
        val command = PosixCommandLine().apply {
            if (config.remoteWorkDir.isNotBlank()) {
                withExePath("cd")
                addParameters(config.remoteWorkDir, "&&", config.pythonExe)
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
 * @param project: the project in which the run configuration will be used
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
class VagrantEditor internal constructor(project: Project) :
    RemotePythonEditor<VagrantOptions, VagrantRunConfiguration>(project) {

    private var hostName = JTextField()
    private var vagrantExe = TextFieldWithBrowseButton().also {
        it.addBrowseFolderListener("Vagrant Executable", "", project, fileChooser)
    }
    private var vagrantOpts = RawCommandLineEditor()

    /**
     * Update UI component with options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: VagrantRunConfiguration) {
        super.resetEditorFrom(config)
        config.let {
            hostName.text = it.hostName
            vagrantExe.text = it.vagrantExe
            vagrantOpts.text = it.vagrantOpts
        }
    }

    /**
     * Update configuration with options from UI.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: VagrantRunConfiguration) {
        super.applyEditorTo(config)
        config.let {
            it.hostName = hostName.text
            it.vagrantExe = vagrantExe.text
            it.vagrantOpts = vagrantOpts.text
        }
    }

    /**
     * Create the UI component.
     *
     * @return Swing component
     */
    override fun createEditor(): JComponent {
        return panel {
            row() {
                targetType()
                targetName()
            }
            row("Parameters:") { targetParams() }
            titledRow("Remote Environment") {}
            row("Vagrant host:") { hostName() }
            row("Python interpreter:") { pythonExe() }
            row("Python options:") { pythonOpts() }
            row("Remote working directory:") { remoteWorkDir() }
            titledRow("Local Environment") {}
            row("Vagrant executable:") { vagrantExe() }
            row("Vagrant options:") { vagrantOpts() }
            row("Local working directory:") { localWorkDir() }
        }
    }
}
