package com.litongjava.media;

import java.nio.ByteBuffer;

/**
 * JNI audio codec and resampler bridge.
 *
 * <p>This class provides native access to:
 *
 * <ul>
 *   <li>G.711 PCMU (mu-law)</li>
 *   <li>G.711 PCMA (A-law)</li>
 *   <li>G.722 wideband codec</li>
 *   <li>PCM resampling</li>
 * </ul>
 *
 * <p>All PCM buffers passed to native methods must use 16-bit signed PCM in little-endian byte order.
 *
 * <p>For best performance, all input and output buffers should be allocated with
 * {@link java.nio.ByteBuffer#allocateDirect(int)} so JNI can access them without extra copying.
 */
public final class MediaCodec {

  /** RTP payload type / codec id for G.711 PCMU (mu-law). */
  public static final int CODEC_PCMU = 0;

  /** RTP payload type / codec id for G.711 PCMA (A-law). */
  public static final int CODEC_PCMA = 8;

  /** RTP payload type / codec id for G.722 wideband audio. */
  public static final int CODEC_G722 = 9;

  static {
    MediaCodecLibraryUtils.load();
  }
  
  /**
   * Initializes and loads the library
   */
  public static void init() {

  }

  // ---------------------------------------------------------------------------
  // Codec
  // ---------------------------------------------------------------------------

  /**
   * Creates a native encoder instance.
   *
   * <p>Typical parameters:
   *
   * <ul>
   *   <li>PCMU: sampleRate=8000, channels=1, bitrate=0</li>
   *   <li>PCMA: sampleRate=8000, channels=1, bitrate=0</li>
   *   <li>G722: sampleRate=16000, channels=1, bitrate=64000/56000/48000</li>
   * </ul>
   *
   * @param codecType codec type, such as {@link #CODEC_PCMU}, {@link #CODEC_PCMA}, or {@link #CODEC_G722}
   * @param sampleRate input PCM sample rate in Hz
   * @param channels channel count, currently mono is recommended
   * @param bitrate codec bitrate in bits per second; ignored for G.711
   * @param options reserved for future use, currently pass 0
   * @return native encoder handle, or {@code <= 0} if creation fails
   */
  public static native long createEncoder(int codecType, int sampleRate, int channels, int bitrate, int options);

  /**
   * Destroys a native encoder instance created by {@link #createEncoder(int, int, int, int, int)}.
   *
   * @param encoder native encoder handle
   */
  public static native void destroyEncoder(long encoder);

  /**
   * Encodes 16-bit PCM samples into codec payload bytes.
   *
   * <p>Input requirements:
   *
   * <ul>
   *   <li>{@code pcm16le} must be a direct buffer</li>
   *   <li>PCM format must be 16-bit signed little-endian</li>
   *   <li>{@code pcmSamples} is the number of PCM samples, not the number of bytes</li>
   * </ul>
   *
   * <p>Output requirements:
   *
   * <ul>
   *   <li>{@code encodedOut} must be a direct buffer</li>
   *   <li>Buffer capacity must be large enough for the encoded frame</li>
   * </ul>
   *
   * @param encoder native encoder handle
   * @param pcm16le input PCM buffer in 16-bit signed little-endian format
   * @param pcmSamples number of input PCM samples
   * @param encodedOut output buffer for encoded bytes
   * @return encoded byte count, or a negative value on error
   */
  public static native int encodeDirect(long encoder, ByteBuffer pcm16le, int pcmSamples, ByteBuffer encodedOut);

  /**
   * Creates a native decoder instance.
   *
   * <p>Typical parameters should match the encoder side:
   *
   * <ul>
   *   <li>PCMU: sampleRate=8000, channels=1, bitrate=0</li>
   *   <li>PCMA: sampleRate=8000, channels=1, bitrate=0</li>
   *   <li>G722: sampleRate=16000, channels=1, bitrate=64000/56000/48000</li>
   * </ul>
   *
   * @param codecType codec type, such as {@link #CODEC_PCMU}, {@link #CODEC_PCMA}, or {@link #CODEC_G722}
   * @param sampleRate output PCM sample rate in Hz
   * @param channels channel count, currently mono is recommended
   * @param bitrate codec bitrate in bits per second; ignored for G.711
   * @param options reserved for future use, currently pass 0
   * @return native decoder handle, or {@code <= 0} if creation fails
   */
  public static native long createDecoder(int codecType, int sampleRate, int channels, int bitrate, int options);

  /**
   * Destroys a native decoder instance created by {@link #createDecoder(int, int, int, int, int)}.
   *
   * @param decoder native decoder handle
   */
  public static native void destroyDecoder(long decoder);

