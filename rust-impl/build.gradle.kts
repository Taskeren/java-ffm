plugins {
	id("io.github.arc-blroth.cargo-wrapper") version "1.1.0"
}

cargo {
	outputs = mapOf("" to System.mapLibraryName("rust_impl"))
}

/**
 * Must have cbindgen installed before running this task.
 *
 * cargo install --force cbindgen
 */
tasks.create<Exec>("cbindgen") {
	commandLine("cbindgen", "--config", "cbindgen.toml", "--crate", "rust_impl", "--output", "build/rust_impl.h")
}