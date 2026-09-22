package com.sim.backend.global.util;

import lombok.NoArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@NoArgsConstructor
public final class CryptoUtil {

    public static byte[] encryptAesGcm(byte[] plaintext, byte[] key, byte[] iv) throws Exception {
        if (iv == null || iv.length != 12) throw new IllegalArgumentException("IV must be 12 bytes for GCM");
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32))
            throw new IllegalArgumentException("Key must be 16/24/32 bytes (AES-128/192/256)");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec ks = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, ks, spec);
        return cipher.doFinal(plaintext);
    }

    public static byte[] decryptAesGcm(byte[] ciphertextAndTag, byte[] key, byte[] iv) throws Exception {
        // TODO : 테스트 후에는 꼭 지우기
        if (key == null || iv == null) return ciphertextAndTag;

        if (iv == null || iv.length != 12) throw new IllegalArgumentException("IV must be 12 bytes for GCM");
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32))
            throw new IllegalArgumentException("Key must be 16/24/32 bytes (AES-128/192/256)");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec ks = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, ks, spec);
        return cipher.doFinal(ciphertextAndTag);
    }
}