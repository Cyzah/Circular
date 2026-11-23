// 顶层插件、维护源码行数门禁与聚合任务；Android 配置在 app 模块。
plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val maintainedPatterns = listOf("**/*.java", "**/*.kt", "**/*.kts", "**/*.ps1", "**/*.gradle")
val maintainedSources = files(
    listOf("app/src", "tools", "gradle").map { directory ->
        fileTree(directory) { include(maintainedPatterns) }
    },
    fileTree(projectDir) { include("*.java", "*.kt", "*.kts", "*.ps1", "*.gradle") }
)

val checkSourceSize = tasks.register("checkSourceSize") {
    group = "verification"
    description = "Reject maintained source files longer than 1500 lines."
    inputs.files(maintainedSources)
    doLast {
        val violations = maintainedSources.files.sortedBy { it.path }.mapNotNull { source ->
            val count = source.useLines { it.count() }
            if (count > 1500) "${source.relativeTo(projectDir)} has $count lines; limit is 1500" else null
        }
        if (violations.isNotEmpty()) {
            throw GradleException(violations.joinToString("\n"))
        }
        logger.lifecycle("Source size check passed (${maintainedSources.files.size} files, limit 1500)")
    }
}

tasks.named("check") { dependsOn(checkSourceSize, ":app:check") }
tasks.named("assemble") { dependsOn(":app:assemble") }
