# java-media-codec

`java-media-codec` 是一个 **Java JNI 音频编解码库**，提供以下音频 codec：

* **G.711 PCMU (μ-law)**
* **G.711 PCMA (A-law)**
* **G.722 wideband**

该项目通过 JNI 调用纯 C 实现的 codec（见项目绑定的 C 实现）。

C 实现项目：

**c-media-codec**

[https://gitee.com/ppnt/c-media-codec](https://gitee.com/ppnt/c-media-codec)

---

# Features

* 支持 **G711 / G722**

* **JNI DirectByteBuffer 零拷贝**

* **低延迟音频处理**

* 适用于：

  * RTP
  * VoIP
  * SIP
  * Media Server
  * Audio Gateway

* 纯 C codec 实现

* 无额外第三方依赖

---

# Codec Support

| Codec | RTP Payload Type | Sample Rate                    | Bitrate         |
| ----- | ---------------- | ------------------------------ | --------------- |
| PCMU  | 0                | 8000                           | 64kbps          |
| PCMA  | 8                | 8000                           | 64kbps          |
| G722  | 9                | 16000 (PCM) / 8000 (RTP clock) | 64k / 56k / 48k |

---

# MediaCodec.java

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

# Example: G.722 Encode / Decode

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

    ByteBuffer pcmIn = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
    ByteBuffer encoded = ByteBuffer.allocateDirect(encodedBytes);
    ByteBuffer pcmOut = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);

    for (int i = 0; i < pcmSamples; i++) {
      short sample = (short) (Math.sin(i * 2.0 * Math.PI / 32.0) * 10000);
      pcmIn.putShort(i * 2, sample);
    }

    long encoder = MediaCodec.createEncoder(codecType, sampleRate, channels, bitrate, options);
    long decoder = MediaCodec.createDecoder(codecType, sampleRate, channels, bitrate, options);

    try {

      int encLen = MediaCodec.encodeDirect(encoder, pcmIn, pcmSamples, encoded);
      System.out.println("encoded bytes = " + encLen);

      int decSamples = MediaCodec.decodeDirect(decoder, encoded, encLen, pcmOut);
      System.out.println("decoded samples = " + decSamples);

    } finally {
      MediaCodec.destroyEncoder(encoder);
      MediaCodec.destroyDecoder(decoder);
    }
  }
}
```

---

# Example: PCMU Encode / Decode

```java
int codecType = MediaCodec.CODEC_PCMU;
int sampleRate = 8000;
int channels = 1;
int bitrate = 0;
int options = 0;
```

调用方式与 G722 相同。

---

# Example: PCMA Encode / Decode

```java
int codecType = MediaCodec.CODEC_PCMA;
int sampleRate = 8000;
int channels = 1;
int bitrate = 0;
int options = 0;
```

调用方式与 PCMU 相同。

---

# Frame Size Reference (20ms)

| Codec    | PCM Samples | PCM Bytes | Encoded Bytes |
| -------- | ----------- | --------- | ------------- |
| PCMU     | 160         | 320       | 160           |
| PCMA     | 160         | 320       | 160           |
| G722 64k | 320         | 640       | 160           |
| G722 56k | 320         | 640       | 140           |
| G722 48k | 320         | 640       | 120           |

---

# Usage Notes

## 1 必须使用 DirectByteBuffer

正确：

```java
ByteBuffer.allocateDirect(size)
```

错误：

```java
ByteBuffer.wrap(byte[])
```

JNI 使用：

```
GetDirectBufferAddress()
```

实现 **zero-copy 音频处理**。

---

## 2 PCM Format

必须使用：

```
16-bit signed PCM
little-endian
mono
```

推荐：

```java
ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
```

---

## 3 G711 Parameters

```
sampleRate = 8000
channels   = 1
bitrate    = 0
options    = 0
```

---

## 4 G722 Parameters

```
sampleRate = 16000
channels   = 1
bitrate    = 64000 / 56000 / 48000
options    = 0
```

---

## 5 Return Values

| Method         | Return                   |
| -------------- | ------------------------ |
| encodeDirect   | encoded byte count       |
| decodeDirect   | decoded PCM sample count |
| negative value | error                    |

---

# Related Project

C codec implementation:

```
c-media-codec
```

[https://gitee.com/ppnt/c-media-codec](https://gitee.com/ppnt/c-media-codec)

---

# License

本项目 codec 实现来自：

**WebRTC third_party codec**

遵循 WebRTC 开源协议。