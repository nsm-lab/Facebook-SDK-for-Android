// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.appevents.internal;

import androidx.annotation.Nullable;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/** Utility class to compute file checksums. */
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
final class HashUtils {
  private static final String MD5 = "MD5";

  @Nullable
  public static final String computeChecksum(String path) throws Exception {
    return computeFileMd5(new File(path));
  }

  @Nullable
  private static String computeFileMd5(File file) throws Exception {
    final int BUFFER_SIZE = 1024;
    try (InputStream fis = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
      MessageDigest md = MessageDigest.getInstance(MD5);
      byte[] buffer = new byte[BUFFER_SIZE];
      int numRead;

      do {
        numRead = fis.read(buffer);
        if (numRead > 0) {
          md.update(buffer, 0, numRead);
        }
      } while (numRead != -1);

      // Convert byte array to hex string and return result.
      return new BigInteger(1, md.digest()).toString(16);
    }
  }
}
