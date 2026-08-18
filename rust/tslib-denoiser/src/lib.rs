#![allow(non_snake_case)]

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jlong};
use jni::JNIEnv;

use sherpa_onnx::{
    OfflineSpeechDenoiserDpdfNetModelConfig, OfflineSpeechDenoiserModelConfig,
    OnlineSpeechDenoiser, OnlineSpeechDenoiserConfig,
};

/// Set to `true` once the streaming-output buffering in the Kotlin layer is
/// ready. Until then the JNI path returns the input PCM untouched so we can
/// prove that create/process/destroy and the asset-copy pipeline are stable.
const ENABLE_ACTUAL_DENOISE: bool = false;

pub struct AudioDenoiserHandle {
    denoiser: Option<OnlineSpeechDenoiser>,
}

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(_vm: jni::JavaVM, _reserved: *mut std::ffi::c_void) -> jint {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("tslib-denoiser"),
    );
    log::info!("tslib-denoiser loaded");
    jni::sys::JNI_VERSION_1_6
}

fn ptr_to_denoiser(ptr: jlong) -> &'static mut AudioDenoiserHandle {
    debug_assert_ne!(ptr, 0, "AudioDenoiser native pointer must not be null");
    unsafe { &mut *(ptr as *mut AudioDenoiserHandle) }
}

fn throw_runtime(env: &mut JNIEnv, message: &str) {
    let _ = env.throw_new("java/lang/RuntimeException", message);
}

fn create_denoiser(model_path: &str) -> Option<OnlineSpeechDenoiser> {
    let config = OnlineSpeechDenoiserConfig {
        model: OfflineSpeechDenoiserModelConfig {
            dpdfnet: OfflineSpeechDenoiserDpdfNetModelConfig {
                model: Some(model_path.to_string()),
                attenuation_limit_db: 0.0,
            },
            num_threads: 1,
            debug: false,
            provider: Some("cpu".to_string()),
            ..Default::default()
        },
    };

    match OnlineSpeechDenoiser::create(&config) {
        Some(denoiser) => {
            log::info!(
                "sherpa-onnx denoiser created (sample_rate={}, frame_shift={})",
                denoiser.sample_rate(),
                denoiser.frame_shift_in_samples()
            );
            Some(denoiser)
        }
        None => {
            log::warn!("sherpa-onnx failed to create denoiser from {model_path}; bypassing");
            None
        }
    }
}

/// `AudioDenoiser.nativeCreate(modelPath, enabled)`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_tslib_AudioDenoiser_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    model_path: JString,
    enabled: jboolean,
) -> jlong {
    let enabled = enabled != 0;
    let denoiser = if enabled {
        if model_path.is_null() {
            log::warn!("AudioDenoiser enabled with null model path; bypassing");
            None
        } else {
            let path: Option<String> = env.get_string(&model_path).ok().map(Into::into);
            match path {
                Some(path) if !path.is_empty() => create_denoiser(&path),
                _ => {
                    log::warn!("AudioDenoiser enabled without a valid model path; bypassing");
                    None
                }
            }
        }
    } else {
        None
    };

    Box::into_raw(Box::new(AudioDenoiserHandle { denoiser })) as jlong
}

/// `AudioDenoiser.nativeProcess(ptr, pcm)` — denoise 16-bit LE PCM.
#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_tslib_AudioDenoiser_nativeProcess<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    ptr: jlong,
    pcm: JByteArray<'local>,
) -> jbyteArray {
    let pcm_bytes = match env.convert_byte_array(&pcm) {
        Ok(bytes) => bytes,
        Err(e) => {
            throw_runtime(&mut env, &format!("Failed to read PCM array: {e}"));
            return std::ptr::null_mut();
        }
    };

    let handle = ptr_to_denoiser(ptr);

    if !(ENABLE_ACTUAL_DENOISE && handle.denoiser.is_some()) {
        return env
            .byte_array_from_slice(&pcm_bytes)
            .map(|array| array.into_raw())
            .unwrap_or(std::ptr::null_mut());
    }

    // Actual denoise path (disabled until Kotlin buffering is ready).
    if pcm_bytes.len() % 2 != 0 {
        throw_runtime(&mut env, "PCM data must have even length (16-bit samples)");
        return std::ptr::null_mut();
    }

    let samples_i16: Vec<i16> = pcm_bytes
        .chunks_exact(2)
        .map(|chunk| i16::from_le_bytes([chunk[0], chunk[1]]))
        .collect();
    let samples_f32: Vec<f32> = samples_i16
        .iter()
        .map(|sample| f32::from(*sample) / 32768.0)
        .collect();

    let denoiser = handle.denoiser.as_ref().expect("checked above");
    let output = denoiser.run(&samples_f32, 48_000);

    if output.samples.is_empty() {
        return env
            .byte_array_from_slice(&pcm_bytes)
            .map(|array| array.into_raw())
            .unwrap_or(std::ptr::null_mut());
    }

    let output_bytes: Vec<u8> = output
        .samples
        .iter()
        .map(|sample| (sample * 32768.0).clamp(-32768.0, 32767.0) as i16)
        .flat_map(i16::to_le_bytes)
        .collect();

    env.byte_array_from_slice(&output_bytes)
        .map(|array| array.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

/// `AudioDenoiser.nativeDestroy(ptr)`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_tslib_AudioDenoiser_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    if ptr != 0 {
        unsafe {
            drop(Box::from_raw(ptr as *mut AudioDenoiserHandle));
        }
    }
}
