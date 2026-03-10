# java-media-codec

`java-media-codec` 是一个面向实时音频场景的 **Java JNI 音频编解码与重采样库**。

它通过 JNI 调用底层纯 C 实现，提供以下能力：

* **G.711 PCMU (μ-law)**
* **G.711 PCMA (A-law)**
* **G.722 wideband codec**
* **PCM 重采样（Resampler）**

适用于：

* **RTP**
* **VoIP**
* **SIP**
* **Media Server**
* **Audio Gateway**
* **WebRTC 相关音频处理链路**

---

# Features

* 支持 **G.711 / G.722 音频编解码**
* 支持 **PCM 重采样**
* JNI `DirectByteBuffer` **零拷贝**
* **低延迟**、适合实时流式处理
* Java 调用简单，底层为纯 C 实现
* 无额外 Java 音频编解码依赖
* 可用于 **编码 / 解码 / 重采样 / RTP 音频桥接**

---

# Related Native Project

本项目绑定的底层 C 实现项目：

**c-media-codec**

* Gitee: [https://gitee.com/ppnt/c-media-codec](https://gitee.com/ppnt/c-media-codec)

---

# JNI Header Generation

如果需要重新生成 JNI 头文件，可使用 JDK 自带 `javah`：

```bash
D:\java\jdk1.8.0_121\bin\javah -J-Dfile.encoding=UTF-8 -d jni -classpath src\main\java com.litongjava.media.MediaCodec
```

说明：

* 输出目录：`jni`
* 目标类：`com.litongjava.media.MediaCodec`

---

# Codec Support

| Codec | RTP Payload Type | PCM Sample Rate | RTP Clock Rate |         Bitrate |
| ----- | ---------------: | --------------: | -------------: | --------------: |
| PCMU  |                0 |            8000 |           8000 |         64 kbps |
| PCMA  |                8 |            8000 |           8000 |         64 kbps |
| G722  |                9 |           16000 |           8000 | 64k / 56k / 48k |

说明：

* `PCMU / PCMA` 为窄带 8kHz
* `G722` 的 PCM 输入采样率通常为 `16000`
* `G722` 在 RTP 中的 clock rate 按标准习惯使用 `8000`

---

# API Overview

## Codec API

用于创建编码器 / 解码器并执行 PCM 与编码数据之间的转换：

* `createEncoder(...)`
* `destroyEncoder(...)`
* `encodeDirect(...)`
* `createDecoder(...)`
* `destroyDecoder(...)`
* `decodeDirect(...)`
* `getPcmSamplesPer20ms(...)`
* `getEncodedBytesPer20ms(...)`

---

## Resampler API

用于 PCM 采样率转换：

* `createResampler(...)`
* `destroyResampler(...)`
* `resampleDirect(...)`
* `resetResampler(...)`
* `setResamplerRate(...)`
* `getResamplerExpectedOutputSamples(...)`

---

# Quick Start

## 1. 创建 G.722 Encoder / Decoder

```java
int codecType = MediaCodec.CODEC_G722;
int sampleRate = 16000;
int channels = 1;
int bitrate = 64000;
int options = 0;

long encoder = MediaCodec.createEncoder(codecType, sampleRate, channels, bitrate, options);
long decoder = MediaCodec.createDecoder(codecType, sampleRate, channels, bitrate, options);
```

---

## 2. 创建 DirectByteBuffer

```java
int pcmSamples = MediaCodec.getPcmSamplesPer20ms(codecType, sampleRate, channels);
int encodedBytes = MediaCodec.getEncodedBytesPer20ms(codecType, sampleRate, channels, bitrate);

ByteBuffer pcmIn = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
ByteBuffer encoded = ByteBuffer.allocateDirect(encodedBytes);
ByteBuffer pcmOut = ByteBuffer.allocateDirect(pcmSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
```

---

## 3. 执行编码 / 解码

```java
int encLen = MediaCodec.encodeDirect(encoder, pcmIn, pcmSamples, encoded);
int decSamples = MediaCodec.decodeDirect(decoder, encoded, encLen, pcmOut);
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

# Example: PCMU Encode / Decode

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

# Example: PCMA Encode / Decode

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

# Example: Resampler

下面给出一个完整的重采样示例，演示：

* 创建重采样器
* 申请输入 / 输出 `DirectByteBuffer`
* 执行 `24000 -> 8000` 重采样
* 销毁 native resampler

```java
package com.litongjava.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaResamplerDemo {

  public static void main(String[] args) {
    int channels = 1;
    int inputRate = 24000;
    int outputRate = 8000;
    int quality = 5;
    int options = 0;

    long resampler = MediaCodec.createResampler(channels, inputRate, outputRate, quality, options);
    if (resampler <= 0) {
      throw new IllegalStateException("create resampler failed");
    }

    int inputSamplesPerChannel = 480;
    int expectedOutSamples = MediaCodec.getResamplerExpectedOutputSamples(resampler, inputSamplesPerChannel);

    if (expectedOutSamples <= 0) {
      throw new IllegalStateException("invalid expected output samples: " + expectedOutSamples);
    }

    ByteBuffer pcmIn = ByteBuffer.allocateDirect(inputSamplesPerChannel * 2).order(ByteOrder.LITTLE_ENDIAN);
    ByteBuffer pcmOut = ByteBuffer.allocateDirect(expectedOutSamples * 2).order(ByteOrder.LITTLE_ENDIAN);

    for (int i = 0; i < inputSamplesPerChannel; i++) {
      short sample = (short) (Math.sin(i * 2.0 * Math.PI / 48.0) * 10000);
      pcmIn.putShort(i * 2, sample);
    }

    try {
      int outSamples = MediaCodec.resampleDirect(resampler, pcmIn, inputSamplesPerChannel, pcmOut);
      System.out.println("resampled output samples = " + outSamples);

      if (outSamples < 0) {
        throw new IllegalStateException("resample failed: " + outSamples);
      }

      for (int i = 0; i < Math.min(outSamples, 10); i++) {
        short value = pcmOut.getShort(i * 2);
        System.out.println("pcmOut[" + i + "] = " + value);
      }
    } finally {
      MediaCodec.destroyResampler(resampler);
    }
  }
}
```

---

# Codec Parameters

## G711

| 参数         | 值          |
| ---------- | ---------- |
| sampleRate | 8000       |
| channels   | 1          |
| bitrate    | ignore / 0 |
| options    | 0          |

20ms 帧：

```text
PCM samples = 160
encoded bytes = 160
```

---

## G722

| 参数         | 值                     |
| ---------- | --------------------- |
| sampleRate | 16000                 |
| channels   | 1                     |
| bitrate    | 64000 / 56000 / 48000 |
| options    | 0                     |

20ms 帧：

| bitrate | encoded bytes |
| ------: | ------------: |
|   64000 |           160 |
|   56000 |           140 |
|   48000 |           120 |

PCM：

```text
320 samples
```

---

# Frame Size Reference (20ms)

| Codec    | PCM Samples | PCM Bytes | Encoded Bytes |
| -------- | ----------: | --------: | ------------: |
| PCMU     |         160 |       320 |           160 |
| PCMA     |         160 |       320 |           160 |
| G722 64k |         320 |       640 |           160 |
| G722 56k |         320 |       640 |           140 |
| G722 48k |         320 |       640 |           120 |

---

# RTP Payload Size Reference

以下表格以 **20ms 音频帧** 为例。

## G.711

| Codec | Payload Type | PCM Sample Rate | PCM Samples / 20ms | Encoded Bytes / 20ms | RTP Timestamp Step |
| ----- | -----------: | --------------: | -----------------: | -------------------: | -----------------: |
| PCMU  |            0 |            8000 |                160 |                  160 |                160 |
| PCMA  |            8 |            8000 |                160 |                  160 |                160 |

说明：

* G.711 编码后 **1 sample = 1 byte**
* `8000 * 20 / 1000 = 160`

---

## G.722

| Codec | Payload Type | PCM Sample Rate | PCM Samples / 20ms | Bitrate | Encoded Bytes / 20ms | RTP Timestamp Step |
| ----- | -----------: | --------------: | -----------------: | ------: | -------------------: | -----------------: |
| G722  |            9 |           16000 |                320 |   64000 |                  160 |                160 |
| G722  |            9 |           16000 |                320 |   56000 |                  140 |                160 |
| G722  |            9 |           16000 |                320 |   48000 |                  120 |                160 |

说明：

* `16000 * 20 / 1000 = 320`
* `64000 / 8 * 20 / 1000 = 160`
* `56000 / 8 * 20 / 1000 = 140`
* `48000 / 8 * 20 / 1000 = 120`
* **G.722 RTP timestamp step 按 8000 clock 计算**

---

# Usage Notes

## 1. 必须使用 DirectByteBuffer

正确：

```java
ByteBuffer.allocateDirect(size)
```

不要使用：

```java
ByteBuffer.wrap(new byte[size])
```

JNI 内部通过：

```text
GetDirectBufferAddress()
```

实现零拷贝音频数据访问。

---

## 2. PCM Format

输入输出 PCM 必须为：

```text
16-bit signed PCM
little-endian
mono
```

推荐写法：

```java
ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
```

---

## 3. G711 Parameters

```text
sampleRate = 8000
channels   = 1
bitrate    = 0
options    = 0
```

---

## 4. G722 Parameters

```text
sampleRate = 16000
channels   = 1
bitrate    = 64000 / 56000 / 48000
options    = 0
```

---

## 5. Resampler Parameters

推荐：

```text
channels   = 1
quality    = 5 or 6
options    = 0
```

输入输出格式：

```text
16-bit signed PCM
little-endian
```

说明：

* `inputSamplesPerChannel` 表示每声道输入样本数
* `resampleDirect(...)` 返回每声道输出样本数
* `getResamplerExpectedOutputSamples(...)` 可用于预估输出缓冲区大小

---

## 6. Return Values

| Method                | Return               |
| --------------------- | -------------------- |
| `encodeDirect(...)`   | 编码后的字节数              |
| `decodeDirect(...)`   | 解码后的 PCM sample 数    |
| `resampleDirect(...)` | 输出 PCM sample 数（每声道） |
| 负数                    | 错误                   |

---

# Zero-Copy Design

本项目 JNI 接口统一使用：

```text
DirectByteBuffer
```

避免：

```text
byte[]
heap buffer copy
```

优势：

* 减少 JNI 内存复制
* 降低 GC 压力
* 降低实时音频处理延迟
* 便于接入 RTP / SIP / WebRTC 音频链路

---

# Typical Scenarios

适合以下场景：

* SIP Server 音频编解码
* RTP Payload 编码与解码
* G.711 / G.722 网关
* Java 媒体服务器
* 音频转码链路
* 上游采样率与目标 codec 采样率不一致时的重采样处理

例如：

* `24000 PCM -> Resample -> 8000 -> PCMU`
* `24000 PCM -> Resample -> 16000 -> G722`
* `G722 decode -> 16000 PCM -> Resample -> 8000 PCM`

---

# License

本项目依赖的底层 codec / resampler 实现来源于开源项目：

* **WebRTC third_party codec**
* **SpeexDSP**

请遵循对应上游项目的开源协议。

---

# Summary

`java-media-codec` 提供：

```text
Java JNI Audio Codec Bridge
        +
G711 / G722 Codec
        +
PCM Resampler
        +
DirectByteBuffer Zero-Copy Pipeline
```

适用于：

```text
VoIP
RTP
SIP
Media Server
Audio Gateway
WebRTC Audio Pipeline
```
