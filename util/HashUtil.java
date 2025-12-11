package com.mybible.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * HashUtil - Utility for password hashing using SHA-256
 *
 * Usage:
 *   HashUtil.hash("password123")  -> returns SHA-256 hash
 *   HashUtil.verify("password123", storedHash) -> returns boolean
 */
public class HashUtil {

    /**
     * Hash a string using SHA-256
     *
     * @param input The string to hash
     * @return The SHA-256 hash as a hex string
     * @throws IllegalArgumentException if input is null or empty
     */
    public static String hash(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify input matches stored hash
     *
     * @param input The input string to verify
     * @param storedHash The stored hash to compare against
     * @return true if the input hash matches the stored hash
     */
    public static boolean verify(String input, String storedHash) {
        if (input == null || storedHash == null) {
            return false;
        }

        String inputHash = hash(input);
        return inputHash.equals(storedHash);
    }

    /**
     * Hash a password (alias for hash method)
     *
     * @param password The password to hash
     * @return The SHA-256 hash as a hex string
     */
    public static String hashPassword(String password) {
        return hash(password);
    }

    /**
     * Verify password matches stored hash
     *
     * @param password The password to verify
     * @param storedHash The stored hash to compare against
     * @return true if the password hash matches the stored hash
     */
    public static boolean verifyPassword(String password, String storedHash) {
        return verify(password, storedHash);
    }
}
