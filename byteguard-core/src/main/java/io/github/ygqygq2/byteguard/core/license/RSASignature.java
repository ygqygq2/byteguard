package io.github.ygqygq2.byteguard.core.license;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 数字签名
 * 
 * <p>用于 License 签名和验证，防止篡改
 * 
 * @author ygqygq2
 */
public class RSASignature {
    
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;
    
    /**
     * 生成 RSA 密钥对
     * 
     * @return 密钥对
     * @throws LicenseException 生成失败
     */
    public KeyPair generateKeyPair() throws LicenseException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE, new SecureRandom());
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new LicenseException("Failed to generate RSA key pair", e);
        }
    }
    
    /**
     * 签名数据
     * 
     * @param data 待签名数据
     * @param privateKey 私钥
     * @return Base64编码的签名
     * @throws LicenseException 签名失败
     */
    public String sign(String data, PrivateKey privateKey) throws LicenseException {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes());
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new LicenseException("Failed to sign data", e);
        }
    }
    
    /**
     * 验证签名
     * 
     * @param data 原始数据
     * @param signatureBase64 Base64编码的签名
     * @param publicKey 公钥
     * @return 签名是否有效
     * @throws LicenseException 验证失败
     */
    public boolean verify(String data, String signatureBase64, PublicKey publicKey) throws LicenseException {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            throw new LicenseException("Failed to verify signature", e);
        }
    }
    
    /**
     * 公钥转 Base64
     * 
     * @param publicKey 公钥
     * @return Base64 字符串
     */
    public String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * 私钥转 Base64
     * 
     * @param privateKey 私钥
     * @return Base64 字符串
     */
    public String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * Base64 转公钥
     * 
     * @param base64 Base64 字符串
     * @return 公钥
     * @throws LicenseException 转换失败
     */
    public PublicKey base64ToPublicKey(String base64) throws LicenseException {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new LicenseException("Failed to decode public key", e);
        }
    }
    
    /**
     * Base64 转私钥
     * 
     * @param base64 Base64 字符串
     * @return 私钥
     * @throws LicenseException 转换失败
     */
    public PrivateKey base64ToPrivateKey(String base64) throws LicenseException {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
            return factory.generatePrivate(spec);
        } catch (Exception e) {
            throw new LicenseException("Failed to decode private key", e);
        }
    }
}
