package software.mdklatt.idea.rpython.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.util.getOrCreate
import javax.swing.JTextField
import org.jdom.Element


/**
 * Generate SecureShellRunConfigurations.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class SecureShellConfigurationFactory(type: RPythonConfigurationType) : RPythonConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
            SecureShellRunConfiguration(project, this, "")

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
    override fun getName() = "SSH Host"

    /**
     * Create a new settings object for the run configuration.
     */
    override fun createSettings() = SecureShellSettings()
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-galaxy.html">ansible-galaxy</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class SecureShellRunConfiguration internal constructor(project: Project, factory: SecureShellConfigurationFactory, name: String) :
        RPythonRunConfiguration(project, factory,  name) {
    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = SecureShellSettingsEditor(project)

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            SecureShellCommandLineState(this, environment)
}


/**
 * Command line process for executing the run configuration.
 *
 * @param config: run configuration
 * @param environ: execution environment
 */
class SecureShellCommandLineState internal constructor(private val config: SecureShellRunConfiguration, environ: ExecutionEnvironment) :
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
        val settings = config.settings as SecureShellSettings
        val command = PosixCommandLine(settings.sshExe)
        if (settings.sshOpts.isNotBlank()) {
            command.addParameters(PosixCommandLine.split(settings.sshOpts))
        }
        var host = settings.sshHost
        if (settings.sshUser.isNotBlank()) {
            host = settings.sshUser + "@" + host
        }
        command.addParameters(host, python(settings))
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
     * @return: Python command string
     */
    private fun python(settings: SecureShellSettings): String {
        // TODO: Identical to VagrantCommandLineState except for parameter type.
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
 * UI component for SSH Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class SecureShellSettingsEditor internal constructor(project: Project) :
        RPythonSettingsEditor<SecureShellRunConfiguration>(project) {

    var sshExe = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("SSH Command", "", project,
                FileChooserDescriptorFactory.createSingleFileDescriptor())
    }
    var sshHost = JTextField()
    var sshOpts = RawCommandLineEditor()
    var sshUser = JTextField()

    /**
     *
     */
    override fun addHostFields(layout: LayoutBuilder) {
        layout.row {
            row("Remote host:") { sshHost() }
            row("Remote user:") { sshUser() }
        }
    }

    /**
     * Reset host fields from a configuration state.
     *
     * @param config: input configuration
     */
    override fun resetHostFields(config: SecureShellRunConfiguration) {
        (config.settings as SecureShellSettings).let {
            sshHost.text = it.sshHost
            sshUser.text = it.sshUser
        }
    }

    /**
     * Apply host fields from a configuration state.
     *
     * @param config: output configuration
     */
    override fun applyHostFields(config: SecureShellRunConfiguration) {
        (config.settings as SecureShellSettings).let {
            it.sshHost = sshHost.text
            it.sshUser = sshUser.text
        }
    }

    /**
     *
     */
    override fun addExecutorFields(layout: LayoutBuilder) {
        layout.apply {
            row("SSH executable:") { sshExe() }
            row("SSH options:") { sshOpts() }
        }
    }

    /**
     * Reset executor fields from a configuration state.
     *
     * @param config: input configuration
     */
    override fun resetExecutorFields(config: SecureShellRunConfiguration) {
        (config.settings as SecureShellSettings).let {
            sshExe.text = it.sshExe
            sshOpts.text = it.sshOpts
        }
    }

    /**
     * Apply executor fields from a configuration state.
     *
     * @param config: output configuration
     */
    override fun applyExecutorFields(config: SecureShellRunConfiguration) {
        (config.settings as SecureShellSettings).let {
            it.sshExe = sshExe.text
            it.sshOpts = sshOpts.text
        }
    }
}


/**
 * Manage SecureShellRunConfiguration runtime settings.
 */
class SecureShellSettings : RPythonSettings() {

    override val xmlTagName = "rpython-ssh"

    var sshExe = ""
        get() = field.ifBlank { "ssh" }
    var sshHost = ""
    var sshUser = ""
    var sshOpts = ""

    /**
     * Load stored settings.
     *
     * @param element: settings root element
     */
    override fun load(element: Element) {
        super.load(element)
        element.getOrCreate(xmlTagName).let {
            sshExe = JDOMExternalizerUtil.readField(it, "sshExe", "")
            sshOpts = JDOMExternalizerUtil.readField(it, "sshOpts", "")
            sshHost = JDOMExternalizerUtil.readField(it, "sshHost", "")
            sshUser = JDOMExternalizerUtil.readField(it, "sshUser", "")
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
            JDOMExternalizerUtil.writeField(it, "sshExe", sshExe)
            JDOMExternalizerUtil.writeField(it, "sshHost", sshHost)
            JDOMExternalizerUtil.writeField(it, "sshUser", sshUser)
            JDOMExternalizerUtil.writeField(it, "sshOpts", sshOpts)
        }
    }
}
