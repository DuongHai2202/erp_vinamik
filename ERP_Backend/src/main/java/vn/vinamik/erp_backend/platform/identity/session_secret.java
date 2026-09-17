package vn.vinamik.erp_backend.platform.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class session_secret {
    private static final SecureRandom secure_random = new SecureRandom();

    private session_secret() {
    }

    public static generated_session_secret generate() {
        byte[] random_bytes = new byte[32];
        secure_random.nextBytes(random_bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(random_bytes);
        return new generated_session_secret(secret, hash(secret));
    }

    public static String hash(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record generated_session_secret(String value, String hash) {
    }
}