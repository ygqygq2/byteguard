package io.github.ygqygq2.byteguard.core.crypto;

/**
 * 加密/解密异常
 * 
 * @author ygqygq2
 */
public class CryptoException extends Exception {
    
    public CryptoException(String message) {
        super(message);
    }
    
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
