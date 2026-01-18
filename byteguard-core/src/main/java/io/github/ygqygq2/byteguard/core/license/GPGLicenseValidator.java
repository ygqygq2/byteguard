package io.github.ygqygq2.byteguard.core.license;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;
import java.util.Iterator;

public class GPGLicenseValidator {
    
    private static final String TRUSTED_GPG_FINGERPRINT = "54446D97EAD0EAF000830AC0276B25461FCE9C7C";
    
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    
    public static String verifyAndExtract(String licenseContent) throws LicenseException {
        try {
            //  1. 手动提取 cleartext（去掉 dash-escaping）
            String clearText = extractClearText(licenseContent);
            
            // 2. 提取 Base64
            String base64 = extractBase64(clearText);
            if (base64 == null) {
                throw new LicenseException("Invalid ByteGuard license format");
            }
            
            // 3. 读取签名
            InputStream in = new ByteArrayInputStream(licenseContent.getBytes(StandardCharsets.UTF_8));
            ArmoredInputStream aIn = new ArmoredInputStream(in);
            
            // 跳过 cleartext
            while (aIn.isClearText()) {
                aIn.read();
            }
            
            JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(aIn);
            PGPSignatureList p3 = (PGPSignatureList) pgpFact.nextObject();
            PGPSignature sig = p3.get(0);
            
            // 4. 加载公钥
            PGPPublicKey publicKey = loadEmbeddedPublicKey();
            String keyFingerprint = bytesToHex(publicKey.getFingerprint());
            
            if (!TRUSTED_GPG_FINGERPRINT.equalsIgnoreCase(keyFingerprint)) {
                throw new LicenseException(
                    "GPG key fingerprint mismatch. Expected: " + TRUSTED_GPG_FINGERPRINT +
                    ", Got: " + keyFingerprint
                );
            }
            
            // 5. 验证签名（保持原始格式 - LF 或 CRLF）
            sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);
            
            // 直接用 clearText，不修改行尾
            byte[] clearBytes = clearText.getBytes(StandardCharsets.UTF_8);
            sig.update(clearBytes);
            
            if (!sig.verify()) {
                throw new LicenseException("GPG signature verification failed");
            }
            
            // 6. Base64 解码
            byte[] jsonBytes = Base64.getDecoder().decode(base64);
            return new String(jsonBytes, StandardCharsets.UTF_8);
            
        } catch (LicenseException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseException("Failed to verify GPG signature: " + e.getMessage(), e);
        }
    }
    
    private static String extractClearText(String licenseContent) throws LicenseException {
        String[] lines = licenseContent.split("\\r?\\n");
        StringBuilder clearText = new StringBuilder();
        boolean inClearText = false;
        
        for (String line : lines) {
            if (line.startsWith("-----BEGIN PGP SIGNED MESSAGE-----")) {
                inClearText = false;  // 等待 Hash: 行后的空行
                continue;
            }
            
            if (!inClearText && line.trim().isEmpty()) {
                inClearText = true;  // 空行后开始 cleartext
                continue;
            }
            
            if (line.startsWith("-----BEGIN PGP SIGNATURE-----")) {
                break;
            }
            
            if (inClearText) {
                // 去掉 dash-escaping
                if (line.startsWith("- ")) {
                    clearText.append(line.substring(2)).append('\n');
                } else {
                    clearText.append(line).append('\n');
                }
            }
        }
        
        // 去掉最后一个多余的 \n
        if (clearText.length() > 0 && clearText.charAt(clearText.length() - 1) == '\n') {
            clearText.setLength(clearText.length() - 1);
        }
        
        return clearText.toString();
    }
    
    private static String extractBase64(String licenseBlock) {
        String[] lines = licenseBlock.split("\\r?\\n");
        StringBuilder base64 = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }
            
            // 跳过可能的标记（兼容不同格式）
            if (line.startsWith("-----") || 
                line.startsWith("Version:") || 
                line.startsWith("Format:")) {
                continue;
            }
            
            // 提取 Base64 行（允许行尾等号）
            if (line.matches("[A-Za-z0-9+/=]+")) {
                base64.append(line);
            }
        }
        
        return base64.length() > 0 ? base64.toString() : null;
    }

    private static PGPPublicKey loadEmbeddedPublicKey() throws Exception {
        try (InputStream keyStream = GPGLicenseValidator.class
                .getResourceAsStream("/keys/gpg_public_key.asc")) {
            
            if (keyStream == null) {
                throw new LicenseException("GPG public key not found in resources");
            }
            
            ArmoredInputStream armoredIn = new ArmoredInputStream(keyStream);
            PGPPublicKeyRingCollection keyRingCollection = new PGPPublicKeyRingCollection(
                armoredIn,
                new org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator()
            );
            
            Iterator<PGPPublicKeyRing> keyRings = keyRingCollection.getKeyRings();
            while (keyRings.hasNext()) {
                PGPPublicKeyRing keyRing = keyRings.next();
                PGPPublicKey masterKey = keyRing.getPublicKey();
                if (masterKey != null) {
                    return masterKey;
                }
            }
            
            throw new LicenseException("No suitable GPG public key found");
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
