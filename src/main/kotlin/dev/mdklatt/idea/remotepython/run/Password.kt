/**
 * Password handling.
 */
package dev.mdklatt.idea.remotepython.run

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.Logger


/**
 * Manage a password in the system credential store.
 *
 * CharArrays are used for password values so that they can be explicitly
 * cleared from memory when no longer needed.
 *
 * @param key: storage key
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html">Persisting Sensitive Data</a>
 * @see <a href="https://stackoverflow.com/q/8881291>Why is char[] preferred over String for passwords?</a>
 */
internal class StoredPassword(key: String) {

    private val logger = Logger.getInstance(this::class.java)
    private val service = generateServiceName(this::class.java.getPackage().name, key)
    private val credentialAttributes = CredentialAttributes(service)

    public var value: CharArray?
        get() {
            logger.debug("Loading password from credential store for $service")
            return PasswordSafe.instance.getPassword(credentialAttributes)?.toCharArray()
        }
        set(value) {
            if (value != null) {
                logger.debug("Saving password to credential store for $service")
                PasswordSafe.instance.set(credentialAttributes, Credentials(null, value))
            } else {
                logger.debug("Removing password from credential store for $service")
                PasswordSafe.instance.set(credentialAttributes, null)
            }
        }
}
