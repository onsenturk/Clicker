#!/usr/bin/env bash
set -euo pipefail

source_root="$(cd "$1" && pwd)"
ndk_root="$(cd "$2" && pwd)"
mkdir -p "$3"
output_root="$(cd "$3" && pwd)"
abi="$4"
media3_source_root="$(cd "$5" && pwd)"
toolchain="$(cygpath -m "$ndk_root/toolchains/llvm/prebuilt/windows-x86_64/bin")"

test "$(git -C "$source_root" rev-parse HEAD)" = c41ff724ede7da657762d61097e26fac296c53bf
test "$(git -C "$media3_source_root" rev-parse HEAD)" = 2bc207851df311340767e913931ca7b28cab1794
git -C "$source_root" diff --quiet HEAD
git -C "$media3_source_root" diff --quiet HEAD -- libraries/decoder_ffmpeg/src/main

case "$abi" in
    arm64-v8a)
        architecture=aarch64
        cpu=armv8-a
        target=aarch64-linux-android25
        extra_cflags=""
        ;;
    armeabi-v7a)
        architecture=arm
        cpu=armv7-a
        target=armv7a-linux-androideabi25
        extra_cflags="-march=armv7-a -mfloat-abi=softfp"
        ;;
    *) exit 2 ;;
esac

mkdir -p "$output_root/$abi"
cd "$output_root/$abi"
bash "$source_root/configure" \
    --target-os=android --arch="$architecture" --cpu="$cpu" \
    --enable-cross-compile --enable-static --disable-shared \
    --cc="$toolchain/clang.exe --target=$target" \
    --cxx="$toolchain/clang++.exe --target=$target" \
    --nm="$toolchain/llvm-nm.exe" --ar="$toolchain/llvm-ar.exe" \
    --ranlib="$toolchain/llvm-ranlib.exe" --strip="$toolchain/llvm-strip.exe" \
    --extra-cflags="-fPIC $extra_cflags" \
    --extra-ldflags="-Wl,-z,max-page-size=16384" \
    --disable-doc --disable-programs --disable-everything --disable-autodetect \
    --disable-avdevice --disable-avformat --disable-swscale --disable-postproc \
    --disable-avfilter --disable-symver --disable-v4l2-m2m --disable-vulkan \
    --disable-gpl --disable-nonfree --disable-version3 \
    --enable-swresample --enable-decoder=ac3,eac3,dca,mp2,mp3,truehd

"$ndk_root/prebuilt/windows-x86_64/bin/make.exe" -j4 SHELL=bash V=1 \
    -f "$(cygpath -m "$source_root")/Makefile" "SRC_PATH=$(cygpath -m "$source_root")" \
    libavcodec/libavcodec.a libavutil/libavutil.a libswresample/libswresample.a

mkdir -p "$output_root/jni/$abi"
"$toolchain/clang++.exe" --target="$target" -std=c++11 -O2 -fPIC -shared \
    -static-libstdc++ -I. -I"$source_root" \
    "$media3_source_root/libraries/decoder_ffmpeg/src/main/jni/ffmpeg_jni.cc" \
    -Wl,--no-undefined -Wl,-soname,libffmpegJNI.so -Wl,-z,max-page-size=16384 -Wl,-Bsymbolic \
    -Wl,--start-group libswresample/libswresample.a libavcodec/libavcodec.a libavutil/libavutil.a \
    -Wl,--end-group -landroid -llog -lm -o "$output_root/jni/$abi/libffmpegJNI.so"
"$toolchain/llvm-strip.exe" --strip-debug "$output_root/jni/$abi/libffmpegJNI.so"
"$toolchain/llvm-readelf.exe" --program-headers "$output_root/jni/$abi/libffmpegJNI.so"