import java.util.Properties

val signingPropertiesFile = rootProject.file("local-signing.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) {
        signingPropertiesFile.reader(Charsets.UTF_8).use { load(it) }
    }
}

fun signingValue(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull
        ?: signingProperties.getProperty(propertyName)?.trim()?.takeIf { it.isNotEmpty() }

val configuredSigningStore = signingValue(
    "CIRCULAR_KEYSTORE",
    "storeFile"
)?.let { rootProject.file(it) }
val releaseSigningStore = configuredSigningStore
val releaseStorePassword = signingValue(
    "CIRCULAR_KEYSTORE_PASSWORD",
    "storePassword"
)
val releaseKeyAlias = signingValue(
    "CIRCULAR_KEY_ALIAS",
    "keyAlias"
)
val releaseKeyPassword = signingValue(
    "CIRCULAR_KEY_PASSWORD",
    "keyPassword"
)
val releaseSigningStoreMissing =
    releaseSigningStore == null || !releaseSigningStore.isFile
val missingReleaseSigning = listOfNotNull(
    "CIRCULAR_KEYSTORE/local storeFile".takeIf { releaseSigningStoreMissing },
    "CIRCULAR_KEYSTORE_PASSWORD/storePassword".takeIf { releaseStorePassword == null },
    "CIRCULAR_KEY_ALIAS/keyAlias".takeIf { releaseKeyAlias == null },
    "CIRCULAR_KEY_PASSWORD/keyPassword".takeIf { releaseKeyPassword == null }
)
val hasReleaseSigning = missingReleaseSigning.isEmpty()
val useReleaseSigningForDebug = providers.gradleProperty(
    "useReleaseSigningForDebug"
).orNull.toBoolean()
if (useReleaseSigningForDebug && !hasReleaseSigning) {
    throw GradleException(
        "Debug Release-signing was requested but signing is unavailable: "
                + missingReleaseSigning.joinToString()
    )
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val versionProperties = Properties().apply {
    rootProject.file("version.properties").reader(Charsets.UTF_8).use { load(it) }
}
val configuredVersionName = listOf("major", "minor", "patch")
    .joinToString(".") { versionProperties.getProperty(it) }
val configuredVersionCode = versionProperties.getProperty("versionCode").toInt()

androidComponents {
    onVariants { variant ->
        val editionSuffix = if (variant.productFlavors.any { it.second == "lspRepo" }) "-lsp-repo" else ""
        val buildSuffix = if (variant.buildType == "release") "" else "-${variant.buildType}"
        variant.outputs.forEach { output ->
            output.outputFileName.set(output.versionName.map { version ->
                "Circular-$version$editionSuffix$buildSuffix.apk"
            })
        }
    }
}

android {
    namespace = "com.cyzah.circular"
    compileSdk = 37
    compileSdkMinor = 2
    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.cyzah.circular"
        // Android 15 及以上；apksig 在 minSdk >= 28 时会跳过 v2，
        // 所以本包实际是 v3-only（下面的 enableV2Signing 只在把 minSdk 降到 27 及以下时才生效）。
        minSdk = 35
        targetSdk = 35
        versionCode = configuredVersionCode
        versionName = configuredVersionName
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "DIAGNOSTIC_REPORTS", "true")
        }
        create("lspRepo") {
            dimension = "distribution"
            applicationId = "io.github.cyzah.circular"
            buildConfigField("boolean", "DIAGNOSTIC_REPORTS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 签名：使用本机未跟踪 JKS；可在 local-signing.properties 或环境变量中配置
    signingConfigs {
        getByName("debug") { storeFile = rootProject.file(".local/android/debug.keystore") }
        create("release") {
            if (hasReleaseSigning) {
                storeFile = releaseSigningStore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
            // 打包签名方案：V2 + V3（关闭 V1/V4）
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
        debug {
            signingConfig = if (useReleaseSigningForDebug) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    // LSPosed 模块元数据位于 src/main/resources/META-INF/xposed/，需原样打包
    packaging {
        resources {
            merges += listOf()
            excludes += listOf()
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Runtime targets the supported HyperOS API; release shrinks unused UI code/resources.
        // Module entry points and Java resources are checked separately after packaging.
        disable += listOf("OldTargetApi")
    }
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        if (!hasReleaseSigning) {
            throw GradleException(
                "Release signing is unavailable. Set: ${missingReleaseSigning.joinToString()}"
            )
        }
    }
}

tasks.configureEach {
    if (name == "preBuild" || name == "check") {
        dependsOn(rootProject.tasks.named("checkSourceSize"))
    }
    if (name == "preGithubReleaseBuild" || name == "preLspRepoReleaseBuild") {
        dependsOn(validateReleaseSigning)
    }
}

dependencies {
    // 由 LSPosed 框架在运行时提供，不打包进 APK；
    // Framework API is supplied by LSPosed; Compose runtime is packaged normally.
    compileOnly(libs.libxposed.api) { isTransitive = false }
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.miuix.glass)
    implementation(libs.miuix.preference)
    testImplementation(libs.junit)
}


kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
