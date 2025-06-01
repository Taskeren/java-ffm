import java.lang.foreign.*
import java.nio.charset.Charset
import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

object Main

fun main() {
	println("Java version: " + System.getProperty("java.version"))

	val libPath = Path("rust_impl.dll")

	// check library
	libPath.deleteExisting()
	if(libPath.notExists()) {
		// try to extract the library from the JAR
		val resource = Main::class.java.classLoader.getResourceAsStream("rust_impl.dll")
			?: throw RuntimeException("Unable to extract required DLL file from the JAR")

		libPath.outputStream().use {
			resource.copyTo(it)
		}
	}
	println("Successfully loaded the Rust DLL!")

	// setup the native linker and symbol lookup
	val linker = Linker.nativeLinker()
	val lookup = SymbolLookup.libraryLookup(libPath, Arena.global())

	// find the function and setup the downcall handle
	val funcPtr = lookup.find("add_numbers").get()
	val func = linker.downcallHandle(
		funcPtr,
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
	)

	// invoke the function with arguments
	val value = func.invoke(1, 1)
	println("Result from Rust function: $value")
	assert(value == 2) { "Expected 2, got $value" }

	val funcPtr1 = lookup.find("print_str").get()
	val func1 = linker.downcallHandle(funcPtr1, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS))

	Arena.ofConfined().use { arena ->
		func1.invoke(arena.allocateFrom("Hello from Kotlin!"))
	}

	Arena.ofConfined().use { arena ->
		val seg = arena.malloc(1024)
		seg.setString(0, "Hello from Kotlin with malloc!", Charsets.UTF_8)
		println(seg.getString(0))
	}

	val funcPtr2 = lookup.find("greeting").get()
	val func2 = linker.downcallHandle(funcPtr2, FunctionDescriptor.of(ValueLayout.ADDRESS))

	Arena.ofConfined().use { arena ->
		val rustGreetingResult = func2.invokeExact() as MemorySegment
		println(rustGreetingResult.getStringUnknownLength())
	}
}

private fun Arena.malloc(size: Long): MemorySegment {
	val linker = Linker.nativeLinker()

	val funcPtrMalloc = linker.defaultLookup().find("malloc").orElseThrow()
	val funcMalloc = linker.downcallHandle(
		funcPtrMalloc,
		FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
	)

	val segment = funcMalloc.invokeExact(size) as MemorySegment

	println("Size, in bytes, of memory created by calling malloc(${size}): ${segment.byteSize()}")

	val funcPtrFree = linker.defaultLookup().find("free").orElseThrow()
	val funcFree = linker.downcallHandle(
		funcPtrFree,
		FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
	)

	return segment.reinterpret(size, this) {
		runCatching { funcFree.invoke(it) }.onFailure { e -> e.printStackTrace() }
	}
}

private fun MemorySegment.getStringUnknownLength(offset: Long = 0, charset: Charset = Charsets.UTF_8): String {
	val bytes = buildList {
		for(i in offset until Int.MAX_VALUE) {
			val byte = get(ValueLayout.JAVA_BYTE, i)
			if(byte == Byte.MIN_VALUE) {
				break
			} else {
				add(byte)
				println(byte)
			}
		}
	}
	return String(bytes.toByteArray(), charset)
}