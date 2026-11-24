import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// ---- Versioning (never hardcoded): the release workflow passes -PversionName / -PversionCode
// derived from the tag. A local build is 0.0.0, which the App updates section shows as a build
// from source.
val appVersionName: String = providers.gradleProperty("versionName").orNull ?: "0.0.0"
val appVersionCode: Int = providers.gradleProperty("versionCode").orNull?.toIntOrNull() ?: 1

// ---- Signing: CI sets SIGNING_* env vars; local release builds use an untracked keystore.properties.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

// Blank counts as missing: GitHub Actions passes an unset secret as an empty string.
fun signingValue(env: String, key: String): String? =
    providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }

// Only sign with a keystore that really exists and has content; otherwise the release build is
// left unsigned instead of failing. The shared release workflow refuses to publish an unsigned APK.
val releaseKeystore: File? = signingValue("SIGNING_STORE_FILE", "storeFile")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.isFile && it.length() > 0L }

android {
    namespace = "com.ks.expense"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ks.expense"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            releaseKeystore?.let { keystore ->
                storeFile = keystore
                storePassword = signingValue("SIGNING_STORE_PASSWORD", "storePassword")
                keyAlias = signingValue("SIGNING_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("SIGNING_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        // A separate app, so testing never touches the real app's data.
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let {
                signingConfig = it
            }
        }
    }

    // Interface text: generated from src/main/lang by ks-utility's lang.mjs (below).
    sourceSets.getByName("main").res.directories.add("build/generated/lang/res")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Exported schemas (specs/constraints.md): every change needs a Migration and a migration test.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    constraints {
        implementation(libs.kotlinx.serialization.core) { because("room-testing needs 1.8.1; app and test APK must match") }
    }

    // Generic utilities shared across KS-Suite apps (git submodule ks-utility/).
    implementation(libs.ks.utility)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

// ---- Languages (ks-utility specs/usage.md#language-files) -------------------------------------
val langTool = rootProject.file("ks-utility/typescript/tools/lang.mjs")
val langDir = file("src/main/lang")
val checkLanguageFiles by tasks.registering(Exec::class) {
    inputs.dir(langDir)
    commandLine("node", langTool, "check", langDir)
}
val languageResources by tasks.registering(Exec::class) {
    val out = layout.buildDirectory.dir("generated/lang/res")
    inputs.dir(langDir)
    outputs.dir(out)
    commandLine("node", langTool, "android", langDir, "--out", out.get().asFile)
}
tasks.named("check") { dependsOn(checkLanguageFiles) }
tasks.named("preBuild") { dependsOn(languageResources) }

// ---- Offline only (specs/constraints.md): the merged manifest must never ask for INTERNET.
abstract class CheckNoInternet : DefaultTask() {
    @get:InputFile
    abstract val manifest: RegularFileProperty

    @TaskAction
    fun check() {
        val text = manifest.get().asFile.readText()
        if ("android.permission.INTERNET" in text) {
            throw GradleException("The merged manifest declares android.permission.INTERNET; this app is offline only.")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val checkTask = tasks.register<CheckNoInternet>("checkNoInternet$name") {
            manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        tasks.configureEach {
            if (this.name == "assemble$name" || this.name == "bundle$name" || this.name == "check") {
                dependsOn(checkTask)
            }
        }
    }
}
