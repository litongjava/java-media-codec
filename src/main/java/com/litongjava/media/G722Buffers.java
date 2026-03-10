package com.litongjava.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class G722Buffers {
  public static final int PCM_SAMPLE_RATE = 16000;
  public static final int RTP_CLOCK_RATE = 8000;
  public static final int FRAME_MS = 20;

  public static ByteBuffer newPcm20msBuffer() {
    return ByteBuffer.allocateDirect(320 * 2).order(ByteOrder.LITTLE_ENDIAN);
  }

  public static ByteBuffer newG72220msBuffer(int bitrate) {
    int bytes = bitrate / 8 * FRAME_MS / 1000;
    return ByteBuffer.allocateDirect(bytes);
  }
}