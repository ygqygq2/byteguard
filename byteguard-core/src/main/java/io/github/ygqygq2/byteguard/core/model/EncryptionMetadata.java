package io.github.ygqygq2.byteguard.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 加密元数据
 *
 * <p>存储在 JAR 的 {@code META-INF/byteguard-metadata.json} 中。
 *
 * <p>JSON 示例：
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "algorithm": "AES-256-GCM",
 *   "keyDerivation": "PBKDF2-HKDF",
 *   "salt": "base64url...",
 *   "encryptedAt": 1700000000000,
 *   "totalClasses": 42,
 *   "classes": {
 *     "com/example/Main.class": 1234
 *   }
 * }
 * }</pre>
 *
 * @author ygqygq2
 */
public class EncryptionMetadata {

    /** 元数据路径（固定） */
    public static final String METADATA_PATH = "META-INF/.byteguard/metadata.json";

    /**
     * 加密类目录前缀（已废弃，保留兼容性）
     *
     * @deprecated 加密字节现在写入原始类路径
     */
    @Deprecated

    private String version = "1.0";
    private String algorithm = "AES-256-GCM";
    private String keyDerivation = "PBKDF2-HKDF";
    private byte[] salt;
    private long encryptedAt;
    private int totalClasses;

    /**
     * key = 类文件路径（如 {@code com/example/Main.class}），value = 加密字节大小
     */
    private Map<String, Integer> classes = new LinkedHashMap<>();

    public EncryptionMetadata() {}

    public EncryptionMetadata(byte[] salt) {
        this.salt = salt;
        this.encryptedAt = System.currentTimeMillis();
    }

    public void addClass(String classPath, int encryptedSize) {
        classes.put(classPath, encryptedSize);
        totalClasses = classes.size();
    }

    public boolean containsClass(String classPath) {
        return classes.containsKey(classPath);
    }

    // --- Getters & Setters ---

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getKeyDerivation() { return keyDerivation; }
    public void setKeyDerivation(String keyDerivation) { this.keyDerivation = keyDerivation; }

    public byte[] getSalt() { return salt; }
    public void setSalt(byte[] salt) { this.salt = salt; }

    public long getEncryptedAt() { return encryptedAt; }
    public void setEncryptedAt(long encryptedAt) { this.encryptedAt = encryptedAt; }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

    public Map<String, Integer> getClasses() { return classes; }
    public void setClasses(Map<String, Integer> classes) {
        this.classes = classes;
        this.totalClasses = classes == null ? 0 : classes.size();
    }
}
