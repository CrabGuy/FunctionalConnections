package server.account;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;

/**
 * Implementation of {@link TokenSigner} that signs and verifies JWT-like tokens using HMAC-SHA256.
 */
public record JwtTokenSigner(String secret) implements TokenSigner {

  /**
   * Signs a username with an expiration time and returns a JWT-style token.
   *
   * @param username the username
   * @param expiresAt the expiration timestamp in milliseconds
   * @return the signed token
   */
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

  /**
   * Verifies a token and extracts the principal.
   *
   * @param token the token to verify
   * @return the account principal
   * @throws InvalidTokenException if the token is invalid or expired
   */
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
      String signingInput = header + "." + payload;

      byte[] expectedSignature = hmacSha256(signingInput);
      byte[] providedSignature = base64UrlDecode(signature);
      if (!constantTimeEquals(expectedSignature, providedSignature)) {
        throw new InvalidTokenException("Invalid signature");
      }

      String payloadJson = new String(base64UrlDecode(payload), StandardCharsets.UTF_8);
      String sub = extractJsonString(payloadJson, "sub");
      long exp = extractJsonLong(payloadJson, "exp");
      if (sub == null || exp == 0) {
        throw new InvalidTokenException("Missing required claims");
      }
      if (exp <= System.currentTimeMillis()) {
        throw new InvalidTokenException("Token expired");
      }
      return new AccountPrincipal(sub, exp);
    } catch (InvalidTokenException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidTokenException("Token verification failed: " + e.getMessage());
    }
  }

  /**
   * Computes HMAC-SHA256 of the given data using the secret.
   *
   * @param data the input string
   * @return the MAC bytes
   */
  private byte[] hmacSha256(String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(keySpec);
      return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("HMAC-SHA256 not available", e);
    }
  }

  /**
   * Base64 URL-encodes the given bytes without padding.
   *
   * @param data the bytes to encode
   * @return the encoded string
   */
  private static String base64UrlEncode(byte[] data) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }

  /**
   * Base64 URL-decodes the given string.
   *
   * @param data the encoded string
   * @return the decoded bytes
   */
  private static byte[] base64UrlDecode(String data) {
    return Base64.getUrlDecoder().decode(data);
  }

  /**
   * Compares two byte arrays in constant time.
   *
   * @param a first array
   * @param b second array
   * @return true if equal, false otherwise
   */
  private static boolean constantTimeEquals(byte[] a, byte[] b) {
    if (a.length != b.length) return false;
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    return result == 0;
  }

  /**
   * Escapes special characters in a JSON string.
   *
   * @param value the raw string
   * @return the escaped string
   */
  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /**
   * Extracts a string value from a simple JSON object by key.
   *
   * @param json the JSON string
   * @param key the key
   * @return the value, or null if not found
   */
  private static String extractJsonString(String json, String key) {
    String pattern = "\"" + key + "\":\"";
    int start = json.indexOf(pattern);
    if (start == -1) return null;
    start += pattern.length();
    int end = json.indexOf('"', start);
    if (end == -1) return null;
    return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
  }

  /**
   * Extracts a long value from a simple JSON object by key.
   *
   * @param json the JSON string
   * @param key the key
   * @return the value, or 0 if not found or invalid
   */
  private static long extractJsonLong(String json, String key) {
    String pattern = "\"" + key + "\":";
    int start = json.indexOf(pattern);
    if (start == -1) return 0;
    start += pattern.length();
    int end = start;
    while (end < json.length()
        && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
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
