package dev.tslib;

/**
 * JNI wrapper around the Rust sherpa-onnx speech denoiser.
 *
 * <p>The handle is intentionally fail-open: if the model cannot be loaded the
 * constructor still succeeds and {@link #process(byte[])} returns the input
 * PCM unchanged, so a missing/corrupt model can never crash an active call.
 *
 * <p>PCM is 16-bit little-endian mono at 48 kHz, matching
 * {@code AudioBridge}'s 20 ms frames (1920 bytes / 960 samples).
 */
public class AudioDenoiser implements AutoCloseable {
    private long nativePtr;

    /**
     * @param modelPath absolute path to the DPDFNet ONNX model, may be null
     *                  when {@code enabled} is false
     * @param enabled   whether to load and prepare the model
     */
    public AudioDenoiser(String modelPath, boolean enabled) {
        this.nativePtr = nativeCreate(modelPath, enabled);
        if (this.nativePtr == 0) {
            throw new TsLibException("Failed to create AudioDenoiser");
        }
    }

    /**
     * Denoise one PCM frame. Returns a byte array of the same length as the
     * input while the streaming buffer is synchronizing; never returns null
     * for a non-empty input.
     */
    public byte[] process(byte[] pcm) {
        checkNotClosed();
        if (pcm == null || pcm.length == 0) {
            return pcm == null ? new byte[0] : pcm;
        }
        return nativeProcess(nativePtr, pcm);
    }

    @Override
    public void close() {
        if (nativePtr != 0) {
            nativeDestroy(nativePtr);
            nativePtr = 0;
        }
    }

    private void checkNotClosed() {
        if (nativePtr == 0) {
            throw new IllegalStateException("AudioDenoiser has been closed");
        }
    }

    // Native methods
    private static native long nativeCreate(String modelPath, boolean enabled);
    private static native byte[] nativeProcess(long ptr, byte[] pcm);
    private static native void nativeDestroy(long ptr);
}
