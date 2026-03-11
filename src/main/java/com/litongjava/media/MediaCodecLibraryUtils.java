package com.litongjava.media;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.litongjava.media.core.CodecCore;

public class MediaCodecLibraryUtils {

  public static final String WIN_AMD64 = "win_amd64";
  public static final String DARWIN_ARM64 = "darwin_arm64";
  public static final String LINUX_AMD64 = "linux_amd64";

  public static void load() {

    String osName = System.getProperty("os.name").toLowerCase();
    String userHome = System.getProperty("user.home");

    System.out.println("os name: " + osName + " user.home: " + userHome + " lib name: " + "media-codec");

    String archName;
    String libFileName = null;

    if (osName.contains("win")) {
      libFileName = CodecCore.WIN_NATIVE_LIBRARY_NAME;
      archName = WIN_AMD64;

    } else if (osName.contains("mac")) {
      libFileName = CodecCore.MACOS_NATIVE_LIBRARY_NAME;
      archName = DARWIN_ARM64;

    } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix") || osName.contains("linux")) {
      libFileName = CodecCore.UNIX_NATIVE_LIBRARY_NAME;
      archName = LINUX_AMD64;

    } else {
      throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    String dstDir = userHome + File.separator + "lib" + File.separator + archName;
    File dir = new File(dstDir);
    if (!dir.exists())
      dir.mkdirs();

    // ================= WINDOWS =================
    if (WIN_AMD64.equals(archName)) {

      File libFile = new File(dstDir, libFileName);
      extractResource("/lib/" + archName + "/" + libFileName, libFile);

      System.load(libFile.getAbsolutePath());
      return;
    }

    // ================= MAC =================
    if (DARWIN_ARM64.equals(archName)) {
      File libFile = new File(dstDir, libFileName);
      extractResource("/lib/" + archName + "/" + libFileName, libFile);
      System.load(libFile.getAbsolutePath());
      return;
    }

    // ================= LINUX =================
    File libFile = new File(dstDir, libFileName);
    extractResource("/lib/" + archName + "/" + libFileName, libFile);
    System.load(libFile.getAbsolutePath());
    return;
  }

  private static void extractResource(String resourcePath, File destination) {
    if (destination.exists())
      return;

    System.out.println("copy from " + resourcePath + " to " + destination.getAbsolutePath());

    try (InputStream in = MediaCodecLibraryUtils.class.getResourceAsStream(resourcePath)) {

      if (in != null) {
        Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } else {
        System.err.println("Resource does not exist: " + resourcePath);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to extract resource: " + resourcePath, e);
    }
  }
}