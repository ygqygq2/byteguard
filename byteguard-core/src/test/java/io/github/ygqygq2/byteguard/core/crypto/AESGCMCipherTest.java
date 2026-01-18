package io.github.ygqygq2.byteguard.core.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AES-GCM 加密测试
 * 
 * @author ygqygq2
 */
class AESGCMCipherTest {
    
    @Test
    void testEncryptDecrypt() throws CryptoException {
        AESGCMCipher cipher = new AESGCMCipher();
        byte[] key = cipher.generateKey();
        
        String plaintext = "Hello, ByteGuard!";
        byte[] plaintextBytes = plaintext.getBytes();
        
        // 加密
        byte[] encrypted = cipher.encrypt(plaintextBytes, key);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > plaintextBytes.length); // 包含 IV + Tag
        
        // 解密
        byte[] decrypted = cipher.decrypt(encrypted, key);
        assertArrayEquals(plaintextBytes, decrypted);
        assertEquals(plaintext, new String(decrypted));
    }
    
    @Test
    void testDecryptWithWrongKey() throws CryptoException {
        AESGCMCipher cipher = new AESGCMCipher();
        byte[] key1 = cipher.generateKey();
        byte[] key2 = cipher.generateKey();
        
        byte[] plaintext = "Secret data".getBytes();
        byte[] encrypted = cipher.encrypt(plaintext, key1);
        
        // 用错误的密钥解密应该失败
        assertThrows(CryptoException.class, () -> {
            cipher.decrypt(encrypted, key2);
        });
    }
    
    @Test
    void testDecryptTamperedData() throws CryptoException {
        AESGCMCipher cipher = new AESGCMCipher();
        byte[] key = cipher.generateKey();
        
        byte[] plaintext = "Original data".getBytes();
        byte[] encrypted = cipher.encrypt(plaintext, key);
        
        // 篡改密文
        encrypted[20] ^= 0xFF;
        
        // 解密应该失败（GCM Tag 验证失败）
        byte[] finalEncrypted = encrypted;
        assertThrows(CryptoException.class, () -> {
            cipher.decrypt(finalEncrypted, key);
        });
    }
    
    @Test
    void testEncryptLargeData() throws CryptoException {
        AESGCMCipher cipher = new AESGCMCipher();
        byte[] key = cipher.generateKey();
        
        // 测试 1MB 数据
        byte[] largeData = new byte[1024 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        byte[] encrypted = cipher.encrypt(largeData, key);
        byte[] decrypted = cipher.decrypt(encrypted, key);
        
        assertArrayEquals(largeData, decrypted);
    }
}
