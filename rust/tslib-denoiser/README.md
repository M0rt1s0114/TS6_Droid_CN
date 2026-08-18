# tslib-denoiser

Standalone JNI library that wraps [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
speech denoising (DPDFNet 48 kHz model) for TS6_Droid_CN.

Kotlin side: `dev.tslib.AudioDenoiser`.

## Pass-through first

`ENABLE_ACTUAL_DENOISE` in `src/lib.rs` is currently `false`; the JNI layer
returns the input PCM untouched. Flip it after the Kotlin streaming buffer is
validated on device.

## Build (Windows)

Prereqs: Rust Android targets, cargo-ndk, Android NDK.

```powershell
rustup target add aarch64-linux-android x86_64-linux-android
cargo install cargo-ndk

# sherpa-onnx Android prebuilt libs (the crate's automatic download expects a
# different archive layout, so we point SHERPA_ONNX_LIB_DIR at the unpacked ABI)
# Download once from:
# https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.5/sherpa-onnx-v1.13.5-android.tar.bz2
# Extract jniLibs/<abi>/ where convenient, then:

$env:ANDROID_NDK_HOME = "$env:LOCALAPPDATA\Android\Sdk\ndk\<ndk-version>"
$env:ANDROID_NDK = $env:ANDROID_NDK_HOME
$env:SHERPA_ONNX_LIB_DIR = "...\sherpa-onnx-v1.13.5-android\jniLibs\arm64-v8a"
cargo ndk -t arm64-v8a -o ..\..\app\src\main\jniLibs build --release

$env:SHERPA_ONNX_LIB_DIR = "...\sherpa-onnx-v1.13.5-android\jniLibs\x86_64"
cargo ndk -t x86_64 -o ..\..\app\src\main\jniLibs build --release
```

Then copy the two runtime libraries into the same ABI folders:

```powershell
# From <rust>\target\aarch64-linux-android\release\
libsherpa-onnx-c-api.so, libonnxruntime.so  -> app\src\main\jniLibs\arm64-v8a\
# From <rust>\target\x86_64-linux-android\release\
libsherpa-onnx-c-api.so, libonnxruntime.so  -> app\src\main\jniLibs\x86_64\
```

The model lives in `app/src/main/assets/denoiser/` and is copied to
`filesDir/denoiser/` on first capture.
