import java.io.File
import java.util.*

fun main(args: Array<String>) = Generator.main(args)

data class AType(
	val prim: String,
	val size: Int,
	val int: Boolean,
	val commonName: String = "${if (int) "Int" else "Float"}${size * 8}Buffer",
	val jsName: String = "${if (int) "Int" else "Float"}${size * 8}Array",
	val jvmName: String = "${prim}Buffer",
	val karray: String = "${prim}Array"
)

object Generator {
	val AUTOGEN_NOTICE = "@WARNING: File AUTOGENERATED by `kmem/jvm/src/test/kotlin/Generator.kt` do not modify manually!"

	val INT8 = AType("Byte", size = 1, int = true)
	val INT16 = AType("Short", size = 2, int = true)
	val INT32 = AType("Int", size = 4, int = true)
	val FLOAT32 = AType("Float", size = 4, int = false)
	val FLOAT64 = AType("Double", size = 8, int = false)

	val TYPES = listOf(INT8, INT16, INT32, FLOAT32, FLOAT64)

	@JvmStatic
	fun main(args: Array<String>) {
		println("Generated")
		println(File(".").absolutePath)
		//File("temp/KmemGen.kt").writeText(generateCommon().joinToString("\n"))
		//File("temp/KmemGenJs.kt").writeText(generateJs().joinToString("\n"))
		//File("temp/KmemGenJvm.kt").writeText(generateJvm().joinToString("\n"))

		File("kmem/common/src/main/kotlin/com/soywiz/kmem/KmemGen.kt").writeText(generateCommon().joinToString("\n"))
		File("kmem/js/src/main/kotlin/com/soywiz/kmem/KmemGenJs.kt").writeText(generateJs().joinToString("\n"))
		File("kmem/jvm/src/main/kotlin/com/soywiz/kmem/KmemGenJvm.kt").writeText(generateJvm().joinToString("\n"))
	}

	fun generateCommon(): List<String> {
		val out = arrayListOf<String>()

		fun line(str: String = "") = run { out += str }

		line("// $AUTOGEN_NOTICE")
		line("@file:Suppress(\"NOTHING_TO_INLINE\", \"EXTENSION_SHADOWED_BY_MEMBER\", \"RedundantUnitReturnType\", \"FunctionName\")")
		line("package com.soywiz.kmem")
		line()
		line("expect class MemBuffer")
		line("expect fun MemBufferAlloc(size: Int): MemBuffer")
		line("expect fun MemBufferWrap(array: ByteArray): MemBuffer")
		line("expect val MemBuffer.size: Int")
		line()
		for (type in TYPES) type.apply {
			line("expect fun MemBuffer._slice$commonName(byteOffset: Int, size: Int): $commonName")
		}
		line()
		for (type in TYPES) type.apply {
			line("fun MemBuffer.slice$commonName(byteOffset: Int = 0, size: Int = (this.size - byteOffset) / $size): $commonName = this._slice$commonName(byteOffset, size)")
		}
		line()
		for (type in TYPES) type.apply {
			line("fun MemBuffer.as$commonName(): $commonName = this.slice$commonName()")
		}
		line()

		for (type in TYPES) type.apply {
			line("expect class $commonName")
			line("expect val $commonName.buffer: MemBuffer")
			line("expect val $commonName.byteOffset: Int")
			line("expect val $commonName.size: Int")
			line("expect operator fun $commonName.get(index: Int): $prim")
			line("expect operator fun $commonName.set(index: Int, value: $prim): Unit")
			line()
		}

		for (type in TYPES) type.apply {
			line("expect fun arraycopy(src: $karray, srcPos: Int, dst: $karray, dstPos: Int, size: Int): Unit")
		}
		line()

		line("expect fun arraycopy(src: MemBuffer, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit")
		for (type in TYPES) type.apply {
			line("expect fun arraycopy(src: $karray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit")
			line("expect fun arraycopy(src: MemBuffer, srcPos: Int, dst: $karray, dstPos: Int, size: Int): Unit")
		}
		line()

		for (type in TYPES) type.apply {
			line("@PublishedApi expect internal fun _fill(array: $karray, value: $prim, pos: Int, size: Int): Unit")
		}
		line()

		for (type in TYPES) type.apply {
			line("inline fun $karray.fill(value: $prim, pos: Int = 0, size: Int = this.size): Unit = _fill(this, value, pos, size)")
		}
		line()

		return out
	}

