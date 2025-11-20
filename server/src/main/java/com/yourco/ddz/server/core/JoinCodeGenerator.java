package com.yourco.ddz.server.core;

import java.security.SecureRandom;

/** Utility class to generate human-friendly 4-letter join codes. */
public class JoinCodeGenerator {
  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // No I, O, 0, 1
  private static final int CODE_LENGTH = 4;
  private static final SecureRandom random = new SecureRandom();

  public static String generate() {
    StringBuilder code = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
