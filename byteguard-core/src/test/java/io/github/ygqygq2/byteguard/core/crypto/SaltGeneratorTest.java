package io.github.ygqygq2.byteguard.core.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaltGeneratorTest {

    private final SaltGenerator generator = new SaltGenerator();

    @Test
    void shouldGenerateDefaultSalt() {
        byte[] salt = generator.generate();
        assertNotNull(salt);
        assertEquals(SaltGenerator.DEFAULT_SALT_LENGTH, salt.length);
    }

    @Test
    void shouldGenerateCustomLengthSalt() {
        byte[] salt = generator.generate(64);
        assertNotNull(salt);
        assertEquals(64, salt.length);
    }

    @Test
    void shouldGenerateRandomSalts() {
        byte[] salt1 = generator.generate();
        byte[] salt2 = generator.generate();
        assertFalse(java.util.Arrays.equals(salt1, salt2), "Two salts should be different");
    }

    @Test
    void shouldRejectTooShortLength() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(8));
    }

    @Test
    void shouldAcceptMinimumLength() {
        byte[] salt = generator.generate(16);
        assertEquals(16, salt.length);
    }
}
