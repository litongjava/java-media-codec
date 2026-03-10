package com.litongjava.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaCodecDemoPcma {

  public static void main(String[] args) {
    int codecType = MediaCodec.CODEC_PCMA;
    int sampleRate = 8000;
    int channels = 1;
    int bitrate = 0;
    int options = 0;

    int pcmSamples = MediaCodec.getPcmSamplesPer20ms(codecType, sampleRate, channels);
    int encodedBytes = MediaCodec.getEncodedBytesPer20ms(codecType, sampleRate, channels, bitrate);

    ByteBuffer pcmIn = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
    ByteBuffer encoded = ByteBuffer.allocateDirect(encodedBytes);
    ByteBuffer pcmOut = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);

    for (int i = 0; i < pcmSamples; i++) {
      short sample = (short) ((i % 50 - 25) * 400);
      pcmIn.putShort(i * 2, sample);
    }

    long encoder = MediaCodec.createEncoder(codecType, sampleRate, channels, bitrate, options);
    long decoder = MediaCodec.createDecoder(codecType, sampleRate, channels, bitrate, options);

    if (encoder == 0 || decoder == 0) {
      throw new IllegalStateException("create encoder/decoder failed");
    }

    try {
      int encLen = MediaCodec.encodeDirect(encoder, pcmIn, pcmSamples, encoded);
      System.out.println("PCMA encoded bytes = " + encLen);

      if (encLen < 0) {
        throw new IllegalStateException("encode failed: " + encLen);
      }

      int decSamples = MediaCodec.decodeDirect(decoder, encoded, encLen, pcmOut);
      System.out.println("PCMA decoded samples = " + decSamples);

      if (decSamples < 0) {
        throw new IllegalStateException("decode failed: " + decSamples);
      }

      for (int i = 0; i < Math.min(decSamples, 10); i++) {
        short value = pcmOut.getShort(i * 2);
        System.out.println("pcmOut[" + i + "] = " + value);
      }
    } finally {
      MediaCodec.destroyEncoder(encoder);
      MediaCodec.destroyDecoder(decoder);
    }
  }
}