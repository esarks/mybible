package com.mybible.util;

import java.util.*;
import java.time.Instant;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * JWTUtil - Utility class for JWT token operations
 *
 * This is a simplified JWT implementation for demonstration.
 * For production, consider using nimbus-jose-jwt or auth0 java-jwt.
 *
 * Usage:
 *   String token = JWTUtil.generate(userId, email, role);
 *   JWTUtil.JWTPayload payload = JWTUtil.verify(token);
 */
public class JWTUtil {

    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    /**
     * Get the JWT secret from environment or use default
     */
    private static String getSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isEmpty()) {
            secret = "mybible-256-bit-secret-key-change-in-production-min-32-chars";
            System.out.println("[JWT] WARNING: Using default JWT secret. Set JWT_SECRET env var in production!");
        }
        return secret;
    }

    /**
     * Generate JWT token
     *
     * @param userId The user ID to encode
     * @param email The email to encode
     * @param role The role (user/admin) to encode
     * @return The generated JWT token string
     */
    public static String generate(String userId, String email, String role) {
        String secret = getSecret();

        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + EXPIRATION_MS;

        // Build payload
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format(
            "{\"sub\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
            userId, escapeJson(email), role, nowMillis / 1000, expMillis / 1000
        );

        // Encode
        String encodedHeader = base64UrlEncode(header.getBytes());
        String encodedPayload = base64UrlEncode(payload.getBytes());
        String data = encodedHeader + "." + encodedPayload;

        // Sign
        String signature = hmacSha256(data, secret);
        String encodedSignature = base64UrlEncode(signature.getBytes());

        return data + "." + encodedSignature;
    }

    /**
     * Verify and decode JWT token
     *
     * @param token The JWT token to verify
     * @return The decoded JWTPayload
     * @throws IllegalArgumentException if token is null, empty, or malformed
     * @throws SecurityException if signature is invalid or token is expired
     */
    public static JWTPayload verify(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        // Verify signature
        String secret = getSecret();
        String data = header + "." + payload;
        String expectedSignature = base64UrlEncode(hmacSha256(data, secret).getBytes());

        if (!signature.equals(expectedSignature)) {
            throw new SecurityException("Invalid token signature");
        }

        // Decode payload
        String decodedPayload = new String(base64UrlDecode(payload));

        // Parse payload
        JWTPayload jwtPayload = new JWTPayload();
        jwtPayload.userId = extractJsonValue(decodedPayload, "sub");
        jwtPayload.email = extractJsonValue(decodedPayload, "email");
        jwtPayload.role = extractJsonValue(decodedPayload, "role");

        String expStr = extractJsonValue(decodedPayload, "exp");
        long exp = Long.parseLong(expStr);
        long now = System.currentTimeMillis() / 1000;

        if (now > exp) {
            throw new SecurityException("Token expired");
        }

        return jwtPayload;
    }

    /**
     * Base64 URL encode
     */
    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Base64 URL decode
     */
    private static byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    /**
     * HMAC SHA-256 signature
     */
    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes());
            return new String(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }

    /**
     * Extract value from JSON string (simple implementation)
     */
    private static String extractJsonValue(String json, String key) {
        // Simple extraction: "key":"value" or "key":number
        String pattern = "\"" + key + "\":\"([^\"]+)\"";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(json);

        if (m.find()) {
            return m.group(1);
        }

        // Try numeric value
        pattern = "\"" + key + "\":([0-9]+)";
        p = Pattern.compile(pattern);
        m = p.matcher(json);

        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    /**
     * Escape string for JSON
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * JWT payload data holder
     */
    public static class JWTPayload {
        public String userId;
        public String email;
        public String role;

        @Override
        public String toString() {
            return String.format("JWTPayload[userId=%s, email=%s, role=%s]",
                userId, email, role);
        }
    }
}
