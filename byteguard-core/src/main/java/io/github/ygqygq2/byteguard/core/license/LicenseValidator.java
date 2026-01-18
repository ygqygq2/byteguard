package io.github.ygqygq2.byteguard.core.license;

import java.security.PublicKey;
import java.time.Instant;

/**
 * License 验证器
 * 
 * <p>验证 License 的有效性，包括：
 * <ul>
 *   <li>签名验证</li>
 *   <li>有效期验证</li>
 *   <li>实例数验证（可选）</li>
 *   <li>机器绑定验证（可选）</li>
 * </ul>
 * 
 * @author ygqygq2
 */
public class LicenseValidator {
    
    private final RSASignature rsaSignature;
    private final PublicKey publicKey;
    
    public LicenseValidator(PublicKey publicKey) {
        this.publicKey = publicKey;
        this.rsaSignature = new RSASignature();
    }
    
    /**
     * 验证 License 完整性和有效性
     * 
     * @param license License 对象
     * @throws LicenseException 验证失败
     */
    public void validate(License license) throws LicenseException {
        if (license == null) {
            throw new LicenseException("License is null");
        }
        
        // 1. 验证签名（防篡改）
        verifySignature(license);
        
        // 2. 验证有效期
        verifyExpiration(license);
        
        // 3. 验证实例数（TODO: 需要分布式锁或本地计数）
        // verifyInstances(license);
        
        // 4. 验证机器绑定（如果启用）
        verifyMachineBinding(license);
    }
    
    /**
     * 验证 RSA 签名
     */
    private void verifySignature(License license) throws LicenseException {
        if (license.getSignature() == null || license.getSignature().isEmpty()) {
            throw new LicenseException("License signature is missing");
        }
        
        String dataToVerify = license.toSignatureData();
        boolean valid = rsaSignature.verify(dataToVerify, license.getSignature(), publicKey);
        
        if (!valid) {
            throw new LicenseException(
                "Invalid license signature - license may be tampered or forged"
            );
        }
    }
    
    /**
     * 验证有效期
     */
    private void verifyExpiration(License license) throws LicenseException {
        Instant now = Instant.now();
        
        if (license.getExpireAt() == null) {
            // 无过期时间，永久有效
            return;
        }
        
        if (now.isAfter(license.getExpireAt())) {
            throw new LicenseException(
                "License expired at " + license.getExpireAt() + 
                " (current time: " + now + ")"
            );
        }
    }
    
    /**
     * 验证机器绑定
     */
    private void verifyMachineBinding(License license) throws LicenseException {
        License.Authorization auth = license.getAuthorization();
        if (auth == null) {
            return;
        }
        
        if (auth.getBindingMode() == License.BindingMode.NONE) {
            // 不需要绑定
            return;
        }
        
        if (auth.getAllowedMachineIds() == null || auth.getAllowedMachineIds().isEmpty()) {
            // 没有指定允许的机器，跳过验证
            return;
        }
        
        // TODO: 获取当前机器 ID 并验证
        // String currentMachineId = MachineFingerprint.getMachineId();
        // if (!auth.getAllowedMachineIds().contains(currentMachineId)) {
        //     throw new LicenseException(
        //         "Machine not authorized. Current: " + currentMachineId
        //     );
        // }
    }
    
    /**
     * 快速验证（仅检查签名和有效期）
     * 
     * @param license License 对象
     * @return 是否有效
     */
    public boolean isValid(License license) {
        try {
            validate(license);
            return true;
        } catch (LicenseException e) {
            return false;
        }
    }
    
    /**
     * 获取 License 信息摘要（用于日志）
     * 
     * @param license License 对象
     * @return 信息字符串
     */
    public String getLicenseInfo(License license) {
        if (license == null) {
            return "No license";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("License Information:\n");
        sb.append("  - License ID: ").append(license.getLicenseId()).append("\n");
        sb.append("  - Type: ").append(license.getLicenseType()).append("\n");
        sb.append("  - Issued to: ").append(license.getIssuedTo()).append("\n");
        sb.append("  - Issued at: ").append(license.getIssuedAt()).append("\n");
        sb.append("  - Expires at: ").append(license.getExpireAt()).append("\n");
        
        if (license.getAuthorization() != null) {
            License.Authorization auth = license.getAuthorization();
            sb.append("  - Max instances: ");
            sb.append(auth.getMaxInstances() == 0 ? "Unlimited" : auth.getMaxInstances());
            sb.append("\n");
            sb.append("  - Binding mode: ").append(auth.getBindingMode()).append("\n");
        }
        
        return sb.toString();
    }
}
