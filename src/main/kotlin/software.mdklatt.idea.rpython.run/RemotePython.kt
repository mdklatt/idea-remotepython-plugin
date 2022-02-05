package software.mdklatt.idea.rpython.run

import com.intellij.execution.configurations.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.util.getOrCreate
import org.jdom.Element
import java.util.*


/**
 * TODO
 */
class RemotePythonConfigurationType : ConfigurationType {
    /**
     * The ID of the configuration type. Should be camel-cased without dashes, underscores, spaces and quotation marks.
     * The ID is used to store run configuration settings in a project or workspace file and
     * must not change between plugin versions.
     */
    override fun getId(): String = this::class.java.simpleName

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
enum class PythonTargetType { MODULE, SCRIPT }  // TODO: internal


/**
 * Manage common configuration settings.
 */
internal abstract class RemotePythonSettings protected constructor() {

    internal abstract val xmlTagName: String

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    private var id: UUID? = null

    var targetName = ""
    var targetType = PythonTargetType.SCRIPT
    var targetParams = ""
    var pythonExe = ""
        get() = field.ifBlank { "python3" }
    var pythonOpts = ""
    var remoteWorkDir = ""
    var localWorkDir = ""

    /**
     * Load stored settings.
     *
     * @param element: JDOM element
     */
    internal open fun load(element: Element) {
        element.getOrCreate(xmlTagName).let {
            val str = JDOMExternalizerUtil.readField(it, "id", "")
            id = if (str.isEmpty()) UUID.randomUUID() else UUID.fromString(str)
            logger.debug("loading settings for configuration ${id.toString()}")
            targetName = JDOMExternalizerUtil.readField(it, "targetName", "")
            targetType = PythonTargetType.valueOf(JDOMExternalizerUtil.readField(it, "targetType", "SCRIPT"))
            targetParams = JDOMExternalizerUtil.readField(it, "targetParams", "")
            pythonExe = JDOMExternalizerUtil.readField(it, "pythonExe", "")
            pythonOpts = JDOMExternalizerUtil.readField(it, "pythonOpts", "")
            remoteWorkDir = JDOMExternalizerUtil.readField(it, "remoteWorkDir", "")
            localWorkDir = JDOMExternalizerUtil.readField(it, "localWorkDir", "")
        }
        return
    }

    /**
     * Save settings.
     *
     * @param element: JDOM element
     */
    internal open fun save(element: Element) {
        val default = element.getAttributeValue("default")?.toBoolean() ?: false
        element.getOrCreate(xmlTagName).let {
            if (!default) {
                id = id ?: UUID.randomUUID()
                logger.debug("saving settings for configuration ${id.toString()}")
                JDOMExternalizerUtil.writeField(it, "id", id.toString())
            } else {
                logger.debug("saving settings for default configuration")
            }
            JDOMExternalizerUtil.writeField(it, "targetName", targetName)
            JDOMExternalizerUtil.writeField(it, "targetType", targetType.name)
            JDOMExternalizerUtil.writeField(it, "targetParams", targetParams)
            JDOMExternalizerUtil.writeField(it, "pythonExe", pythonExe)
            JDOMExternalizerUtil.writeField(it, "pythonOpts", pythonOpts)
            JDOMExternalizerUtil.writeField(it, "remoteWorkDir", remoteWorkDir)
            JDOMExternalizerUtil.writeField(it, "localWorkDir", localWorkDir)
        }
        return
    }
}
