/**
 * Unit tests for CommandLine.kt.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.common.exec.PosixCommandLine


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the CommandLine class.
 */
internal class PosixCommandLineTest : BasePlatformTestCase() {

    private lateinit var command: PosixCommandLine

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        command = PosixCommandLine("python", "--version")
    }

    /**
     * Test the withPythonVenv() extension method.
     */
    fun testWithPythonVenv() {
        val venv = "venv"
        assertTrue(command == command.withPythonVenv(venv))
        assertEquals("${venv}/bin:\$PATH", command.environment["PATH"])
        assertEquals(venv, command.environment["VIRTUAL_ENV"])
    }
}
