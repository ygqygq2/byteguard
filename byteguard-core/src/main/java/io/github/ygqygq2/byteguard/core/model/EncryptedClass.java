package io.github.ygqygq2.byteguard.core.model;

/**
 * 加密后的类数据
 * 
 * @author ygqygq2
 */
public class EncryptedClass {
    
    private final String className;
    private final byte[] encryptedBytes;
    
    public EncryptedClass(String className, byte[] encryptedBytes) {
        this.className = className;
        this.encryptedBytes = encryptedBytes;
    }
    
    public String getClassName() {
        return className;
    }
    
    public byte[] getEncryptedBytes() {
        return encryptedBytes;
    }
    
    /**
     * 转换类名为资源路径
     * 
     * @return 资源路径（如 com/example/MyClass.class）
     */
    public String getResourcePath() {
        return className.replace('.', '/') + ".class";
    }
}
