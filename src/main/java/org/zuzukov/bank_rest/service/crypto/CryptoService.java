package org.zuzukov.bank_rest.service.crypto;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zuzukov.bank_rest.exception.CryptoInitializationException;
import org.zuzukov.bank_rest.exception.DecryptionException;
import org.zuzukov.bank_rest.exception.EncryptionException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class CryptoService {

    @Value("${card.encryption-secret}")
    private String secretBase64;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        try {
            if (secretBase64 == null || secretBase64.isBlank()) {
                throw new CryptoInitializationException("Encryption secret (card.encryption-secret) is not configured");
            }

            byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
            if (keyBytes.length != 32) {
                throw new CryptoInitializationException(
                        "Invalid AES key length: " + keyBytes.length + " bytes. Expected 32 bytes (256 bits)."
                );
            }

            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            log.info("CryptoService initialized successfully with 256-bit AES key");
        } catch (Exception e) {
            log.error("Failed to initialize CryptoService: {}", e.getMessage(), e);
            throw new CryptoInitializationException("Failed to initialize CryptoService", e);
        }
    }

    public String encrypt(String plaintext) {
        if (secretKey == null) {
            throw new CryptoInitializationException("CryptoService not initialized: secretKey is null");
        }
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
            ByteBuffer bb = ByteBuffer.allocate(iv.length + ciphertext.length);
            bb.put(iv);
            bb.put(ciphertext);

            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage(), e);
            throw new EncryptionException("Encryption failed: " + e.getMessage(), e);
        }
    }

    public String decrypt(String encoded) {
        if (secretKey == null) {
            throw new CryptoInitializationException("CryptoService not initialized: secretKey is null");
        }
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            ByteBuffer bb = ByteBuffer.wrap(data);

            byte[] iv = new byte[12];
            bb.get(iv);
            byte[] ciphertext = new byte[bb.remaining()];
            bb.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage(), e);
            throw new DecryptionException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
