
/////////////////////////
/////////////////////////

fun ByteArray.contains(other: ByteArray): Boolean = indexOf(other) >= 0

fun ByteArray.indexOf(other: ByteArray): Int {
    val full = this
    for (n in 0 until full.size - other.size) if (other.indices.all { full[n + it] == other[it] }) return n
    return -1
}
