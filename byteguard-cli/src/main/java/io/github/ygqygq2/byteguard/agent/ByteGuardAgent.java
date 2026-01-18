package io.github.ygqygq2.byteguard.agent;

import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;
import io.github.ygqygq2.byteguard.core.license.License;
import io.github.ygqygq2.byteguard.core.license.LicenseException;
import io.github.ygqygq2.byteguard.core.license.LicenseSerializer;
import io.github.ygqygq2.byteguard.core.license.LicenseValidator;
import io.github.ygqygq2.byteguard.core.license.GPGLicenseValidator;
import io.github.ygqygq2.byteguard.core.license.PublicKeyLoader;
import io.github.ygqygq2.byteguard.core.loader.ClassDecryptor;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.security.PublicKey;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ByteGuard JavaAgent 入口
 * 
 * <p>用法: java -javaagent:byteguard.jar=password=yourpassword -jar app.jar
 * 
 * @author ygqygq2
 */
public class ByteGuardAgent {
    
    private static ClassDecryptor decryptor;
    private static License license;
    
    /**
     * Agent 入口（VM 启动时）
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[ByteGuard] Starting ByteGuard Agent...");
        
        try {
            // 1. 解析参数
            AgentConfig config = parseAgentArgs(agentArgs);
            
            // 2. 查找并验证 License
            license = loadAndValidateLicense(config);
            System.out.println("[ByteGuard] License validated successfully");
            System.out.println("  - License ID: " + license.getLicenseId());
            System.out.println("  - Issued to: " + license.getIssuedTo());
            System.out.println("  - Type: " + license.getLicenseType());
            if (license.getExpireAt() != null) {
                System.out.println("  - Expires at: " + license.getExpireAt());
            }
            
            // 3. 查找加密的 JAR 并读取元数据
            EncryptionMetadata metadata = loadMetadata();
            if (metadata != null) {
                System.out.println("[ByteGuard] Found encrypted JAR with " + metadata.totalClasses + " classes");
            }
            
            // 4. 初始化解密器
            byte[] salt = metadata != null ? metadata.salt : new KeyDerivation().generateSalt();
            KeyDerivation keyDerivation = new KeyDerivation();
            byte[] masterKey = keyDerivation.deriveMasterKey(config.password, salt);
            
            decryptor = new ClassDecryptor(masterKey);
            System.out.println("[ByteGuard] Decryption engine initialized");
            
            // 5. 注册 ClassFileTransformer
            if (metadata != null && metadata.encryptedClasses != null) {
                ByteGuardTransformer transformer = new ByteGuardTransformer(
                    decryptor, 
                    metadata.encryptedClasses,
                    metadata.jarFile
                );
                inst.addTransformer(transformer);
                System.out.println("[ByteGuard] ClassFileTransformer registered");
            }
            
            System.out.println("[ByteGuard] Agent initialized successfully");
            
        } catch (Exception e) {
            System.err.println("[ByteGuard] Failed to initialize agent: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Agent 入口（运行时附加）
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
    
    /**
     * 解析 Agent 参数
     * 
     * <p>格式: password=xxx,license=/path/to/license.lic
     */
    private static AgentConfig parseAgentArgs(String agentArgs) {
        AgentConfig config = new AgentConfig();
        
        if (agentArgs == null || agentArgs.isEmpty()) {
            throw new IllegalArgumentException(
                "Missing agent arguments. Usage: -javaagent:byteguard.jar=password=yourpassword"
            );
        }
        
        String[] pairs = agentArgs.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                
                switch (key) {
                    case "password":
                        config.password = value;
                        break;
                    case "license":
                        config.licensePath = value;
                        break;
                }
            }
        }
        
        if (config.password == null || config.password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        return config;
    }
    
    /**
     * 加载并验证 License
     */
    private static License loadAndValidateLicense(AgentConfig config) throws Exception {
        // 查找 License 文件
        File licenseFile = findLicenseFile(config.licensePath);
        
        if (licenseFile == null || !licenseFile.exists()) {
            throw new LicenseException(
                "License file not found. Please place license.lic in the current directory " +
                "or specify path with -Dbyteguard.license=/path/to/license.lic"
            );
        }
        
        // 加载 License
        String licenseContent = Files.readString(licenseFile.toPath());
        
        // 1. GPG 验证并提取 JSON
        System.out.println("[ByteGuard] Verifying GPG signature...");
        String jsonContent = GPGLicenseValidator.verifyAndExtract(licenseContent);
        System.out.println("[ByteGuard] GPG signature verification: PASSED");
        
        // 2. 解析 License JSON
        LicenseSerializer serializer = new LicenseSerializer();
        License license = serializer.fromJson(jsonContent);
        
        // 3. GPG 已验证，无需 RSA 签名验证
        // GPG clearsign 已提供完整性和真实性保证
        
        // 4. 打印 License 信息
        PublicKey publicKey = PublicKeyLoader.loadEmbeddedPublicKey();
        LicenseValidator validator = new LicenseValidator(publicKey);
        System.out.println("[ByteGuard] " + validator.getLicenseInfo(license));
        
        return license;
    }
    
    /**
     * 查找 License 文件
     * 
     * <p>优先级：
     * <ol>
     *   <li>命令行参数指定的路径</li>
     *   <li>系统属性 -Dbyteguard.license</li>
     *   <li>环境变量 BYTEGUARD_LICENSE</li>
     *   <li>当前目录 ./license.lic</li>
     *   <li>用户目录 ~/.byteguard/license.lic</li>
     * </ol>
     */
    private static File findLicenseFile(String configPath) {
        // 1. 命令行参数
        if (configPath != null && !configPath.isEmpty()) {
            File f = new File(configPath);
            if (f.exists()) return f;
        }
        
        // 2. 系统属性
        String sysProp = System.getProperty("byteguard.license");
        if (sysProp != null) {
            File f = new File(sysProp);
            if (f.exists()) return f;
        }
        
        // 3. 环境变量
        String envVar = System.getenv("BYTEGUARD_LICENSE");
        if (envVar != null) {
            File f = new File(envVar);
            if (f.exists()) return f;
        }
        
        // 4. 当前目录
        File current = new File("license.lic");
        if (current.exists()) return current;
        
        // 5. 用户目录
        String home = System.getProperty("user.home");
        File userDir = new File(home, ".byteguard/license.lic");
        if (userDir.exists()) return userDir;
        
        return null;
    }
    
    /**
     * Agent 配置
     */
    private static class AgentConfig {
        String password;
        String licensePath;
    }
    
    /**
     * 获取解密器（供其他类使用）
     */
    public static ClassDecryptor getDecryptor() {
        return decryptor;
    }
    
    /**
     * 获取 License（供其他类使用）
     */
    public static License getLicense() {
        return license;
    }
    
    /**
     * 加载加密元数据
     */
    private static EncryptionMetadata loadMetadata() {
        try {
            // 查找包含加密元数据的 JAR
            String classpath = System.getProperty("java.class.path");
            String[] jars = classpath.split(File.pathSeparator);
            
            for (String jarPath : jars) {
                File jarFile = new File(jarPath);
                if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
                    continue;
                }
                
                try (JarFile jar = new JarFile(jarFile)) {
                    JarEntry metadataEntry = jar.getJarEntry("META-INF/.byteguard/metadata.json");
                    if (metadataEntry == null) {
                        continue;
                    }
                    
                    // 读取元数据
                    try (InputStream is = jar.getInputStream(metadataEntry);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        
                        StringBuilder json = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            json.append(line);
                        }
                        
                        return parseMetadata(json.toString(), jarFile);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ByteGuard] Warning: Failed to load metadata: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 解析元数据 JSON
     */
    private static EncryptionMetadata parseMetadata(String json, File jarFile) {
        EncryptionMetadata metadata = new EncryptionMetadata();
        metadata.jarFile = jarFile;
        
        // 简单 JSON 解析
        String saltStr = extractJsonString(json, "salt");
        if (saltStr != null) {
            metadata.salt = Base64.getDecoder().decode(saltStr);
        }
        
        String totalClassesStr = extractJsonNumber(json, "totalClasses");
        if (totalClassesStr != null) {
            metadata.totalClasses = Integer.parseInt(totalClassesStr);
        }
        
        // 解析加密类映射
        metadata.encryptedClasses = new HashMap<>();
        String classesBlock = extractJsonObject(json, "encryptedClasses");
        if (classesBlock != null) {
            parseEncryptedClasses(classesBlock, metadata.encryptedClasses);
        }
        
        return metadata;
    }
    
    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    private static String extractJsonNumber(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    private static String extractJsonObject(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start == -1) return null;
        
        start = json.indexOf("{", start);
        int braceCount = 1;
        int end = start + 1;
        
        while (braceCount > 0 && end < json.length()) {
            char c = json.charAt(end);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            end++;
        }
        
        return json.substring(start, end);
    }
    
    private static void parseEncryptedClasses(String json, Map<String, String> map) {
        // 提取所有类名和加密路径
        String pattern = "\"([^\"]+)\"\\s*:\\s*\\{[^}]*\"encryptedPath\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        while (m.find()) {
            String className = m.group(1);
            String encryptedPath = m.group(2);
            map.put(className, encryptedPath);
        }
    }
    
    /**
     * 加密元数据
     */
    private static class EncryptionMetadata {
        File jarFile;
        byte[] salt;
        int totalClasses;
        Map<String, String> encryptedClasses; // className -> encryptedPath
    }
    
    /**
     * ClassFileTransformer - 拦截类加载并解密
     */
    private static class ByteGuardTransformer implements ClassFileTransformer {
        
        private final ClassDecryptor decryptor;
        private final Map<String, String> encryptedClasses;
        private final File jarFile;
        
        ByteGuardTransformer(ClassDecryptor decryptor, Map<String, String> encryptedClasses, File jarFile) {
            this.decryptor = decryptor;
            this.encryptedClasses = encryptedClasses;
            this.jarFile = jarFile;
        }
        
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            
            // 转换类名格式: com/example/MyClass -> com.example.MyClass
            String dottedClassName = className != null ? className.replace('/', '.') : null;
            
            // 检查是否是加密的类
            if (dottedClassName == null || !encryptedClasses.containsKey(dottedClassName)) {
                return null; // 不修改
            }
            
            try {
                // classfileBuffer 本身就是加密的字节码（因为我们把加密数据写到原始位置）
                // 直接解密即可
                byte[] decrypted = decryptor.decrypt(dottedClassName, classfileBuffer);
                System.out.println("[ByteGuard] Decrypted class: " + dottedClassName);
                return decrypted;
                
            } catch (Exception e) {
                System.err.println("[ByteGuard] Failed to decrypt class " + dottedClassName + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
    }
}
