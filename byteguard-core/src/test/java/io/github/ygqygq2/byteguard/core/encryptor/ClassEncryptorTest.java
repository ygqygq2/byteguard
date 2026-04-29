package io.github.ygqygq2.byteguard.core.encryptor;

import io.github.ygqygq2.byteguard.core.crypto.CryptoException;
import io.github.ygqygq2.byteguard.core.crypto.KeyDerivation;
import io.github.ygqygq2.byteguard.core.crypto.SaltGenerator;
import io.github.ygqygq2.byteguard.core.model.EncryptedClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassEncryptorTest {

    private byte[] masterKey;
    private ClassEncryptor encryptor;

    @BeforeEach
    void setUp() throws CryptoException {
        byte[] salt = new SaltGenerator().generate();
        masterKey = new KeyDerivation().deriveMasterKey("testpassword", salt);
        encryptor = new ClassEncryptor(masterKey);
    }

    @Test
    void shouldEncryptAndDecrypt() throws CryptoException {
        byte[] original = "Hello, ByteGuard!".getBytes();
        EncryptedClass ec = encryptor.encrypt("com.example.Main", original);

        assertNotNull(ec);
        assertEquals("com.example.Main", ec.getClassName());
        assertNotEquals(original.length, ec.getEncryptedBytes().length); // encrypted is bigger (IV+tag)

        byte[] decrypted = encryptor.decrypt("com.example.Main", ec.getEncryptedBytes());
        assertArrayEquals(original, decrypted);
    }

    @Test
    void shouldProduceDifferentCiphertextsForSameInput() throws CryptoException {
        byte[] original = "Same content".getBytes();
        EncryptedClass ec1 = encryptor.encrypt("com.example.Main", original);
        EncryptedClass ec2 = encryptor.encrypt("com.example.Main", original);

        // IV is random per call, so ciphertexts differ
        assertFalse(java.util.Arrays.equals(ec1.getEncryptedBytes(), ec2.getEncryptedBytes()));
    }

    @Test
    void shouldProduceDifferentKeysPerClass() throws CryptoException {
        byte[] original = "class body".getBytes();
        encryptor.encrypt("com.example.ClassA", original);
        EncryptedClass ecB = encryptor.encrypt("com.example.ClassB", original);

        // Different class keys + AAD 绑定类名上下文 → 交叉解密失败
        assertThrows(CryptoException.class,
            () -> encryptor.decrypt("com.example.ClassA", ecB.getEncryptedBytes()));
    }

    @Test
    void shouldRejectWrongMasterKey() throws CryptoException {
        byte[] original = "secret".getBytes();
        EncryptedClass ec = encryptor.encrypt("com.example.Main", original);

        byte[] wrongKey = new byte[32];
        ClassEncryptor wrongEncryptor = new ClassEncryptor(wrongKey);
        assertThrows(CryptoException.class,
            () -> wrongEncryptor.decrypt("com.example.Main", ec.getEncryptedBytes()));
    }

    @Test
    void shouldRejectNullClassName() {
        byte[] original = "body".getBytes();
        assertThrows(CryptoException.class, () -> encryptor.encrypt(null, original));
    }

    @Test
    void shouldRejectNullBytes() {
        assertThrows(CryptoException.class, () -> encryptor.encrypt("com.example.Main", null));
    }

    @Test
    void shouldRejectWrongMasterKeySize() {
        assertThrows(IllegalArgumentException.class, () -> new ClassEncryptor(new byte[16]));
    }

    @Test
    void shouldReturnCorrectResourcePath() throws CryptoException {
        byte[] original = "body".getBytes();
        EncryptedClass ec = encryptor.encrypt("com.example.sub.MyClass", original);

        assertEquals("com/example/sub/MyClass.class", ec.getResourcePath());
    }
}
