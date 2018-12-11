package com.soywiz.kmem

inline class UByteArrayInt(val bytes: ByteArray) {
    val size: Int get() = bytes.size
    operator fun get(index: Int) = bytes[index].toInt() and 0xFF
    operator fun set(index: Int, value: Int) = run { bytes[index] = value.toByte() }
}

fun UByteArrayInt(size: Int) = UByteArrayInt(ByteArray(size))

fun ByteArray.asUByteArrayInt() = UByteArrayInt(this)
fun UByteArrayInt.asByteArray() = this.bytes
