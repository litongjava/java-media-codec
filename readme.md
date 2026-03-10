# java-media-codec

[绑定的c项目](https://gitee.com/ppnt/c-media-codec)

## MediaCodec.java

```java
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
```

---

## Example: G.722 encode / decode

```java
package com.litongjava.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaCodecDemoG722 {

  public static void main(String[] args) {
    int codecType = MediaCodec.CODEC_G722;
    int sampleRate = 16000;
    int channels = 1;
    int bitrate = 64000;
    int options = 0;

    int pcmSamples = MediaCodec.getPcmSamplesPer20ms(codecType, sampleRate, channels);
    int encodedBytes = MediaCodec.getEncodedBytesPer20ms(codecType, sampleRate, channels, bitrate);

    if (pcmSamples <= 0 || encodedBytes <= 0) {
      throw new IllegalStateException("invalid frame size, pcmSamples=" + pcmSamples + ", encodedBytes=" + encodedBytes);
    }

    ByteBuffer pcmIn = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
    ByteBuffer encoded = ByteBuffer.allocateDirect(encodedBytes);
    ByteBuffer pcmOut = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);

    for (int i = 0; i < pcmSamples; i++) {
      short sample = (short) (Math.sin(i * 2.0 * Math.PI / 32.0) * 10000);
      pcmIn.putShort(i * 2, sample);
    }

    long encoder = MediaCodec.createEncoder(codecType, sampleRate, channels, bitrate, options);
    long decoder = MediaCodec.createDecoder(codecType, sampleRate, channels, bitrate, options);

    if (encoder == 0 || decoder == 0) {
      throw new IllegalStateException("create encoder/decoder failed");
    }

    try {
      int encLen = MediaCodec.encodeDirect(encoder, pcmIn, pcmSamples, encoded);
      System.out.println("G722 encoded bytes = " + encLen);

      if (encLen < 0) {
        throw new IllegalStateException("encode failed: " + encLen);
      }

      int decSamples = MediaCodec.decodeDirect(decoder, encoded, encLen, pcmOut);
      System.out.println("G722 decoded samples = " + decSamples);

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
```

---

## Example: PCMU encode / decode

```java
package com.litongjava.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaCodecDemoPcmu {

  public static void main(String[] args) {
    int codecType = MediaCodec.CODEC_PCMU;
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
      short sample = (short) ((i % 40 - 20) * 500);
      pcmIn.putShort(i * 2, sample);
    }

    long encoder = MediaCodec.createEncoder(codecType, sampleRate, channels, bitrate, options);
    long decoder = MediaCodec.createDecoder(codecType, sampleRate, channels, bitrate, options);

    if (encoder == 0 || decoder == 0) {
      throw new IllegalStateException("create encoder/decoder failed");
    }

    try {
      int encLen = MediaCodec.encodeDirect(encoder, pcmIn, pcmSamples, encoded);
      System.out.println("PCMU encoded bytes = " + encLen);

      if (encLen < 0) {
        throw new IllegalStateException("encode failed: " + encLen);
      }

      int decSamples = MediaCodec.decodeDirect(decoder, encoded, encLen, pcmOut);
      System.out.println("PCMU decoded samples = " + decSamples);

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
```

---

## Example: PCMA encode / decode

```java
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
```

---

## Notes

### 1. 必须使用 DirectByteBuffer

正确：

```java
ByteBuffer.allocateDirect(size)
```

不要用：

```java
ByteBuffer.wrap(new byte[size])
```

---

### 2. PCM 必须是 little-endian 16-bit

建议：

```java
ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
```

---

### 3. G711 参数

* `sampleRate = 8000`
* `channels = 1`
* `bitrate = 0`
* `options = 0`

---

### 4. G722 参数

常规宽带模式：

* `sampleRate = 16000`
* `channels = 1`
* `bitrate = 64000 / 56000 / 48000`
* `options = 0`

---

### 5. 返回值含义

* `encodeDirect(...)` 返回编码后的字节数
* `decodeDirect(...)` 返回解码后的 PCM sample 数
* 返回负数表示错误

