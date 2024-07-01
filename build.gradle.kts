import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinProjectExtension>()?.apply {
            jvmToolchain(
                libs.versions.toolchain
                    .get()
                    .toInt(),
            )
        }
    }
}

fun execTask(taskName: String) {
    var execute = "gradlew.bat"
    if (!OperatingSystem.current().isWindows) {
        execute = "gradlew"
    }
    exec {
        executable = execute
        args = listOf(taskName, "--scan")
    }
}
tasks.register("publish") {
    doLast { execTask(":plugin:publishToCentralPortal") }
}
tasks.register("format") {
    doLast { execTask(":plugin:ktlintFormat") }
}
