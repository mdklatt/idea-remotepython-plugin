/**
 * Extensions for the Map class.
 */
package software.mdklatt.idea.remotepython.run


/**
 * Map extension method to return the first key corresponding to a value.
 */
internal fun <K, V> Map<K, V>.getKey(value: V) =
    // Beware, this is O(N).
    entries.firstOrNull { it.value == value }?.key ?: throw IllegalArgumentException("value '${value}' not found")
