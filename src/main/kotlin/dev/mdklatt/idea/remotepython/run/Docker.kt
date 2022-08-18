/**
 * Run Python in a Docker container.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText


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
    override fun getName() = "Docker Container"

    /**
     * Return the type of the options storage class.
     *
     * @return: options class type
     */
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
    internal var localWorkDir by string()
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
    override fun getConfigurationEditor() = DockerEditor()

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
            "workdir" to config.pythonWorkDir.ifBlank { null },  // null to omit
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
            addParameters(PosixCommandLine.split(config.targetArgs))
        }
        return PosixCommandLine.split(command.commandLineString).toTypedArray()
    }
}


/**
 * UI component for setting run configuration options.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
class DockerEditor internal constructor() :
    RemotePythonEditor<DockerOptions, DockerRunConfiguration>() {

    private companion object {
        val hostTypeOptions = mapOf(
            DockerHostType.CONTAINER to "Container name:",
            DockerHostType.IMAGE to "Image label:",
            DockerHostType.SERVICE to "Compose service:",
        )
    }

    private var hostType = DockerHostType.IMAGE
    private var hostName = ""
    private var dockerExe = ""
    private var dockerCompose = ""
    private var dockerOpts = ""
    private var localWorkDir = ""

    /**
     * Reset UI component with local executor options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetExecutorOptions(config: DockerRunConfiguration) {
        config.let {
            hostType = it.hostType
            hostName = it.hostName
            dockerExe = it.dockerExe
            dockerCompose = it.dockerCompose
            dockerOpts = it.dockerOpts
            localWorkDir = it.localWorkDir
        }
    }

    /**
     * Apply UI local executor options to configuration.
     *
     * @param config: run configuration
     */
    override fun applyExecutorOptions(config: DockerRunConfiguration) {
        config.let {
            it.hostType = hostType
            it.hostName = hostName
            it.dockerExe = dockerExe
            it.dockerCompose = dockerCompose
            it.dockerOpts = dockerOpts
            it.localWorkDir = localWorkDir
        }
    }

    /**
     * Add local executor settings to the UI component.
     *
     * @param parent: parent component builder
     */
    override fun addExecutorFields(parent: Panel) {
        parent.run {
            row {
                comboBox(hostTypeOptions.values).bindItem(
                    { hostTypeOptions[hostType] },
                    { hostType = hostTypeOptions.getKey(it) },
                )
                textField().bindText(::hostName)
            }
            row("Docker executable:") {
                textFieldWithBrowseButton("Docker Executable").bindText(::dockerExe)
            }
            row("Docker compose file:") {
                textFieldWithBrowseButton("Docker Compose File").bindText(::dockerCompose)
            }
            row("Docker options:") {
                expandableTextField().bindText(::dockerOpts)
            }
            row("Local working directory") {
                textFieldWithBrowseButton(
                    browseDialogTitle = "Local Working Directory",
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                ).bindText(::localWorkDir)
            }
        }
    }
}
