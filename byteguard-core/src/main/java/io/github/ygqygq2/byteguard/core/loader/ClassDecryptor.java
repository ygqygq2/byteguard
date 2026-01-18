package io.github.ygqygq2.byteguard.core.loader;

import io.github.ygqygq2.byteguard.core.crypto.AESGCMCipher;
import io.github.ygqygq2.byteguard.core.crypto.CryptoException;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类解密器
 * 
 * <p>负责解密加密的类字节码，包含缓存机制
 * 
 * @author ygqygq2
 */
public class ClassDecryptor {
    
    private final byte[] masterKey;
    private final KeyDerivation keyDerivation;
    private final AESGCMCipher cipher;
    
    // 缓存：className -> 解密后的字节码
    private final Map<String, byte[]> cache;
    private final int maxCacheSize;
    
    public ClassDecryptor(byte[] masterKey) {
        this(masterKey, 1000); // 默认缓存 1000 个类
    }
    
    public ClassDecryptor(byte[] masterKey, int maxCacheSize) {
        this.masterKey = masterKey;
        this.keyDerivation = new KeyDerivation();
        this.cipher = new AESGCMCipher();
        this.cache = new ConcurrentHashMap<>();
        this.maxCacheSize = maxCacheSize;
    }
    
    /**
     * 解密类字节码
     * 
     * @param className 类的全限定名
     * @param encryptedBytes 加密的字节码
     * @return 解密后的字节码
     * @throws CryptoException 解密失败
     */
    public byte[] decrypt(String className, byte[] encryptedBytes) throws CryptoException {
        // 检查缓存
        byte[] cached = cache.get(className);
        if (cached != null) {
            return cached;
        }
        
        // 派生类密钥
        byte[] classKey = keyDerivation.deriveClassKey(masterKey, className);
        
        // 解密
        byte[] decrypted = cipher.decrypt(encryptedBytes, classKey);
        
        // 缓存（LRU 简化版：满了就清空）
        if (cache.size() >= maxCacheSize) {
            cache.clear();
        }
        cache.put(className, decrypted);
        
        return decrypted;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * 获取缓存统计
     * 
     * @return 缓存大小
     */
    public int getCacheSize() {
        return cache.size();
    }
}
