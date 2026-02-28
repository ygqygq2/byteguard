package io.github.ygqygq2.byteguard.core.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyDerivationTest {

    private final KeyDerivation kd = new KeyDerivation();

    @Test
    void shouldDeriveMasterKey() throws CryptoException {
        byte[] salt = new SaltGenerator().generate();
        byte[] key = kd.deriveMasterKey("password123", salt);

        assertNotNull(key);
        assertEquals(32, key.length, "Master key should be 32 bytes (AES-256)");
    }

    @Test
    void shouldProduceDeterministicMasterKey() throws CryptoException {
        byte[] salt = new SaltGenerator().generate();
        byte[] key1 = kd.deriveMasterKey("password", salt);
        byte[] key2 = kd.deriveMasterKey("password", salt);

        assertArrayEquals(key1, key2, "Same password+salt must yield the same key");
    }

    @Test
    void shouldProduceDifferentKeysForDifferentPasswords() throws CryptoException {
        byte[] salt = new SaltGenerator().generate();
        byte[] key1 = kd.deriveMasterKey("password1", salt);
        byte[] key2 = kd.deriveMasterKey("password2", salt);

        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    @Test
    void shouldProduceDifferentKeysForDifferentSalts() throws CryptoException {
        SaltGenerator sg = new SaltGenerator();
        byte[] key1 = kd.deriveMasterKey("password", sg.generate());
        byte[] key2 = kd.deriveMasterKey("password", sg.generate());

        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    @Test
    void shouldRejectNullPassword() {
        byte[] salt = new SaltGenerator().generate();
        assertThrows(CryptoException.class, () -> kd.deriveMasterKey(null, salt));
    }

    @Test
    void shouldRejectShortSalt() {
        assertThrows(CryptoException.class, () -> kd.deriveMasterKey("password", new byte[8]));
    }

    @Test
    void shouldDeriveClassKey() throws CryptoException {
        byte[] masterKey = new byte[32];
        byte[] classKey = kd.deriveClassKey(masterKey, "com.example.Main");

        assertNotNull(classKey);
        assertEquals(32, classKey.length);
    }

    @Test
    void shouldProduceDifferentClassKeysForDifferentClasses() throws CryptoException {
        byte[] masterKey = new byte[32];
        byte[] key1 = kd.deriveClassKey(masterKey, "com.example.ClassA");
        byte[] key2 = kd.deriveClassKey(masterKey, "com.example.ClassB");

        assertFalse(java.util.Arrays.equals(key1, key2));
    }

    @Test
    void shouldRejectNullMasterKey() {
        assertThrows(CryptoException.class, () -> kd.deriveClassKey(null, "com.example.Foo"));
    }

    @Test
    void shouldRejectWrongSizeMasterKey() {
        assertThrows(CryptoException.class, () -> kd.deriveClassKey(new byte[16], "com.example.Foo"));
    }
}
