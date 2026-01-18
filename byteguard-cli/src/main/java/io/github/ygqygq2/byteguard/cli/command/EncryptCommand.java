package io.github.ygqygq2.byteguard.cli.command;

import io.github.ygqygq2.byteguard.core.crypto.AESGCMCipher;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Encrypt 命令 - JAR 文件加密
 * 
 * @author ygqygq2
 */
public class EncryptCommand {
    
    private final AESGCMCipher cipher = new AESGCMCipher();
    private final KeyDerivation kd = new KeyDerivation();
    
    public void execute(String[] args) throws Exception {
        System.out.println("[ByteGuard] Encrypt JAR");
        
        // 解析参数
        String inputJar = null;
        String outputJar = null;
        String password = null;
        String[] excludePatterns = new String[0];
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input":
                    inputJar = args[++i];
                    break;
                case "--output":
                    outputJar = args[++i];
                    break;
                case "--password":
                    password = args[++i];
                    break;
                case "--exclude":
                    excludePatterns = args[++i].split(",");
                    break;
            }
        }
        
        if (inputJar == null || outputJar == null || password == null) {
            throw new IllegalArgumentException(
                "Missing required arguments: --input, --output, --password"
            );
        }
        
        File input = new File(inputJar);
        File output = new File(outputJar);
        
        if (!input.exists()) {
            throw new FileNotFoundException("Input JAR not found: " + inputJar);
        }
        
        System.out.println("Input:  " + inputJar);
        System.out.println("Output: " + outputJar);
        System.out.println("Password: ****");
        
        // 派生主密钥
        byte[] salt = kd.generateSalt();
        byte[] masterKey = kd.deriveMasterKey(password, salt);
        
        // 加密 JAR
        int classCount = encryptJar(input, output, masterKey, salt, excludePatterns);
        
        System.out.println();
        System.out.println("✓ Encryption completed!");
        System.out.println("  - Classes encrypted: " + classCount);
        System.out.println("  - Output: " + output.getAbsolutePath());
    }
    
    /**
     * 加密 JAR 文件
     */
    private int encryptJar(File input, File output, byte[] masterKey, byte[] salt,
                           String[] excludePatterns) throws Exception {
        
        int encryptedCount = 0;
        Map<String, ClassInfo> encryptedClasses = new HashMap<>();
        
        try (JarFile jarFile = new JarFile(input);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(output))) {
            
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                // 跳过目录
                if (entry.isDirectory()) {
                    jos.putNextEntry(new ZipEntry(name));
                    jos.closeEntry();
                    continue;
                }
                
                // 判断是否加密
                boolean shouldEncrypt = name.endsWith(".class") 
                    && !name.startsWith("META-INF/")
                    && !isExcluded(name, excludePatterns);
                
                if (shouldEncrypt) {
                    // 读取原始类字节码
                    byte[] classBytes = readEntryBytes(jarFile, entry);
                    
                    // 获取类名
                    String className = name.substring(0, name.length() - 6) // 去掉 .class
                                          .replace('/', '.');
                    
                    // 派生类专用密钥
                    byte[] classKey = kd.deriveClassKey(masterKey, className);
                    
                    // 加密
                    byte[] encrypted = cipher.encrypt(classBytes, classKey);
                    
                    // 关键改动：将加密后的字节码写到原始位置
                    // 这样 JVM 可以找到类，Transformer 会拦截并解密
                    jos.putNextEntry(new ZipEntry(name));
                    jos.write(encrypted);
                    jos.closeEntry();
                    
                    // 同时保存一份到加密目录（用于元数据记录）
                    String encryptedPath = "META-INF/.encrypted/" + name;
                    jos.putNextEntry(new ZipEntry(encryptedPath));
                    jos.write(encrypted);
                    jos.closeEntry();
                    
                    // 记录元数据
                    ClassInfo info = new ClassInfo(name, encryptedPath, className);
                    encryptedClasses.put(className, info);
                    
                    encryptedCount++;
                    
                } else {
                    // 非加密文件：直接复制
                    jos.putNextEntry(new ZipEntry(name));
                    byte[] bytes = readEntryBytes(jarFile, entry);
                    jos.write(bytes);
                    jos.closeEntry();
                }
            }
            
            // 写入元数据文件
            writeMetadata(jos, salt, encryptedClasses);
        }
        
        return encryptedCount;
    }
    
    /**
     * 类信息
     */
    private static class ClassInfo {
        final String originalPath;
        final String encryptedPath;
        final String className;
        
        ClassInfo(String originalPath, String encryptedPath, String className) {
            this.originalPath = originalPath;
            this.encryptedPath = encryptedPath;
            this.className = className;
        }
    }
    
    /**
     * 读取 JAR Entry 的字节
     */
    private byte[] readEntryBytes(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
    
    /**
     * 判断文件是否应该排除
     */
    private boolean isExcluded(String path, String[] excludePatterns) {
        for (String pattern : excludePatterns) {
            if (path.contains(pattern.trim())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 写入加密元数据
     */
    private void writeMetadata(JarOutputStream jos, byte[] salt, Map<String, ClassInfo> encryptedClasses) 
            throws IOException {
        jos.putNextEntry(new ZipEntry("META-INF/.byteguard/metadata.json"));
        
        // 简单 JSON 序列化
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"1.0\",\n");
        json.append("  \"algorithm\": \"AES-256-GCM\",\n");
        json.append("  \"salt\": \"").append(Base64.getEncoder().encodeToString(salt)).append("\",\n");
        json.append("  \"totalClasses\": ").append(encryptedClasses.size()).append(",\n");
        json.append("  \"encryptedAt\": ").append(System.currentTimeMillis()).append(",\n");
        json.append("  \"encryptedClasses\": {\n");
        
        int count = 0;
        for (Map.Entry<String, ClassInfo> entry : encryptedClasses.entrySet()) {
            if (count++ > 0) json.append(",\n");
            ClassInfo info = entry.getValue();
            json.append("    \"").append(entry.getKey()).append("\": {\n");
            json.append("      \"originalPath\": \"").append(info.originalPath).append("\",\n");
            json.append("      \"encryptedPath\": \"").append(info.encryptedPath).append("\",\n");
            json.append("      \"className\": \"").append(info.className).append("\"\n");
            json.append("    }");
        }
        
        json.append("\n  }\n");
        json.append("}\n");
        
        jos.write(json.toString().getBytes("UTF-8"));
        jos.closeEntry();
    }
}
