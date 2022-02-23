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
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.util.getOrCreate
import org.jdom.Element
import javax.swing.JTextField


/**
 * Create a DockerRunConfiguration instance.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class DockerConfigurationFactory(type: RPythonConfigurationType) : RPythonConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
            DockerRunConfiguration(project, this, "")

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @return: name
     */
    override fun getName() = "Docker Host"

    /**
     * Create a new settings object for the run configuration.
     */
    override fun createSettings() = DockerSettings()
}


/**
 * Run Configuration for executing Python in a <a href="https://docs.docker.com/">Docker</a> container.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class DockerRunConfiguration internal constructor(project: Project, factory: DockerConfigurationFactory, name: String) :
        RPythonRunConfiguration(project, factory,  name) {
    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = DockerSettingsEditor(project)

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            DockerCommandLineState(this, environment)
}


/**
 * Docker host type.
 */
enum class DockerHostType { IMAGE, CONTAINER, SERVICE }


/**
 * Command line process for executing the run configuration.
 *
 * @param config: run configuration
 * @param environ: execution environment
 */
class DockerCommandLineState internal constructor(private val config: DockerRunConfiguration, environ: ExecutionEnvironment) :
        CommandLineState(environ) {
    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     *
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        val settings = config.settings as DockerSettings
        val runOptions = mapOf(
            "workdir" to settings.remoteWorkDir.ifBlank { null },  // null to omit
            "rm" to true,
            "entrypoint" to ""
        )
        val command = PosixCommandLine(settings.dockerExe)
        when (settings.hostType) {
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
                    "file" to settings.dockerCompose.ifBlank { null }
                ))
                command.addParameter("run")
                command.addOptions(runOptions)
            }
        }
        command.addParameters(settings.hostName, *python(settings))
        if (settings.localWorkDir.isNotBlank()) {
            command.setWorkDirectory(settings.localWorkDir)
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
     * @param settings: runtime settings
     * @return: Python command string
     */
    private fun python(settings: DockerSettings): Array<String> {
        val command = PosixCommandLine().apply {
            withExePath(settings.pythonExe)
            if (settings.targetType == TargetType.MODULE) {
                addParameter("-m")
            }
            addParameter(settings.targetName)
            addParameters(PosixCommandLine.split(settings.targetParams))
        }
        return PosixCommandLine.split(command.commandLineString).toTypedArray()
    }
}


/**
 * UI component for Docker Run Configuration settings.
 *
 * @param project: parent project
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class DockerSettingsEditor internal constructor(project: Project) :
        RPythonSettingsEditor<DockerRunConfiguration>(project) {

    companion object {
        private val hostTypes = listOf(
            Pair(DockerHostType.IMAGE, "Image name:"),
            Pair(DockerHostType.CONTAINER, "Container name:"),
            Pair(DockerHostType.SERVICE, "Service name:"),
        )
    }

    private var hostName = JTextField()
    private var hostType = ComboBox(hostTypes.map{ it.second }.toTypedArray())
    private var dockerExe = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Docker Executable", "", project, fileChooser)
    }
    private var dockerOpts = RawCommandLineEditor()
    private var dockerCompose = TextFieldWithBrowseButton().apply {
        isEditable = false
        addBrowseFolderListener("Docker Compose File", "", project, fileChooser)
    }

    /**
     *
     */
    override fun addHostFields(layout: LayoutBuilder) {
        layout.row {
            hostType()
            hostName()
        }
    }

    /**
     * Reset host fields from a configuration state.
     *
     * @param config: input configuration
     */
    override fun resetHostFields(config: DockerRunConfiguration) {
        (config.settings as DockerSettings).let {
            hostName.text = it.hostName
            hostType.selectedIndex = hostTypes.map{ it.first }.indexOf(it.hostType)
        }
    }

    /**
     * Apply host fields from a configuration state.
     *
     * @param config: output configuration
     */
    override fun applyHostFields(config: DockerRunConfiguration) {
        (config.settings as DockerSettings).let {
            it.dockerExe = dockerExe.text
            it.dockerCompose = dockerCompose.text
            it.dockerOpts = dockerOpts.text
            it.hostName = hostName.text
            it.hostType = hostTypes[hostType.selectedIndex].first
        }
    }

    /**
     *
     */
    override fun addExecutorFields(layout: LayoutBuilder) {
        layout.apply {
            row("Docker command:") { dockerExe() }
            row("Docker compose file:") { dockerCompose() }
            row("Docker options:") { dockerOpts() }
        }
    }

    /**
     * Reset executor fields from a configuration state.
     *
     * @param config: input configuration
     */
    override fun resetExecutorFields(config: DockerRunConfiguration) {
        (config.settings as DockerSettings).let {
            dockerExe.text = it.dockerExe
            dockerCompose.text = it.dockerCompose
            dockerOpts.text = it.dockerOpts
        }
    }

    /**
     * Apply executor fields from a configuration state.
     *
     * @param config: output configuration
     */
    override fun applyExecutorFields(config: DockerRunConfiguration) {
        (config.settings as DockerSettings).let {
            it.dockerExe = dockerExe.text
            it.dockerCompose = dockerCompose.text
            it.dockerOpts = dockerOpts.text
        }
    }
}


/**
 * Manage DockerRunConfiguration runtime settings.
 */
class DockerSettings : RPythonSettings() {

    override val xmlTagName = "rpython-docker"

    var dockerExe = ""
        get() = field.ifBlank { "docker" }
    var dockerOpts = ""
    var dockerCompose = ""
    var hostName = ""
    var hostType = DockerHostType.IMAGE

    /**
     * Load stored settings.
     *
     * @param element: settings root element
     */
    override fun load(element: Element) {
        super.load(element)
        element.getOrCreate(xmlTagName).let {
            dockerExe = JDOMExternalizerUtil.readField(it, "dockerExe", "")
            dockerOpts = JDOMExternalizerUtil.readField(it, "dockerOpts", "")
            dockerCompose = JDOMExternalizerUtil.readField(it, "dockerCompose", "")
            hostName = JDOMExternalizerUtil.readField(it, "hostName", "")
            hostType = DockerHostType.valueOf(JDOMExternalizerUtil.readField(it, "hostType", "IMAGE"))
        }
    }

    /**
     * Save settings.
     *
     * @param element: settings root element
     */
    override fun save(element: Element) {
        super.save(element)
        element.getOrCreate(xmlTagName).let {
            JDOMExternalizerUtil.writeField(it, "dockerExe", dockerExe)
            JDOMExternalizerUtil.writeField(it, "dockerOpts", dockerOpts)
            JDOMExternalizerUtil.writeField(it, "dockerCompose", dockerCompose)
            JDOMExternalizerUtil.writeField(it, "hostName", hostName)
            JDOMExternalizerUtil.writeField(it, "hostType", hostType.name)
        }
    }
}
