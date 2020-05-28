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
 * TODO
 */
class VagrantConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
            VagrantRunConfiguration(project, this, "")

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @return: name
     */
    override fun getName() = "Vagrant Python"

    /**
     * Run configuration ID used for serialization.
     *
     * @return: unique ID
     */
    override fun getId(): String = this::class.java.simpleName
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-galaxy.html">ansible-galaxy</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class VagrantRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
        RunConfigurationBase<RunProfileState>(project, factory, name) {

    var settings = VagrantRunSettings()

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

    /**
     * Read settings from a JDOM element.
     *
     * This is part of the RunConfiguration persistence API.
     *
     * @param element: input element.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        settings = VagrantRunSettings(element)
        return
    }

    /**
     * Write settings to a JDOM element.
     *
     * This is part of the RunConfiguration persistence API.

     * @param element: output element.
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        settings.write(element)
        return
    }
}


/**
 * TODO
 */
class VagrantCommandLineState internal constructor(private val config: VagrantRunConfiguration, environment: ExecutionEnvironment) :
        CommandLineState(environment) {
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
        val command = PosixCommandLine(settings.vagrant, listOf("ssh"))
        val options = mutableMapOf<String, Any?>(
            "command" to python(settings)
        )
        command.addOptions(options)
        command.addParameter(settings.vagrantHost)
        if (settings.vagrantWorkDir.isNotBlank()) {
            command.setWorkDirectory(settings.vagrantWorkDir)
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
    private fun python(settings: VagrantRunSettings): String {
        val command = PosixCommandLine().apply {
            if (settings.pythonWorkDir.isNotBlank()) {
                withExePath("cd")
                addParameters(listOf(settings.pythonWorkDir, "&&", settings.python))
            }
            else {
                withExePath(settings.python)
            }
            if (settings.targetType == PythonTargetType.MODULE) {
                addParameter("-m")
            }
            addParameter(settings.target)
            addParameters(PosixCommandLine.split(settings.targetParams))
        }
        return command.commandLineString
    }
}


/**
 * UI component for Vagrant Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class VagrantSettingsEditor internal constructor(project: Project) :
        SettingsEditor<VagrantRunConfiguration>() {

    companion object {
        // Ordering of targetTypeLabels and targetTypeValues must agree.
        private val targetTypeLabels = arrayOf(
            "Script path:",
            "Module name:"
        )
        private val targetTypeValues = arrayOf(
            PythonTargetType.SCRIPT,
            PythonTargetType.MODULE
        )
    }

    var target = JTextField()
    var targetType = ComboBox<String>(targetTypeLabels)
    var targetParams = RawCommandLineEditor()
    var python = JTextField()
    var pythonOpts = RawCommandLineEditor()
    var pythonWorkDir = JTextField()
    var vagrant = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Vagrant Command", "", project,
                FileChooserDescriptorFactory.createSingleFileDescriptor())
    }
    var vagrantHost = JTextField()
    var vagrantWorkDir = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Vagrant Working Directory", "", project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor())
    }

    /**
     * Create the widget for this editor.
     *
     * @return UI widget
     */
    override fun createEditor(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel {
            row() {
                targetType()
                target()
            }
            row("Parameters:") { targetParams() }
            titledRow("Remote Environment") {}
            row("Python interpreter:") { python() }
            row("Python options:") { pythonOpts() }
            row("Remote working directory:") { pythonWorkDir() }
            titledRow("Local Environment") {}
            row("Vagrant host:") { vagrantHost() }
            row("Vagrant command:") { vagrant() }
            row("Local working directory:") { vagrantWorkDir() }
        }
    }

    /**
     * Reset editor fields from the configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: VagrantRunConfiguration) {
        config.apply {
            target.text = settings.target
            targetType.selectedIndex = targetTypeValues.indexOf(settings.targetType)
            targetParams.text = settings.targetParams
            python.text = settings.python
            pythonOpts.text = settings.pythonOpts
            pythonWorkDir.text = settings.pythonWorkDir
            vagrant.text = settings.vagrant
            vagrantHost.text = settings.vagrantHost
            vagrantWorkDir.text = settings.vagrantWorkDir
        }
        return
    }

    /**
     * Apply editor fields to the configuration state.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: VagrantRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        config.apply {
            settings = VagrantRunSettings()
            settings.target = target.text
            settings.targetType = targetTypeValues[targetType.selectedIndex]
            settings.targetParams = targetParams.text
            settings.python = python.text
            settings.pythonOpts = pythonOpts.text
            settings.pythonWorkDir = pythonWorkDir.text
            settings.vagrant = vagrant.text
            settings.vagrantHost = vagrantHost.text
            settings.vagrantWorkDir = vagrantWorkDir.text
        }
        return
    }
}


/**
 * Manage VagrantRunConfiguration runtime settings.
 */
class VagrantRunSettings internal constructor() {

    companion object {
        private const val JDOM_TAG = "python-vagrant"
    }

    var target = ""
    var targetType = PythonTargetType.SCRIPT
    var targetParams = ""
    var python = ""
        get() = if (field.isNotBlank()) field else "python3"
    var pythonOpts = ""
    var pythonWorkDir = ""
    var vagrant = ""
        get() = if (field.isNotBlank()) field else "vagrant"
    var vagrantHost = ""
    var vagrantWorkDir = ""

    /**
     * Construct object from a JDOM element.
     *
     * @param element: input element
     */
    internal constructor(element: Element) : this() {
        element.getOrCreate(JDOM_TAG).let {
            target = JDOMExternalizerUtil.readField(it, "target", "")
            targetType = PythonTargetType.valueOf(JDOMExternalizerUtil.readField(it, "targetType", "SCRIPT"))
            targetParams = JDOMExternalizerUtil.readField(it, "targetParams", "")
            python = JDOMExternalizerUtil.readField(it, "python", "")
            pythonOpts = JDOMExternalizerUtil.readField(it, "pythonOpts", "")
            pythonWorkDir = JDOMExternalizerUtil.readField(it, "pythonWorkDir", "")
            vagrant = JDOMExternalizerUtil.readField(it, "vagrant", "")
            vagrantHost = JDOMExternalizerUtil.readField(it, "vagrantHost", "")
            vagrantWorkDir = JDOMExternalizerUtil.readField(it, "vagrantWorkDir", "")
        }
        return
    }

    /**
     * Write settings to a JDOM element.
     *
     * @param element: output element
     */
    fun write(element: Element) {
        element.getOrCreate(JDOM_TAG).let {
            JDOMExternalizerUtil.writeField(it, "target", target)
            JDOMExternalizerUtil.writeField(it, "targetType", targetType.name)
            JDOMExternalizerUtil.writeField(it, "targetParams", targetParams)
            JDOMExternalizerUtil.writeField(it, "python", python)
            JDOMExternalizerUtil.writeField(it, "pythonOpts", pythonOpts)
            JDOMExternalizerUtil.writeField(it, "pythonWorkDir", pythonWorkDir)
            JDOMExternalizerUtil.writeField(it, "vagrant", vagrant)
            JDOMExternalizerUtil.writeField(it, "vagrantHost", vagrantHost)
            JDOMExternalizerUtil.writeField(it, "vagrantWorkDir", vagrantWorkDir)
        }
        return
    }
}
