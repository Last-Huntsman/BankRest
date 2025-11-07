package org.zuzukov.bank_rest.service.crypto;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {
    @Value("${card.encryption-secret}")
    private String secretBase64;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
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
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
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
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}


