package server.account;

import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Minimal JWT (HS256) signer/verifier using the provided secret.
 * Implemented as a record for immutability and clarity.
 */
public record JwtTokenSigner(String secret) implements TokenSigner {

    @Override
    public String sign(String username, long expiresAt) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + escapeJson(username) + "\",\"exp\":" + expiresAt + "}";

        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = base64UrlEncode(hmacSha256(signingInput));

        return signingInput + "." + signature;
    }

    @Override
    public AccountPrincipal verify(String token) throws InvalidTokenException {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidTokenException("Malformed token");
            }

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            // Verify signature
            String signingInput = header + "." + payload;
            byte[] expectedSignature = hmacSha256(signingInput);
            byte[] providedSignature = base64UrlDecode(signature);
            if (!constantTimeEquals(expectedSignature, providedSignature)) {
                throw new InvalidTokenException("Invalid signature");
            }

            // Decode payload and check expiration
            String payloadJson = new String(base64UrlDecode(payload), StandardCharsets.UTF_8);
            String sub = extractJsonString(payloadJson, "sub");
            long exp = extractJsonLong(payloadJson, "exp");
            if (sub == null || exp == 0) {
                throw new InvalidTokenException("Missing required claims");
            }

            if (exp <= Instant.now().getEpochSecond()) {
                throw new InvalidTokenException("Token expired");
            }

            return new AccountPrincipal(sub, exp);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Token verification failed: " + e.getMessage());
        }
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static long extractJsonLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        String number = json.substring(start, end);
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}