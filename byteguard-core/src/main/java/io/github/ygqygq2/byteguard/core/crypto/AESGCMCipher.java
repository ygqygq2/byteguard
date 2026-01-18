package io.github.ygqygq2.byteguard.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * AES-256-GCM 加密/解密实现
 * 
 * <p>使用 Galois/Counter Mode (GCM) 提供认证加密，确保数据完整性和机密性。
 * 
 * <p>密文格式: [IV(12 bytes)] + [Ciphertext] + [Auth Tag(16 bytes)]
 * 
 * @author ygqygq2
 */
public class AESGCMCipher {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 推荐的 GCM IV 长度
    private static final int GCM_TAG_LENGTH = 128; // 128 bits = 16 bytes
    private static final int AES_KEY_SIZE = 256 / 8; // 32 bytes
    
    private final SecureRandom secureRandom;
    
    public AESGCMCipher() {
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * 加密字节数组
     * 
     * @param plaintext 明文
     * @param key 32字节的 AES-256 密钥
     * @return 密文 (IV + Ciphertext + Tag)
     * @throws CryptoException 加密失败
     */
    public byte[] encrypt(byte[] plaintext, byte[] key) throws CryptoException {
        validateKey(key);
        
        try {
            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // 初始化 Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            
            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // 组合: IV + Ciphertext (包含 Tag)
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            
            return result;
            
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }
    
    /**
     * 解密字节数组
     * 
     * @param encrypted 密文 (IV + Ciphertext + Tag)
     * @param key 32字节的 AES-256 密钥
     * @return 明文
     * @throws CryptoException 解密失败（密钥错误、数据被篡改等）
     */
    public byte[] decrypt(byte[] encrypted, byte[] key) throws CryptoException {
        validateKey(key);
        
        if (encrypted.length < GCM_IV_LENGTH + GCM_TAG_LENGTH / 8) {
            throw new CryptoException("Invalid encrypted data: too short");
        }
        
        try {
            // 提取 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);
            
            // 提取密文
            int ciphertextLength = encrypted.length - GCM_IV_LENGTH;
            byte[] ciphertext = new byte[ciphertextLength];
            System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertextLength);
            
            // 初始化 Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            // 解密（自动验证 Tag）
            return cipher.doFinal(ciphertext);
            
        } catch (Exception e) {
            throw new CryptoException("Decryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成随机 AES-256 密钥
     * 
     * @return 32字节的随机密钥
     */
    public byte[] generateKey() {
        byte[] key = new byte[AES_KEY_SIZE];
        secureRandom.nextBytes(key);
        return key;
    }
    
    private void validateKey(byte[] key) throws CryptoException {
        if (key == null || key.length != AES_KEY_SIZE) {
            throw new CryptoException(
                "Invalid key size: expected " + AES_KEY_SIZE + " bytes, got " + 
                (key == null ? "null" : key.length)
            );
        }
    }
}
