package com.smartclassroom.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing with PBKDF2 (HMAC-SHA256), a random salt per password and 120,000 iterations.
 * Stored format:  iterations:salt:hash  (Base64 for salt and hash).
 * A password is never stored or compared in plain text.
 */
public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /** Returns true if the plain password matches the stored hash. Any malformed stored value simply returns false. */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(plainPassword.toCharArray(), salt, iterations);
            // constant-time comparison so timing cannot reveal how many bytes matched
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available on this JVM", e);
        }
    }
}
