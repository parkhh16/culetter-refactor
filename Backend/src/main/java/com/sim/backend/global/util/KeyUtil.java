package com.sim.backend.global.util;

import lombok.NoArgsConstructor;

import java.security.SecureRandom;

@NoArgsConstructor
public final class KeyUtil {
    private static final SecureRandom RNG = new SecureRandom();

    /** 32바이트(256비트) AES 대칭키 생성 (DB 저장 O) */
    public static byte[] newAes256KeyBytes() {
        byte[] key = new byte[32];
        RNG.nextBytes(key);
        return key;
    }

    /** 각 파일마다 들어가는 난수 값 (DB 저장 X) */
    public static byte[] newIv12() {
        byte[] iv = new byte[12];
        RNG.nextBytes(iv);
        return iv;
    }
}