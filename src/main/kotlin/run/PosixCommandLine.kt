/**
 * Extensions for the CommandLine class.
 */
package dev.mdklatt.idea.remotepython.run

import dev.mdklatt.idea.common.exec.PosixCommandLine
import kotlin.io.path.Path
import java.io.File
import kotlin.io.path.pathString


/**
 * Activate a Python virtualenv for execution.
 *
 * @param venvPath: path to virtualenv directory
 * @return modified instance
 */
internal fun PosixCommandLine.withPythonVenv(venvPath: String): PosixCommandLine {
    // Per virtualenv docs, all activators do is prepend the environment's bin/
    // directory to PATH. Per inspection of an installed 'activate' script, a
    // VIRTUAL_ENV variable is also set, and PYTHONHOME is unset if it exists.
    // <https://virtualenv.pypa.io/en/latest/user_guide.html#activators>
    withEnvironment(mapOf<String, Any?>(
        "VIRTUAL_ENV" to venvPath,
        "PATH" to "${venvPath}/bin:\$PATH",
        "PYTHONHOME" to "",  // unset
    ))
    return this
}
