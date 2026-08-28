package com.foodflow.service;

import com.foodflow.config.RazorpayConfig;
import com.foodflow.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private final RazorpayConfig razorpayConfig;

    public RazorpayService(RazorpayConfig razorpayConfig) {
        this.razorpayConfig = razorpayConfig;
    }

    public String getKeyId() {
        return razorpayConfig.getKeyId();
    }

    /**
     * Creates a server-side Razorpay order representation.
     * Amount is in INR rupees; converted to paise (amount * 100).
     */
    public String createRazorpayOrder(BigDecimal amountInRupees, String receipt) {
        log.info("Creating server-side Razorpay order for amount: ₹{}, receipt: {}", amountInRupees, receipt);
        long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).longValue();
        if (amountInPaise <= 0) {
            throw new ApiException("Payment amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        // Generate deterministic / unique Razorpay order ID
        String razorpayOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Generated Razorpay order ID: {}", razorpayOrderId);
        return razorpayOrderId;
    }

    /**
     * Cryptographically verifies the Razorpay signature using HMAC-SHA256.
     * Formula: HMAC_SHA256(razorpayOrderId + "|" + razorpayPaymentId, razorpayKeySecret)
     */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            log.warn("Missing parameter for Razorpay signature verification");
            return false;
        }

        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    razorpayConfig.getKeySecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String calculatedSignature = hexString.toString();
            // Use constant-time comparison to prevent timing attacks
            boolean matches = MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    razorpaySignature.trim().getBytes(StandardCharsets.UTF_8)
            );

            if (!matches) {
                log.warn("Razorpay signature verification failed for order: {}, payment: {}", razorpayOrderId, razorpayPaymentId);
            } else {
                log.info("Razorpay signature verified successfully for order: {}, payment: {}", razorpayOrderId, razorpayPaymentId);
            }
            return matches;
        } catch (Exception ex) {
            log.error("Error during Razorpay signature verification", ex);
            return false;
        }
    }

    /**
     * Helper to compute signature (useful for tests and mock verifications)
     */
    public String calculateSignature(String razorpayOrderId, String razorpayPaymentId) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    razorpayConfig.getKeySecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to calculate test signature", ex);
        }
    }
}
