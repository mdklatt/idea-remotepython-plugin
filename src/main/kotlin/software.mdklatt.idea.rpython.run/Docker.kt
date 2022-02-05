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
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.layout.panel
import com.intellij.util.getOrCreate
import org.jdom.Element
import javax.swing.JComponent
import javax.swing.JTextField


/**
 * Create a DockerRunConfiguration instance.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class DockerConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
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
     * Run configuration ID used for serialization.
     *
     * @return: unique ID
     */
    override fun getId(): String = this::class.java.simpleName
}


/**
 * Run Configuration for executing Python in a <a href="https://docs.docker.com/">Docker</a> container.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class DockerRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
        RunConfigurationBase<RunProfileState>(project, factory, name) {

    internal var settings = DockerSettings()

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
 * Docker host type.
 */
internal enum class DockerHostType { IMAGE, CONTAINER, SERVICE }


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
        val settings = config.settings
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
            if (settings.targetType == PythonTargetType.MODULE) {
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
        SettingsEditor<DockerRunConfiguration>() {

    companion object {
        private val targetTypes = listOf(
            Pair(PythonTargetType.SCRIPT, "Script path:"),
            Pair(PythonTargetType.MODULE, "Module name:"),
        )
        private val dockerHostTypes = listOf(
            Pair(DockerHostType.IMAGE, "Image name:"),
            Pair(DockerHostType.CONTAINER, "Container name:"),
            Pair(DockerHostType.SERVICE, "Service name:"),
        )
    }

    var target = JTextField()
    var targetType = ComboBox(targetTypes.map{ it.second }.toTypedArray())
    var targetParams = RawCommandLineEditor()
    var python = JTextField()
    var pythonOpts = RawCommandLineEditor()
    var remoteWorkDir = JTextField()
    var dockerHost = JTextField()
    var dockerHostType = ComboBox(dockerHostTypes.map{ it.second }.toTypedArray())
    var docker = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Docker Command", "", project,
                FileChooserDescriptorFactory.createSingleFileDescriptor())
    }
    var dockerCompose = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Docker Compose File", "", project,
            FileChooserDescriptorFactory.createSingleFileDescriptor())
    }
    var dockerOpts = RawCommandLineEditor()
    var localWorkDir = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Docker Working Directory", "", project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor())
    }

    /**
     * Create the widget for this editor.
     *
     * @return UI widget
     */
    override fun createEditor(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        dockerHostType.addItemListener{ event ->
            val index = dockerHostTypes.map { it.first }.indexOf(DockerHostType.SERVICE)
            //val index = (event.source as ComboBox<*>).selectedIndex
            if ((event.source as ComboBox<*>).selectedIndex == index) {
                dockerCompose.isEditable = true
            } else {
                dockerCompose.text = ""
                dockerCompose.isEditable = false
            }
        }
        return panel {
            row() {
                targetType()
                target()
            }
            row("Parameters:") { targetParams() }
            titledRow("Remote Environment") {}
            row() {
                dockerHostType()
                dockerHost()
            }
            row("Python interpreter:") { python() }
            row("Python options:") { pythonOpts() }
            row("Remote working directory:") { remoteWorkDir() }
            titledRow("Local Environment") {}
            row("Docker command:") { docker() }
            row("Docker compose file:") { dockerCompose() }
            row("Docker options:") { dockerOpts() }
            row("Local working directory:") { localWorkDir() }
        }
    }

    /**
     * Reset editor fields from the configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: DockerRunConfiguration) {
        config.apply {
            target.text = settings.targetName
            targetType.selectedIndex = targetTypes.map{ it.first }.indexOf(settings.targetType)
            targetParams.text = settings.targetParams
            python.text = settings.pythonExe
            pythonOpts.text = settings.pythonOpts
            remoteWorkDir.text = settings.remoteWorkDir
            docker.text = settings.dockerExe
            dockerCompose.text = settings.dockerCompose
            dockerOpts.text = settings.dockerOpts
            dockerHost.text = settings.hostName
            dockerHostType.selectedIndex = dockerHostTypes.map{ it.first }.indexOf(settings.hostType)
            localWorkDir.text = settings.localWorkDir
        }
        return
    }

    /**
     * Apply editor fields to the configuration state.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: DockerRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        config.apply {
            settings = DockerSettings()
            settings.targetName = target.text
            settings.targetType = targetTypes[targetType.selectedIndex].first
            settings.targetParams = targetParams.text
            settings.pythonExe = python.text
            settings.pythonOpts = pythonOpts.text
            settings.remoteWorkDir = remoteWorkDir.text
            settings.dockerExe = docker.text
            settings.dockerCompose = dockerCompose.text
            settings.dockerOpts = dockerOpts.text
            settings.hostName = dockerHost.text
            settings.hostType = dockerHostTypes[dockerHostType.selectedIndex].first
            settings.localWorkDir = localWorkDir.text
        }
        return
    }
}


/**
 * Manage DockerRunConfiguration runtime settings.
 */
internal class DockerSettings : RemotePythonSettings() {

    override val xmlTagName = "ansible-playbook"

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
    internal override fun load(element: Element) {
        super.load(element)
        element.getOrCreate(xmlTagName).let {
            dockerExe = JDOMExternalizerUtil.readField(it, "dockerExe", "")
            dockerOpts = JDOMExternalizerUtil.readField(it, "dockerOpts", "")
            dockerCompose = JDOMExternalizerUtil.readField(it, "dockerCompose", "")
            hostName = JDOMExternalizerUtil.readField(it, "hostName", "")
            hostType = DockerHostType.valueOf(JDOMExternalizerUtil.readField(it, "hostType", "IMAGE"))
        }
        return
    }

    /**
     * Save settings.
     *
     * @param element: settings root element
     */
    internal override fun save(element: Element) {
        super.save(element)
        element.getOrCreate(xmlTagName).let {
            JDOMExternalizerUtil.writeField(it, "dockerExe", dockerExe)
            JDOMExternalizerUtil.writeField(it, "dockerOpts", dockerOpts)
            JDOMExternalizerUtil.writeField(it, "dockerCompose", dockerCompose)
            JDOMExternalizerUtil.writeField(it, "hostName", hostName)
            JDOMExternalizerUtil.writeField(it, "hostType", hostType.name)
        }
        return
    }
}