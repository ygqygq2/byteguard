package io.github.ygqygq2.byteguard.agent;

import io.github.ygqygq2.byteguard.core.loader.ClassDecryptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * ByteGuard 专用 ClassLoader
 * 
 * <p>从加密的 JAR 中读取并解密类
 * 
 * @author ygqygq2
 */
public class ByteGuardClassLoader extends URLClassLoader {
    
    private final ClassDecryptor decryptor;
    private final Map<String, String> encryptedClasses; // className -> encryptedPath
    
    public ByteGuardClassLoader(URL[] urls, ClassLoader parent, ClassDecryptor decryptor,
                                Map<String, String> encryptedClasses) {
        super(urls, parent);
        this.decryptor = decryptor;
        this.encryptedClasses = encryptedClasses;
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 检查是否是加密的类
        if (encryptedClasses.containsKey(name)) {
            try {
                // 从 JAR 读取加密的类字节码
                String encryptedPath = encryptedClasses.get(name);
                byte[] encryptedBytes = readClassBytes(encryptedPath);
                
                if (encryptedBytes == null) {
                    throw new ClassNotFoundException("Encrypted class data not found: " + name);
                }
                
                // 解密
                byte[] decryptedBytes = decryptor.decrypt(name, encryptedBytes);
                
                System.out.println("[ByteGuard] Loaded and decrypted class: " + name);
                
                // 定义类
                return defineClass(name, decryptedBytes, 0, decryptedBytes.length);
                
            } catch (Exception e) {
                throw new ClassNotFoundException("Failed to load encrypted class: " + name, e);
            }
        }
        
        // 非加密类：使用默认加载
        return super.findClass(name);
    }
    
    /**
     * 从 JAR 读取类字节码
     */
    private byte[] readClassBytes(String path) throws IOException {
        URL resource = getResource(path);
        if (resource == null) {
            return null;
        }
        
        try (InputStream is = resource.openStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            return baos.toByteArray();
        }
    }
}
