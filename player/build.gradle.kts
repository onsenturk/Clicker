import java.util.Properties
import java.util.jar.JarInputStream
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.streamvault.player"
    compileSdk = 36

    defaultConfig {
        minSdk = 25
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        baseline = file("lint-baseline.xml")
        warningsAsErrors = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

kover {
    currentProject {
        createVariant("ci") {
            add("debug")
        }
    }
}

val ffmpegArtifactName = "media3-decoder-ffmpeg-${libs.versions.media3.get()}"
val ffmpegAarFile = layout.projectDirectory.file("libs/$ffmpegArtifactName.aar").asFile
val ffmpegManifestFile = layout.projectDirectory.file("libs/$ffmpegArtifactName.properties").asFile

val verifyLocalFfmpegArtifact by tasks.registering {
    group = "verification"
    description = "Verifies the bundled Media3 FFmpeg artifact, metadata, and supported ABIs."

    val artifactFile = ffmpegAarFile
    val manifestFile = ffmpegManifestFile
    val expectedMedia3Version = libs.versions.media3.get()
    inputs.file(artifactFile)
    inputs.file(manifestFile)
    inputs.property("media3Version", expectedMedia3Version)

    doLast {
        check(artifactFile.isFile) {
            "Required FFmpeg artifact missing: ${artifactFile.absolutePath}"
        }
        check(manifestFile.isFile) {
            "Required FFmpeg manifest missing: ${manifestFile.absolutePath}"
        }

        val manifest = Properties().apply {
            manifestFile.inputStream().use(::load)
        }
        check(manifest.getProperty("media3Version") == expectedMedia3Version) {
            "FFmpeg manifest media3Version must be $expectedMedia3Version"
        }

        val enabledDecoders = manifest.getProperty("enabledDecoders")
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
        check("mp2" in enabledDecoders) {
            "FFmpeg artifact must include the mp2 decoder for MPEG layer II audio streams"
        }

        ZipFile(artifactFile).use { archive ->
            listOf("arm64-v8a", "armeabi-v7a").forEach { abi ->
                val nativeLibrary = checkNotNull(archive.getEntry("jni/$abi/libffmpegJNI.so")) {
                    "FFmpeg artifact is missing libffmpegJNI.so for $abi"
                }
                val nativeLibraryText = archive.getInputStream(nativeLibrary).use {
                    it.readBytes().toString(Charsets.ISO_8859_1)
                }
                check("ff_mp2_decoder" in nativeLibraryText) {
                    "FFmpeg native library is missing the mp2 decoder: ${nativeLibrary.name}"
                }
            }

            val classesJar = checkNotNull(archive.getEntry("classes.jar")) {
                "FFmpeg artifact is missing classes.jar"
            }
            val requiredClasses = mutableSetOf(
                "androidx/media3/decoder/ffmpeg/FfmpegLibrary.class",
                "androidx/media3/decoder/ffmpeg/FfmpegAudioRenderer.class"
            )
            archive.getInputStream(classesJar).use { input ->
                JarInputStream(input).use { jar ->
                    generateSequence { jar.nextJarEntry }.forEach { entry ->
                        requiredClasses.remove(entry.name)
                        if (entry.name == "androidx/media3/decoder/ffmpeg/FfmpegLibrary.class") {
                            check("audio/mpeg-L2" in jar.readBytes().toString(Charsets.ISO_8859_1)) {
                                "FFmpeg FfmpegLibrary must expose audio/mpeg-L2 MIME type for MPEG layer II audio"
                            }
                        }
                    }
                }
            }
            check(requiredClasses.isEmpty()) {
                "FFmpeg artifact is missing required classes: $requiredClasses"
            }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyLocalFfmpegArtifact)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(project(":domain"))

    // Media3
    api(files(ffmpegAarFile).builtBy(verifyLocalFfmpegArtifact))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.smoothstreaming)
    implementation(libs.media3.exoplayer.rtsp)  // PE-H03: RTSP stream support
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // OkHttp (for custom data source)
    implementation(libs.okhttp)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Core
    implementation(libs.core.ktx)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
