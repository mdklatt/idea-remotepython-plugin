/**
 * Unit tests for the Password module.
 */
package dev.mdklatt.idea.remotepython.run.test

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.remotepython.run.*
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions


/**
 * Unit tests for the StoredPassword class.
 */
internal class StoredPasswordTest {

    private val value = charArrayOf('1', '2', '3', '4')
    private val password = StoredPassword(this::class.java.getPackage().name)

    /**
     * Per-test cleanup.
     */
    @AfterEach
    fun tearDown() {
        password.value = null  // remove from credential store
    }

    /**
     * Test an undefined password.
     */
    @Test
    fun testUndefined() {
        assertNull(password.value)
    }

    /**
     * Test value set/get.
     */
    @Test
    fun testValue() {
        password.value = value
        assertContentEquals(value, password.value)
    }

    /**
     * Test password removal.
     */
    @Test
    fun testRemove() {
        password.value = value
        password.value = null
        assertNull(password.value)
    }
 }


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.

/**
 * Unit tests for the PasswordDialog class.
 */
internal class PasswordDialogTest : BasePlatformTestCase() {

    private lateinit var dialog: PasswordDialog

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        dialog = PasswordDialog("Password Test")
    }

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        Assertions.assertEquals("Password Test", dialog.title)
    }
}