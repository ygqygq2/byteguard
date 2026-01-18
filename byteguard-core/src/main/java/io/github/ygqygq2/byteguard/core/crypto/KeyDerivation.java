package io.github.ygqygq2.byteguard.core.crypto;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * 密钥派生函数 - PBKDF2 + HKDF
 * 
 * <p>PBKDF2: 从用户密码派生主密钥（抗暴力破解）
 * <p>HKDF: 从主密钥派生每个类的独立密钥（隔离性）
 * 
 * @author ygqygq2
 */
public class KeyDerivation {
    
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 100_000; // 10万次迭代
    private static final int KEY_LENGTH = 256; // bits
    
    /**
     * 从密码派生主密钥（使用 PBKDF2）
     * 
     * @param password 用户密码
     * @param salt 盐值（至少32字节）
     * @return 32字节的主密钥
     * @throws CryptoException 密钥派生失败
     */
    public byte[] deriveMasterKey(String password, byte[] salt) throws CryptoException {
        if (password == null || password.isEmpty()) {
            throw new CryptoException("Password cannot be null or empty");
        }
        if (salt == null || salt.length < 16) {
            throw new CryptoException("Salt must be at least 16 bytes");
        }
        
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH
            );
            
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] key = factory.generateSecret(spec).getEncoded();
            
            // 清除密码
            spec.clearPassword();
            
            return key;
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException("Failed to derive master key", e);
        }
    }
    
    /**
     * 从主密钥派生类密钥（使用简化的 HKDF）
     * 
     * <p>使用类名作为上下文信息，确保每个类有独立的密钥
     * 
     * @param masterKey 主密钥（32字节）
     * @param className 类的全限定名（如 com.example.MyClass）
     * @return 32字节的类密钥
     * @throws CryptoException 派生失败
     */
    public byte[] deriveClassKey(byte[] masterKey, String className) throws CryptoException {
        if (masterKey == null || masterKey.length != 32) {
            throw new CryptoException("Master key must be 32 bytes");
        }
        if (className == null || className.isEmpty()) {
            throw new CryptoException("Class name cannot be null or empty");
        }
        
        try {
            // HKDF-Expand 简化实现
            // 实际是: HMAC-SHA256(masterKey, className || 0x01)
            byte[] info = className.getBytes(StandardCharsets.UTF_8);
            byte[] input = Arrays.copyOf(info, info.length + 1);
            input[input.length - 1] = 0x01;
            
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(masterKey, "HmacSHA256");
            hmac.init(keySpec);
            
            return hmac.doFinal(input);
            
        } catch (Exception e) {
            throw new CryptoException("Failed to derive class key for: " + className, e);
        }
    }
    
    /**
     * 生成随机盐值
     * 
     * @return 32字节的随机盐
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[32];
        new java.security.SecureRandom().nextBytes(salt);
        return salt;
    }
}
