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
 * <p>在加载类时自动解密，支持所有 Java 特性（Lambda、Method Reference 等）。
 * 加密后的 JAR 中，加密字节位于原始类路径（由 {@link
 * io.github.ygqygq2.byteguard.core.encryptor.JarEncryptor} 生成）。
 *
 * @author ygqygq2
 */
public class DecryptingClassLoader extends URLClassLoader {

    private final ClassDecryptor decryptor;

    public DecryptingClassLoader(URL[] urls, ClassLoader parent, ClassDecryptor decryptor) {
        super(urls, parent);
        this.decryptor = decryptor;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 尝试从原始类路径读取（加密字节已替换原始字节码）
        String classResourcePath = name.replace('.', '/') + ".class";

        try (InputStream is = getResourceAsStream(classResourcePath)) {
            if (is != null) {
                byte[] encryptedBytes = readAllBytes(is);
                byte[] decryptedBytes = decryptor.decrypt(name, encryptedBytes);
                return defineClass(name, decryptedBytes, 0, decryptedBytes.length);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to load encrypted class: " + name, e);
        } catch (CryptoException e) {
            throw new ClassNotFoundException("Failed to decrypt class: " + name, e);
        }

        return super.findClass(name);
    }

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
