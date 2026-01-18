package io.github.ygqygq2.byteguard.core.license;

/**
 * License 相关异常
 * 
 * @author ygqygq2
 */
public class LicenseException extends Exception {
    
    public LicenseException(String message) {
        super(message);
    }
    
    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
