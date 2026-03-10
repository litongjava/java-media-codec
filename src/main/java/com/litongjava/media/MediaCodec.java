package com.litongjava.media;

import java.nio.ByteBuffer;

public final class MediaCodec {
  public static final int CODEC_PCMU = 0;
  public static final int CODEC_PCMA = 8;
  public static final int CODEC_G722 = 9;

  static {
    MediaCodecLibraryUtils.load();
  }

  public static native long createEncoder(int codecType, int sampleRate, int channels, int bitrate, int options);

  public static native void destroyEncoder(long encoder);

  public static native int encodeDirect(long encoder, ByteBuffer pcm16le, int pcmSamples, ByteBuffer encodedOut);

  public static native long createDecoder(int codecType, int sampleRate, int channels, int bitrate, int options);

  public static native void destroyDecoder(long decoder);

  public static native int decodeDirect(long decoder, ByteBuffer encoded, int encodedLen, ByteBuffer pcm16leOut);

  public static native int getPcmSamplesPer20ms(int codecType, int sampleRate, int channels);

  public static native int getEncodedBytesPer20ms(int codecType, int sampleRate, int channels, int bitrate);
}