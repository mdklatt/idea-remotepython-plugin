/**
 * Unit tests for the Password module.
 */
package dev.mdklatt.idea.remotepython.run.test

import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import dev.mdklatt.idea.remotepython.run.StoredPassword


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
        password.value = null  // remove from password store
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
