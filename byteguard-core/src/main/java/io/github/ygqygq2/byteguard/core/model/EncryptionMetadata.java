package io.github.ygqygq2.byteguard.core.model;

/**
 * 加密元数据
 * 
 * <p>存储在 JAR 的 META-INF/byteguard-metadata.json 中
 * 
 * @author ygqygq2
 */
public class EncryptionMetadata {
    
    private String version = "1.0";
    private String algorithm = "AES-256-GCM";
    private byte[] salt;              // PBKDF2 盐值
    private long encryptedAt;         // 加密时间戳
    private int totalClasses;         // 加密的类总数
    
    public EncryptionMetadata() {
    }
    
    public EncryptionMetadata(byte[] salt, int totalClasses) {
        this.salt = salt;
        this.totalClasses = totalClasses;
        this.encryptedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    
    public byte[] getSalt() { return salt; }
    public void setSalt(byte[] salt) { this.salt = salt; }
    
    public long getEncryptedAt() { return encryptedAt; }
    public void setEncryptedAt(long encryptedAt) { this.encryptedAt = encryptedAt; }
    
    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }
}
