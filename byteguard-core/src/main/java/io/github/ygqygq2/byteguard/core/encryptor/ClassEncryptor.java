package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.crypto.AESGCMCipher;
import io.github.ygqygq2.byteguard.core.crypto.CryptoException;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;
import io.github.ygqygq2.byteguard.core.model.EncryptedClass;

/**
 * 单个类文件加密器
 *
 * <p>流程：
 * <ol>
 *   <li>从主密钥派生类专属密钥（HKDF）
 *   <li>使用 AES-256-GCM 加密字节码
 *   <li>返回 {@link EncryptedClass}（IV 已内嵌于密文）
 * </ol>
 *
 * @author ygqygq2
 * @see KeyDerivation
 * @see AESGCMCipher
 */
public class ClassEncryptor {

    private final byte[] masterKey;
    private final KeyDerivation keyDerivation;
    private final AESGCMCipher cipher;

    /**
     * @param masterKey 32 字节主密钥（由 PBKDF2 从用户密码派生）
     */
    public ClassEncryptor(byte[] masterKey) {
        if (masterKey == null || masterKey.length != 32) {
            throw new IllegalArgumentException("masterKey must be 32 bytes");
        }
        this.masterKey = masterKey;
        this.keyDerivation = new KeyDerivation();
        this.cipher = new AESGCMCipher();
    }

    /**
     * 加密单个类的字节码
     *
     * @param className     类的全限定名（如 {@code com.example.Main}）
     * @param classBytes    原始 .class 字节码
     * @return 加密结果
     * @throws CryptoException 加密失败
     */
    public EncryptedClass encrypt(String className, byte[] classBytes) throws CryptoException {
        if (className == null || className.isEmpty()) {
            throw new CryptoException("className cannot be null or empty");
        }
        if (classBytes == null || classBytes.length == 0) {
            throw new CryptoException("classBytes cannot be null or empty");
        }

        byte[] classKey = keyDerivation.deriveClassKey(masterKey, className);
        byte[] encryptedBytes = cipher.encrypt(classBytes, classKey, AESGCMCipher.aadFromClassName(className));
        return new EncryptedClass(className, encryptedBytes);
    }

    /**
     * 解密单个类的字节码（用于验证）
     *
     * @param className      类的全限定名
     * @param encryptedBytes 加密后的字节码
     * @return 原始字节码
     * @throws CryptoException 解密失败
     */
    public byte[] decrypt(String className, byte[] encryptedBytes) throws CryptoException {
        byte[] classKey = keyDerivation.deriveClassKey(masterKey, className);
        return cipher.decrypt(encryptedBytes, classKey, AESGCMCipher.aadFromClassName(className));
    }
}
