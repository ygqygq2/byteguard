package io.github.ygqygq2.byteguard.core.license;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 公钥加载器
 * 
 * <p>从 resources 加载内嵌的 RSA 公钥
 * 
 * @author ygqygq2
 */
public class PublicKeyLoader {
    
    /**
     * 从 resources 加载 RSA 公钥
     * 
     * @return RSA 公钥
     * @throws Exception 加载失败
     */
    public static PublicKey loadEmbeddedPublicKey() throws Exception {
        try (InputStream keyStream = PublicKeyLoader.class
                .getResourceAsStream("/keys/public_key.pem")) {
            
            if (keyStream == null) {
                throw new LicenseException("RSA public key not found in resources");
            }
            
            // 读取 PEM 文件
            String pemContent = new String(keyStream.readAllBytes());
            
            // 移除 PEM 头尾和空白字符
            String publicKeyPEM = pemContent
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            
            // Base64 解码
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
            
            // 生成公钥
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }
    }
}
