package software.mdklatt.idea.rpython.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
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
import javax.swing.JComponent
import javax.swing.JTextField


/**
 * Generate VagrantRunConfigurations.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class VagrantConfigurationFactory(type: RPythonConfigurationType) : RPythonConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
            VagrantRunConfiguration(project, this, "")

    /**
     * Returns the id of the run configuration that is used for serialization. For compatibility reason the default implementation calls
     * the method {@link #getName()} and this may cause problems if {@link #getName} returns localized value. So the default implementation
     * <strong>must be overridden</strong> in all inheritors. In existing implementations you need to use the same value which is returned
     * by {@link #getName()} for compatibility but store it directly in the code instead of taking from a message bundle. For new configurations
     * you may use any unique ID; if a new {@link ConfigurationType} has a single {@link ConfigurationFactory}, use {@link SimpleConfigurationType} instead.
     */
    override fun getId() = name  // for backwards compatibility with existing configs

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @return: name
     */
    override fun getName() = "Vagrant Host"

    /**
     * Create a new settings object for the run configuration.
     */
    override fun createSettings() = VagrantSettings()
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-galaxy.html">ansible-galaxy</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class VagrantRunConfiguration internal constructor(project: Project, factory: VagrantConfigurationFactory, name: String) :
    RPythonRunConfiguration(project, factory,  name) {

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = VagrantSettingsEditor(project)

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            VagrantCommandLineState(this, environment)
}


/**
 * Command line process for executing the run configuration.
 *
 * @param config: run configuration
 * @param environ: execution environment
 */
class VagrantCommandLineState internal constructor(private val config: VagrantRunConfiguration, environ: ExecutionEnvironment) :
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
        val settings = config.settings as VagrantSettings
        val command = PosixCommandLine(settings.vagrantExe, listOf("ssh"))
        val options = mutableMapOf<String, Any?>(
            "command" to python(settings)
        )
        command.addOptions(options)
        command.addParameter(settings.vagrantHost)
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
    private fun python(settings: VagrantSettings): String {
        val command = PosixCommandLine().apply {
            if (settings.remoteWorkDir.isNotBlank()) {
                withExePath("cd")
                addParameters(settings.remoteWorkDir, "&&", settings.pythonExe)
            }
            else {
                withExePath(settings.pythonExe)
            }
            if (settings.targetType == TargetType.MODULE) {
                addParameter("-m")
            }
            addParameter(settings.targetName)
            addParameters(PosixCommandLine.split(settings.targetParams))
        }
        return command.commandLineString
    }
}


/**
 * UI component for Vagrant Run Configuration settings.
 *
 * @param project: parent project
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class VagrantSettingsEditor internal constructor(project: Project) :
    RPythonSettingsEditor<VagrantRunConfiguration>(project) {

    var vagrantExe = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Vagrant Executable", "", project,
                FileChooserDescriptorFactory.createSingleFileDescriptor())
    }
    var vagrantHost = JTextField()

    /**
     *
     */
    override fun addHostFields(layout: LayoutBuilder) {
        layout.row {
            row("Vagrant host:") { vagrantHost() }
        }
    }

    /**
     * Reset host fields from a configuration state.
     *
     * @param config: input configuration
     */
    override fun resetHostFields(config: VagrantRunConfiguration) {
        (config.settings as VagrantSettings).let {
            vagrantHost.text = it.vagrantHost
        }
    }

    /**
     * Apply host fields from a configuration state.
     *
     * @param config: output configuration
     */
    override fun applyHostFields(config: VagrantRunConfiguration) {
        (config.settings as VagrantSettings).let {
            it.vagrantHost = vagrantHost.text
        }
    }

    /**
     *
     */
    override fun addExecutorFields(layout: LayoutBuilder) {
        layout.apply {
            row("Vagrant executable:") { vagrantExe() }
        }
    }

    /**
     * Reset executor fields from a configuration state.
     *
     * @param config: input configuration
     */
    override fun resetExecutorFields(config: VagrantRunConfiguration) {
        (config.settings as VagrantSettings).let {
            vagrantHost.text = it.vagrantHost
        }
    }

    /**
     * Apply executor fields from a configuration state.
     *
     * @param config: output configuration
     */
    override fun applyExecutorFields(config: VagrantRunConfiguration) {
        (config.settings as VagrantSettings).let {
            it.vagrantExe = vagrantExe.text
        }
    }
}


/**
 * Manage VagrantRunConfiguration runtime settings.
 */
class VagrantSettings() : RPythonSettings() {

    override val xmlTagName = "rpython-vagrant"

    var vagrantExe = ""
        get() = field.ifBlank { "vagrant" }
    var vagrantHost = ""

    /**
     * Load stored settings.
     *
     * @param element: settings root element
     */
    override fun load(element: Element) {
        super.load(element)
        element.getOrCreate(xmlTagName).let {
            vagrantExe = JDOMExternalizerUtil.readField(it, "vagrantExe", "")
            vagrantHost = JDOMExternalizerUtil.readField(it, "vagrantHost", "")
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
            JDOMExternalizerUtil.writeField(it, "vagrantExe", vagrantExe)
            JDOMExternalizerUtil.writeField(it, "vagrantHost", vagrantHost)
        }
    }
}