	fun generateJs(): List<String> {
		val out = arrayListOf<String>()

		fun line(str: String = "") = run { out += str }

		line("// $AUTOGEN_NOTICE")
		line("@file:Suppress(\"NOTHING_TO_INLINE\", \"EXTENSION_SHADOWED_BY_MEMBER\", \"RedundantUnitReturnType\", \"FunctionName\", \"UnsafeCastFromDynamic\")")
		line("package com.soywiz.kmem")
		line()
		line("import org.khronos.webgl.*")
		line()
		line("actual typealias MemBuffer = ArrayBuffer")
		line("actual inline fun MemBufferAlloc(size: Int): MemBuffer = ArrayBuffer(size)")
		line("actual inline fun MemBufferWrap(array: ByteArray): MemBuffer = array.unsafeCast<Int8Array>().buffer")
		line("actual inline val MemBuffer.size: Int get() = this.byteLength")
		line()
		for (type in TYPES) type.apply {
			line("actual inline fun MemBuffer._slice$commonName(byteOffset: Int, size: Int): $commonName = $jsName(this, byteOffset, size)")
		}
		line()

		for (type in TYPES) type.apply {
			line("actual typealias $commonName = $jsName")
			line("actual inline val $commonName.buffer: MemBuffer get() = this.buffer")
			line("actual inline val $commonName.byteOffset: Int get() = this.byteOffset")
			line("actual inline val $commonName.size: Int get() = this.asDynamic().length")
			line("actual inline operator fun $commonName.get(index: Int): $prim = this.asDynamic()[index]")
			line("actual inline operator fun $commonName.set(index: Int, value: $prim): Unit = run { this.asDynamic()[index] = value }")
			line()
		}

		for (type in TYPES) type.apply {
			line("inline fun $karray.as$jsName(): $jsName = this.unsafeCast<$jsName>()")
			line("inline fun $karray.asTyped(): $jsName = this.unsafeCast<$jsName>()")
		}
		line()
		for (type in TYPES) type.apply {
			line("actual fun arraycopy(src: $karray, srcPos: Int, dst: $karray, dstPos: Int, size: Int): Unit = dst.asTyped().set(src.asTyped().subarray(srcPos, srcPos + size), dstPos)")
		}
		line()

		line("actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = Int8Array(dst, dstPos).set(Int8Array(src, srcPos, size), 0)")
		for (type in TYPES) type.apply {
			line("actual fun arraycopy(src: $karray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = $jsName(dst).set(src.asTyped().subarray(srcPos, srcPos + size), dstPos)")
			line("actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: $karray, dstPos: Int, size: Int): Unit = dst.asTyped().set(dst.asTyped().subarray(srcPos, srcPos + size), dstPos)")
		}

		line()

		for (type in TYPES) type.apply {
			line("@PublishedApi actual inline internal fun _fill(array: $karray, value: $prim, pos: Int, size: Int): Unit = run { array.asDynamic().fill(value, pos, pos + size) }")
		}
		line()

		return out
	}

	fun generateJvm(): List<String> {
		val out = arrayListOf<String>()
		fun line(str: String = "") = run { out += str }

		line("// $AUTOGEN_NOTICE")
		line("@file:Suppress(\"NOTHING_TO_INLINE\", \"EXTENSION_SHADOWED_BY_MEMBER\", \"RedundantUnitReturnType\", \"FunctionName\")")
		line("package com.soywiz.kmem")
		line()
		line("import java.nio.*")
		line("import java.util.*")
		line()
		line("actual class MemBuffer(val buffer: ByteBuffer)")
		line("actual fun MemBufferAlloc(size: Int): MemBuffer = TODO()")
		line("actual fun MemBufferWrap(array: ByteArray): MemBuffer = TODO()")
		line("actual inline val MemBuffer.size: Int get() = buffer.capacity()")
		line()
		for (type in TYPES) type.apply {
			line("actual fun MemBuffer._slice$commonName(byteOffset: Int, size: Int): $commonName = TODO()")
		}
		line()

		for (type in TYPES) type.apply {
			line("actual class $commonName(val mbuffer: MemBuffer, val jbuffer: $jvmName)")
			line("actual val $commonName.buffer: MemBuffer get() = TODO()")
			line("actual val $commonName.byteOffset: Int get() = TODO()")
			line("actual val $commonName.size: Int get() = TODO()")
			line("actual operator fun $commonName.get(index: Int): $prim = TODO()")
			line("actual operator fun $commonName.set(index: Int, value: $prim): Unit = TODO()")
			line()
		}

		for (type in TYPES) type.apply {
			line("actual fun arraycopy(src: $karray, srcPos: Int, dst: $karray, dstPos: Int, size: Int): Unit = TODO()")
		}
		line()

		line("actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()")
		for (type in TYPES) type.apply {
			line("actual fun arraycopy(src: $karray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()")
			line("actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: $karray, dstPos: Int, size: Int): Unit = TODO()")
		}
		line()

		for (type in TYPES) type.apply {
			line("@PublishedApi actual internal fun _fill(array: $karray, value: $prim, pos: Int, size: Int): Unit = Arrays.fill(array, pos, pos + size, value)")
		}
		line()

		return out
	}
}