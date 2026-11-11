package com.sagiphayop.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Salted PBKDF2 password hashing. Stored format: {@code iterations:saltHex:hashHex}.
 * Plain text is never stored or compared.
 */
public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    public static String hash(String plain) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        HexFormat hex = HexFormat.of();
        return ITERATIONS + ":" + hex.formatHex(salt) + ":" + hex.formatHex(derive(plain, salt, ITERATIONS));
    }

    public static boolean verify(String plain, String stored) {
        if (plain == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            HexFormat hex = HexFormat.of();
            int iterations = Integer.parseInt(parts[0]);
            byte[] expected = hex.parseHex(parts[2]);
            byte[] actual = derive(plain, hex.parseHex(parts[1]), iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String plain, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(plain.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }
}
