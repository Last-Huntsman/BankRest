package org.zuzukov.bank_rest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

	private CryptoService cryptoService;

	@BeforeEach
	void setup() throws Exception {
		cryptoService = new CryptoService();
		byte[] key = new byte[32];
		for (int i = 0; i < key.length; i++) key[i] = (byte) i;
		String base64 = Base64.getEncoder().encodeToString(key);
		Field f = CryptoService.class.getDeclaredField("secretBase64");
		f.setAccessible(true);
		f.set(cryptoService, base64);
		cryptoService.init();
	}

	@Test
	void encrypt_decrypt_roundtrip() {
		String plain = "hello-123";
		String enc = cryptoService.encrypt(plain);
		assertNotEquals(plain, enc);
		String dec = cryptoService.decrypt(enc);
		assertEquals(plain, dec);
	}
}