  /**
   * Decodes codec payload bytes into 16-bit PCM samples.
   *
   * <p>Input requirements:
   *
   * <ul>
   *   <li>{@code encoded} must be a direct buffer</li>
   *   <li>{@code encodedLen} is the number of valid encoded bytes to decode</li>
   * </ul>
   *
   * <p>Output requirements:
   *
   * <ul>
   *   <li>{@code pcm16leOut} must be a direct buffer</li>
   *   <li>PCM format is 16-bit signed little-endian</li>
   *   <li>Buffer capacity must be large enough for decoded PCM data</li>
   * </ul>
   *
   * @param decoder native decoder handle
   * @param encoded input buffer containing encoded bytes
   * @param encodedLen number of encoded bytes to decode
   * @param pcm16leOut output PCM buffer in 16-bit signed little-endian format
   * @return decoded PCM sample count, or a negative value on error
   */
  public static native int decodeDirect(long decoder, ByteBuffer encoded, int encodedLen, ByteBuffer pcm16leOut);

  /**
   * Returns the PCM sample count for a 20 ms frame for the given codec configuration.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>PCMU / PCMA at 8000 Hz mono -> 160 samples</li>
   *   <li>G722 at 16000 Hz mono -> 320 samples</li>
   * </ul>
   *
   * @param codecType codec type
   * @param sampleRate PCM sample rate in Hz
   * @param channels channel count
   * @return PCM samples per 20 ms frame, or a negative / non-positive value if unsupported
   */
  public static native int getPcmSamplesPer20ms(int codecType, int sampleRate, int channels);

  /**
   * Returns the encoded payload size in bytes for a 20 ms frame.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>PCMU / PCMA -> 160 bytes</li>
   *   <li>G722 64k -> 160 bytes</li>
   *   <li>G722 56k -> 140 bytes</li>
   *   <li>G722 48k -> 120 bytes</li>
   * </ul>
   *
   * @param codecType codec type
   * @param sampleRate PCM sample rate in Hz
   * @param channels channel count
   * @param bitrate codec bitrate in bits per second
   * @return encoded bytes per 20 ms frame, or a negative / non-positive value if unsupported
   */
  public static native int getEncodedBytesPer20ms(int codecType, int sampleRate, int channels, int bitrate);

  // ---------------------------------------------------------------------------
  // Resampler
  // ---------------------------------------------------------------------------

  /**
   * Creates a native PCM resampler.
   *
   * <p>The resampler is intended for streaming use cases where input PCM sample rate and output PCM sample rate
   * are different.
   *
   * <p>Recommended values:
   *
   * <ul>
   *   <li>{@code channels = 1}</li>
   *   <li>{@code quality = 5} or {@code 6}</li>
   *   <li>{@code options = 0}</li>
   * </ul>
   *
   * @param channels channel count, currently mono is recommended
   * @param inputRate input PCM sample rate in Hz, for example 24000
   * @param outputRate output PCM sample rate in Hz, for example 8000 or 16000
   * @param quality SpeexDSP resampling quality level
   * @param options reserved for future use, currently pass 0
   * @return native resampler handle, or {@code <= 0} if creation fails
   */
  public static native long createResampler(int channels, int inputRate, int outputRate, int quality, int options);

  /**
   * Destroys a native resampler instance created by {@link #createResampler(int, int, int, int, int)}.
   *
   * @param resampler native resampler handle
   */
  public static native void destroyResampler(long resampler);

  /**
   * Performs streaming PCM resampling.
   *
   * <p>Both input and output buffers must be direct buffers.
   *
   * <p>Input format:
   *
   * <ul>
   *   <li>16-bit signed PCM</li>
   *   <li>little-endian</li>
   * </ul>
   *
   * <p>Output format:
   *
   * <ul>
   *   <li>16-bit signed PCM</li>
   *   <li>little-endian</li>
   * </ul>
   *
   * <p>{@code inputSamplesPerChannel} is measured per channel, not total interleaved sample count across all channels.
   *
   * @param resampler native resampler handle
   * @param pcm16leIn input PCM buffer
   * @param inputSamplesPerChannel number of input samples per channel
   * @param pcm16leOut output PCM buffer
   * @return output sample count per channel, or a negative value on error
   */
  public static native int resampleDirect(long resampler, ByteBuffer pcm16leIn, int inputSamplesPerChannel,
      ByteBuffer pcm16leOut);

  /**
   * Resets the internal state of the resampler.
   *
   * <p>This is useful when the input stream is discontinuous or when you want to clear buffered state between
   * unrelated audio segments.
   *
   * @param resampler native resampler handle
   * @return 0 on success, or a negative value on error
   */
  public static native int resetResampler(long resampler);

  /**
   * Updates the input and output sample rates of an existing resampler.
   *
   * @param resampler native resampler handle
   * @param inputRate new input PCM sample rate in Hz
   * @param outputRate new output PCM sample rate in Hz
   * @return 0 on success, or a negative value on error
   */
  public static native int setResamplerRate(long resampler, int inputRate, int outputRate);

  /**
   * Returns the theoretical maximum output sample count per channel for a given input sample count.
   *
   * <p>This method is useful when preallocating the destination direct buffer on the Java side.
   *
   * @param resampler native resampler handle
   * @param inputSamplesPerChannel input sample count per channel
   * @return expected maximum output sample count per channel, or a negative / non-positive value on error
   */
  public static native int getResamplerExpectedOutputSamples(long resampler, int inputSamplesPerChannel);
}