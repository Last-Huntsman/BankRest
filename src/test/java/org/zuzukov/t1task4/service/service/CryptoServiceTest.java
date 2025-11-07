package org.zuzukov.t1task4.service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zuzukov.bank_rest.service.crypto.CryptoService;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CryptoServiceTest {
    private CryptoService crypto;

    @BeforeEach
    void setUp() throws Exception {
        crypto = new CryptoService();
        setField(crypto, "secretBase64", "c2VjcmV0ZW5jcnlwdGlvbgAAAAAAAAAAAAAAAAAAAA==");
        crypto.init();
    }

    @Test
    void roundtrip() {
        String plain = "4111111111111111";
        String enc = crypto.encrypt(plain);
        assertNotEquals(plain, enc);
        String dec = crypto.decrypt(enc);
        assertEquals(plain, dec);
    }

    private static void setField(Object target, String name, String value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}


