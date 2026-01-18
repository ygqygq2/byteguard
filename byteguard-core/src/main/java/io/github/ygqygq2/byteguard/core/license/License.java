package io.github.ygqygq2.byteguard.core.license;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * License 数据模型
 * 
 * @author ygqygq2
 */
public class License {
    
    private String version = "1.0";
    private String licenseId;
    private LicenseType licenseType;
    private String issuedTo;
    private Instant issuedAt;
    private Instant expireAt;
    
    private Authorization authorization;
    private String signature;
    
    public enum LicenseType {
        TRIAL,      // 试用版
        STANDARD,   // 标准版
        ENTERPRISE  // 企业版
    }
    
    public enum BindingMode {
        NONE,       // 不绑定
        OPTIONAL,   // 可选绑定（首次运行时绑定）
        STRICT      // 严格绑定
    }
    
    public static class Authorization {
        private int maxInstances;              // 最大实例数（0=不限）
        private List<String> features;         // 功能列表
        private BindingMode bindingMode;
        private List<String> allowedMachineIds;
        
        public Authorization() {
            this.features = new ArrayList<>();
            this.allowedMachineIds = new ArrayList<>();
            this.bindingMode = BindingMode.NONE;
        }
        
        // Getters and Setters
        public int getMaxInstances() { return maxInstances; }
        public void setMaxInstances(int maxInstances) { this.maxInstances = maxInstances; }
        
        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }
        
        public BindingMode getBindingMode() { return bindingMode; }
        public void setBindingMode(BindingMode bindingMode) { this.bindingMode = bindingMode; }
        
        public List<String> getAllowedMachineIds() { return allowedMachineIds; }
        public void setAllowedMachineIds(List<String> allowedMachineIds) { 
            this.allowedMachineIds = allowedMachineIds; 
        }
    }
    
    // Getters and Setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String licenseId) { this.licenseId = licenseId; }
    
    public LicenseType getLicenseType() { return licenseType; }
    public void setLicenseType(LicenseType licenseType) { this.licenseType = licenseType; }
    
    public String getIssuedTo() { return issuedTo; }
    public void setIssuedTo(String issuedTo) { this.issuedTo = issuedTo; }
    
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    
    public Instant getExpireAt() { return expireAt; }
    public void setExpireAt(Instant expireAt) { this.expireAt = expireAt; }
    
    public Authorization getAuthorization() { return authorization; }
    public void setAuthorization(Authorization authorization) { this.authorization = authorization; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
    /**
     * 生成签名数据（除 signature 字段外的所有数据）
     * 
     * @return 用于签名/验证的字符串
     */
    public String toSignatureData() {
        StringBuilder sb = new StringBuilder();
        sb.append("version=").append(version).append("|");
        sb.append("licenseId=").append(licenseId).append("|");
        sb.append("licenseType=").append(licenseType).append("|");
        sb.append("issuedTo=").append(issuedTo).append("|");
        sb.append("issuedAt=").append(issuedAt != null ? issuedAt.toString() : "").append("|");
        sb.append("expireAt=").append(expireAt != null ? expireAt.toString() : "").append("|");
        
        if (authorization != null) {
            sb.append("maxInstances=").append(authorization.maxInstances).append("|");
            sb.append("features=").append(String.join(",", authorization.features)).append("|");
            sb.append("bindingMode=").append(authorization.bindingMode).append("|");
            sb.append("allowedMachineIds=").append(String.join(",", authorization.allowedMachineIds));
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "License{" +
                "licenseId='" + licenseId + '\'' +
                ", type=" + licenseType +
                ", issuedTo='" + issuedTo + '\'' +
                ", expireAt=" + expireAt +
                '}';
    }
}
