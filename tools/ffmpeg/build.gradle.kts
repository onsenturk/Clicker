plugins {
    id("com.android.library") version "8.10.1"
}

val media3Version = "1.11.0"
val media3Source = file(providers.gradleProperty("media3Source").get())
val ffmpegSource = file(providers.gradleProperty("ffmpegSource").get())
val decoderSource = media3Source.resolve("libraries/decoder_ffmpeg/src/main")

val prepareNativeNotices by tasks.registering(Copy::class) {
    from(media3Source.resolve("LICENSE")) {
        rename { "MEDIA3-FFMPEG-LICENSE.txt" }
    }
    from(ffmpegSource.resolve("COPYING.LGPLv2.1")) {
        rename { "FFMPEG-LGPLv2.1.txt" }
    }
    into(layout.buildDirectory.dir("generated/nativeNotices/META-INF"))
}

android {
    namespace = "androidx.media3.decoder.ffmpeg"
    compileSdk = 36

    defaultConfig {
        minSdk = 25
        ndk.abiFilters += setOf("arm64-v8a", "armeabi-v7a")
    }

    sourceSets.getByName("main") {
        manifest.srcFile(decoderSource.resolve("AndroidManifest.xml"))
        java.srcDir(decoderSource.resolve("java"))
        jniLibs.srcDir(file(providers.gradleProperty("ffmpegNativeOutput").get()))
        resources.srcDir(layout.buildDirectory.dir("generated/nativeNotices"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareNativeNotices)
}

dependencies {
    api("androidx.media3:media3-decoder:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.annotation:annotation:1.9.1")
    compileOnly("org.checkerframework:checker-qual:3.49.0")
}