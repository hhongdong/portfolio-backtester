package com.portfolio.backtester.application.backtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.portfolio.backtester.api.dto.BacktestRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Builds a stable idempotency key from a request body. Uses SHA-256 over
 * the canonical JSON form (sorted property names) so that two semantically
 * identical requests with different field ordering hash to the same key.
 *
 * Same key + same SUCCESS result → return cached result instead of redoing
 * 30 seconds of compute. Same key + IN-PROGRESS → caller can poll the
 * existing run instead of duplicating it.
 */
public final class IdempotencyKey {

    private static final ObjectMapper CANONICAL = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private IdempotencyKey() {}

    public static String of(BacktestRequest request) {
        try {
            byte[] canonical = CANONICAL.writeValueAsBytes(request);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha.digest(canonical);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("failed to canonicalize request", e);
        }
    }

    public static String ofRaw(String canonicalJson) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(canonicalJson.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
