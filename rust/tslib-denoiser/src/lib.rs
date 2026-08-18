#![allow(non_snake_case)]

use std::collections::VecDeque;

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jlong};
use jni::JNIEnv;

use sherpa_onnx::{
    OfflineSpeechDenoiserDpdfNetModelConfig, OfflineSpeechDenoiserModelConfig,
    OnlineSpeechDenoiser, OnlineSpeechDenoiserConfig,
};

/// Streaming is handled inside this library now: input frames are split into
/// the model's preferred frame shift and produced audio is queued so every
/// `process()` call returns exactly the number of samples it received.
const ENABLE_ACTUAL_DENOISE: bool = true;

pub struct AudioDenoiserHandle {
    denoiser: Option<OnlineSpeechDenoiser>,
    input_queue: VecDeque<f32>,
    output_queue: VecDeque<i16>,
    frame_shift: usize,
    started: bool,
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

    Box::into_raw(Box::new(AudioDenoiserHandle {
        denoiser,
        input_queue: VecDeque::new(),
        output_queue: VecDeque::new(),
        frame_shift: 0,
        started: false,
    })) as jlong
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

    // Actual denoise path with streaming buffering.
    if pcm_bytes.len() % 2 != 0 {
        throw_runtime(&mut env, "PCM data must have even length (16-bit samples)");
        return std::ptr::null_mut();
    }

    let samples_i16: Vec<i16> = pcm_bytes
        .chunks_exact(2)
        .map(|chunk| i16::from_le_bytes([chunk[0], chunk[1]]))
        .collect();
    let frame_len = samples_i16.len();
    if frame_len == 0 {
        return env
            .byte_array_from_slice(&pcm_bytes)
            .map(|array| array.into_raw())
            .unwrap_or(std::ptr::null_mut());
    }

    let AudioDenoiserHandle {
        denoiser,
        input_queue,
        output_queue,
        frame_shift,
        started,
    } = handle;
    let denoiser = denoiser.as_ref().expect("checked above");
    let sample_rate = denoiser.sample_rate().max(8_000);
    let shift = if *frame_shift > 0 {
        *frame_shift
    } else {
        let shift = denoiser.frame_shift_in_samples().max(1) as usize;
        *frame_shift = shift;
        shift
    };

    for sample in &samples_i16 {
        input_queue.push_back(f32::from(*sample) / 32768.0);
    }

    while input_queue.len() >= shift {
        let chunk: Vec<f32> = input_queue.drain(..shift).collect();
        let output = denoiser.run(&chunk, sample_rate);
        for sample in output.samples {
            let sample = (sample * 32768.0).clamp(-32768.0, 32767.0) as i16;
            output_queue.push_back(sample);
        }
    }

    if output_queue.len() >= frame_len {
        if !*started {
            *started = true;
            log::info!(
                "denoiser streaming started: frame={frame_len} samples, model_shift={shift}, sample_rate={sample_rate}"
            );
        }
        let output_bytes: Vec<u8> = output_queue
            .drain(..frame_len)
            .flat_map(i16::to_le_bytes)
            .collect();
        return env
            .byte_array_from_slice(&output_bytes)
            .map(|array| array.into_raw())
            .unwrap_or(std::ptr::null_mut());
    }

    // Model lookahead has not produced audio yet; return the original frame
    // so the caller always receives its expected frame size.
    env.byte_array_from_slice(&pcm_bytes)
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
