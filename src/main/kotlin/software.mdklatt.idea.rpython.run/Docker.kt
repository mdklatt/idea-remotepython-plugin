/**
 * Run Python in a Docker container.
 */
package software.mdklatt.idea.rpython.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
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
class DockerConfigurationFactory(type: RemotePythonConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance
     */
    override fun createTemplateConfiguration(project: Project) = DockerRunConfiguration(project, this, "")

    /**
     * Returns the id of the run configuration that is used for serialization. For compatibility reason the default implementation calls
     * the method {@link #getName()} and this may cause problems if {@link #getName} returns localized value. So the default implementation
     * <strong>must be overridden</strong> in all inheritors. In existing implementations you need to use the same value which is returned
     * by {@link #getName()} for compatibility but store it directly in the code instead of taking from a message bundle. For new configurations
     * you may use any unique ID; if a new {@link ConfigurationType} has a single {@link ConfigurationFactory}, use {@link SimpleConfigurationType} instead.
     */
    override fun getId() = "RemotePythonDockerConfiguration"

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @return: name
     */
    override fun getName() = "Docker Host"

    override fun getOptionsClass() = DockerOptions::class.java
}

/**
 * Docker host type.
 */
enum class DockerHostType { IMAGE, CONTAINER, SERVICE }


/**
 * Handle persistence of run configuration options.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
class DockerOptions : RemotePythonOptions() {
    internal var hostType by string()
    internal var hostName by string()
    internal var dockerExe by string()
    internal var dockerCompose by string()
    internal var dockerOpts by string()
}


/**
 * Run Configuration for executing Python in a <a href="https://www.docker.com">Docker</a> container.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class DockerRunConfiguration(project: Project, factory: DockerConfigurationFactory, name: String) :
    RemotePythonRunConfiguration<DockerOptions>(project, factory, name) {

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) = DockerState(this, environment)

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = DockerEditor(project)

    var hostType: DockerHostType
        get() = DockerHostType.valueOf(options.hostType ?: "IMAGE")
        set(value) {
            options.hostType = value.name
        }
    var hostName: String
        get() = options.hostName ?: ""
        set(value) {
            options.hostName = value
        }
    var dockerExe: String
        get() = options.dockerExe ?: "docker"
        set(value) {
            options.dockerExe = value.ifBlank { "docker" }
        }
    var dockerOpts: String
        get() = options.dockerOpts ?: ""
        set(value) {
            options.dockerOpts = value
        }
    var dockerCompose: String
        get() = options.dockerCompose ?: ""
        set(value) {
            options.dockerCompose = value
        }
}


/**
 * Command line process for executing the run configuration.
 *
 * @param config: run configuration
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
class DockerState internal constructor(private val config: DockerRunConfiguration, environment: ExecutionEnvironment) :
        CommandLineState(environment) {

    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @see GeneralCommandLine
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        val runOptions = mapOf(
            "workdir" to config.remoteWorkDir.ifBlank { null },  // null to omit
            "rm" to true,
            "entrypoint" to ""
        )
        val command = PosixCommandLine(config.dockerExe)
        when (config.hostType) {
            DockerHostType.CONTAINER -> {
                command.addParameter("exec")
            }
            DockerHostType.IMAGE -> {
                command.addParameter("run")
                command.addOptions(runOptions)
            }
            DockerHostType.SERVICE -> {
                command.addParameter("compose")
                command.addOptions(mapOf(
                    "file" to config.dockerCompose.ifBlank { null }  // null to omit
                ))
                command.addParameter("run")
                command.addOptions(runOptions)
            }
        }
        command.addParameters(config.hostName, *python())
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
    private fun python(): Array<String> {
        val command = PosixCommandLine().apply {
            withExePath(config.pythonExe)
            if (config.targetType == TargetType.MODULE) {
                addParameter("-m")
            }
            addParameter(config.targetName)
            addParameters(PosixCommandLine.split(config.targetParams))
        }
        return PosixCommandLine.split(command.commandLineString).toTypedArray()
    }
}


/**
 * UI component for setting run configuration options.
 *
 * @param project: the project in which the run configuration will be used
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
class DockerEditor internal constructor(project: Project) :
    RemotePythonEditor<DockerOptions, DockerRunConfiguration>(project) {

    companion object {
        val hostTypeOptions = mapOf(
            DockerHostType.CONTAINER to "Container name:",
            DockerHostType.IMAGE to "Image label:",
            DockerHostType.SERVICE to "Compose service:",
        )
    }

    private var hostType = ComboBox(hostTypeOptions.values.toTypedArray())
    private var hostName = JTextField()
    private var dockerExe = TextFieldWithBrowseButton().also {
        it.addBrowseFolderListener("Docker Executable", "", project, fileChooser)
    }
    private var dockerCompose = TextFieldWithBrowseButton().also {
        // TODO: This should only be editable if hostType == SERVICE.
        it.addBrowseFolderListener("Docker Compose File", "", project, fileChooser)
    }
    private var dockerOpts = RawCommandLineEditor()

    /**
     * Update UI component with options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: DockerRunConfiguration) {
        super.resetEditorFrom(config)
        config.let {
            hostType.selectedItem = hostTypeOptions[it.hostType]
            hostName.text = it.hostName
            dockerExe.text = it.dockerExe
            dockerCompose.text = it.dockerCompose
            dockerOpts.text = it.dockerOpts
        }
    }

    /**
     * Update configuration with options from UI.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: DockerRunConfiguration) {
        super.applyEditorTo(config)
        config.let {
            it.hostType = hostTypeOptions.getKey(hostType.selectedItem)
            it.hostName = hostName.text
            it.dockerExe = dockerExe.text
            it.dockerCompose = dockerCompose.text
            it.dockerOpts = dockerOpts.text
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
            row {
                hostType()
                hostName()
            }
            row("Python interpreter:") { pythonExe() }
            row("Python options:") { pythonOpts() }
            row("Remote working directory:") { remoteWorkDir() }
            titledRow("Local Environment") {}
            row("Docker command:") { dockerExe() }
            row("Docker compose file:") { dockerCompose() }
            row("Docker options:") { dockerOpts() }
            row("Local working directory:") { localWorkDir() }
        }
    }
}
