package io.github.ygqygq2.byteguard.core.loader;

import io.github.ygqygq2.byteguard.core.crypto.CryptoException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 解密 ClassLoader
 * 
 * <p>在加载类时自动解密，支持所有 Java 特性（Lambda、Method Reference 等）
 * 
 * @author ygqygq2
 */
public class DecryptingClassLoader extends URLClassLoader {
    
    private static final String ENCRYPTED_PREFIX = "META-INF/.encrypted/";
    
    private final ClassDecryptor decryptor;
    
    public DecryptingClassLoader(URL[] urls, ClassLoader parent, ClassDecryptor decryptor) {
        super(urls, parent);
        this.decryptor = decryptor;
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 尝试从加密目录加载
        String encryptedPath = ENCRYPTED_PREFIX + name.replace('.', '/') + ".class";
        
        try (InputStream is = getResourceAsStream(encryptedPath)) {
            if (is != null) {
                // 读取加密的字节码
                byte[] encryptedBytes = readAllBytes(is);
                
                // 解密
                byte[] decryptedBytes = decryptor.decrypt(name, encryptedBytes);
                
                // 定义类
                return defineClass(name, decryptedBytes, 0, decryptedBytes.length);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to load encrypted class: " + name, e);
        } catch (CryptoException e) {
            throw new ClassNotFoundException("Failed to decrypt class: " + name, e);
        }
        
        // 如果加密目录没有，尝试正常加载（可能是未加密的类）
        return super.findClass(name);
    }
    
    /**
     * 读取 InputStream 的所有字节
     */
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        return buffer.toByteArray();
    }
}
