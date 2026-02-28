package io.github.ygqygq2.byteguard.core.crypto;

import java.security.SecureRandom;

/**
 * 安全随机盐生成器
 *
 * <p>使用 {@link SecureRandom} 生成密码学安全的盐值，用于 PBKDF2 密钥派生。
 *
 * @author ygqygq2
 * @see KeyDerivation
 */
public class SaltGenerator {

    /** 默认盐长度（字节） */
    public static final int DEFAULT_SALT_LENGTH = 32;

    private final SecureRandom secureRandom;

    public SaltGenerator() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * 生成默认长度（32 字节）的随机盐
     *
     * @return 32 字节随机盐
     */
    public byte[] generate() {
        return generate(DEFAULT_SALT_LENGTH);
    }

    /**
     * 生成指定长度的随机盐
     *
     * @param length 盐的字节长度（至少 16）
     * @return 随机盐
     * @throws IllegalArgumentException 长度小于 16
     */
    public byte[] generate(int length) {
        if (length < 16) {
            throw new IllegalArgumentException("Salt length must be at least 16 bytes, got: " + length);
        }
        byte[] salt = new byte[length];
        secureRandom.nextBytes(salt);
        return salt;
    }
}
