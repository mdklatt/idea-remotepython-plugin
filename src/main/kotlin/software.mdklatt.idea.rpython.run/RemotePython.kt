package software.mdklatt.idea.rpython.run.software.mdklatt.idea.rpython.run

import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import software.mdklatt.idea.rpython.run.*


/**
 * Handle persistence of run configuration options.
 *
 * This base class defines options common to all Remote Python configurations.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
abstract class RemotePythonOptions : RunConfigurationOptions() {
    internal var targetType by string()
    internal var targetName by string()
    internal var targetParams by string()
    internal var pythonExe by string()
    internal var pythonOpts by string()
    internal var remoteWorkDir by string()
    internal var localWorkDir by string()
}


/**
 * Run configuration for remote Python execution.
 *
 * This base class defines options common to all Remote Python configurations.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
abstract class RemotePythonRunConfiguration<Options : RemotePythonOptions>(
    project: Project,
    factory: DockerConfigurationFactory,
    name: String
) :
    RunConfigurationBase<Options>(project, factory, name) {

    final override fun getOptions(): Options {
        // Kotlin considers this an unsafe cast because generics do not have
        // runtime type information unless they are reified, which is not
        // supported for class parameters.
        @Suppress("UNCHECKED_CAST")
        return super.getOptions() as Options
    }

    // TODO: Why can't options.<property> be used as a delegate?

    var targetType: TargetType
        get() = software.mdklatt.idea.rpython.run.TargetType.valueOf(options.targetType ?: "MODULE")
        set(value) {
            options.targetType = value.name
        }
    var targetName: String
        get() = options.targetName ?: ""
        set(value) {
            options.targetName = value
        }
    var targetParams: String
        get() = options.targetParams ?: ""
        set(value) {
            options.targetParams = value
        }
    var pythonExe: String
        get() = options.pythonExe ?: "python3"
        set(value) {
            options.pythonExe = value.ifBlank { "python3" }
        }
    var pythonOpts: String
        get() = options.pythonOpts ?: ""
        set(value) {
            options.pythonOpts = value
        }
    var remoteWorkDir: String
        get() = options.remoteWorkDir ?: ""
        set(value) {
            options.remoteWorkDir = value
        }
    var localWorkDir: String
        get() = options.localWorkDir ?: ""
        set(value) {
            options.localWorkDir = value
        }
}
