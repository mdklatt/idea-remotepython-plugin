package software.mdklatt.idea.rpython.run

import com.intellij.execution.configurations.*
import com.intellij.icons.AllIcons


/**
 * TODO
 */
class RPythonConfigurationType : ConfigurationType {
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
    override fun getConfigurationFactories() = arrayOf<ConfigurationFactory>(
        RemoteConfigurationFactory(this),
        VagrantConfigurationFactory(this),
        DockerConfigurationFactory(this),
    )
}


/**
 * Python execution target types.
 */
enum class PythonTargetType { MODULE, SCRIPT }
