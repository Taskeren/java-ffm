plugins {
	kotlin("jvm")
	application
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
	mavenCentral()
}

val rust: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	rust(project(":rust-impl"))

	testImplementation(kotlin("test"))
}

tasks.test {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(23)
}

tasks.processResources {
	from(rust)
}

application {
	mainClass = "MainKt"
}

tasks.named<JavaExec>("run") {
	workingDir = file("${layout.projectDirectory}/run").also { it.mkdirs() }
	jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.named<JavaExec>("runShadow") {
	workingDir = file("${layout.projectDirectory}/run").also { it.mkdirs() }
	jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
