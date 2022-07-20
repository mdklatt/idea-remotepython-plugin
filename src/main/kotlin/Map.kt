/**
 * Extensions for the Map class.
 */
package software.mdklatt.idea.rpython.run


/**
 * Map extension method to return the key corresponding to a value.
 */
internal fun <K, V> Map<K, V>.getKey(value: V) =
    // Beware, this is O(N).
    entries.firstOrNull { it.value == value }?.key ?: throw IllegalArgumentException("value '${value}' not found")
