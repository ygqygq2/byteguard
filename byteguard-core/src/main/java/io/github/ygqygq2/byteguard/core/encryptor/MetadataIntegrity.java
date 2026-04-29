package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.crypto.CryptoException;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;
import io.github.ygqygq2.byteguard.core.model.EncryptionMetadata;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 元数据完整性保护。
 *
 * <p>使用从主密钥派生出的独立 HMAC 密钥，对元数据的规范化 JSON 进行完整性校验。
 */
public class MetadataIntegrity {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final KeyDerivation keyDerivation = new KeyDerivation();
    private final MetadataWriter metadataWriter = new MetadataWriter();

    public byte[] computeMac(EncryptionMetadata metadata, byte[] masterKey) throws CryptoException {
        if (metadata == null) {
            throw new CryptoException("Metadata cannot be null");
        }

        try {
            byte[] metadataKey = keyDerivation.deriveMetadataKey(masterKey);
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(metadataKey, HMAC_ALGORITHM));
            byte[] canonicalBytes = metadataWriter.toCanonicalJson(metadata).getBytes(StandardCharsets.UTF_8);
            return hmac.doFinal(canonicalBytes);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Failed to compute metadata MAC", e);
        }
    }

    public boolean verify(EncryptionMetadata metadata, byte[] masterKey) throws CryptoException {
        byte[] actual = metadata.getMetadataMac();
        if (actual == null || actual.length == 0) {
            return false;
        }

        byte[] expected = computeMac(metadata, masterKey);
        return MessageDigest.isEqual(expected, actual);
    }
}