# ByteGuard Development Guide

## Prerequisites

- JDK 8+ (推荐 JDK 17)
- Git
- 不需要安装 Gradle（使用 Gradle Wrapper）

## Project Setup

```bash
# 克隆仓库
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard

# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 生成 JAR
./gradlew jar
```

## Project Structure

```
byteguard/
├── byteguard-core/          # 核心库
│   └── src/
│       ├── main/java/       # 源代码
│       └── test/java/       # 测试代码
├── byteguard-cli/           # CLI 工具
│   └── src/main/java/
├── byteguard-maven-plugin/  # Maven 插件
│   └── src/main/java/
├── docs/                    # 文档
├── gradle/                  # Gradle Wrapper
├── build.gradle.kts         # 根构建文件
└── settings.gradle.kts      # 项目设置
```

## Development Tasks

### Build

```bash
# 完整构建
./gradlew build

# 跳过测试
./gradlew build -x test

# 清理构建
./gradlew clean
```

### Testing

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :byteguard-core:test

# 生成测试报告
./gradlew test jacocoTestReport
```

### Run

```bash
# 运行 CLI (加密)
./gradlew :byteguard-cli:run --args="encrypt --help"

# 直接运行 JAR
java -jar byteguard-cli/build/libs/byteguard-cli-1.0.0-SNAPSHOT.jar
```

## Implementation Roadmap

### Phase 1: Core Crypto (Week 1-2)

实现 `byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/crypto/`:

1. **SaltGenerator.java**
```java
public class SaltGenerator {
    public static byte[] generate() {
        // 使用 SecureRandom 生成 32 字节盐
    }
}
```

2. **KeyDerivation.java**
```java
public class KeyDerivation {
    public static byte[] deriveKeyPBKDF2(char[] password, byte[] salt) {
        // PBKDF2WithHmacSHA256, 100k 迭代
    }
    
    public static byte[] deriveClassKey(byte[] masterKey, String className) {
        // HKDF 派生每类密钥
    }
}
```

3. **AESGCMCipher.java**
```java
public class AESGCMCipher {
    public static byte[] encrypt(byte[] plaintext, byte[] key) {
        // AES-256-GCM 加密，返回 [IV|密文|TAG]
    }
    
    public static byte[] decrypt(byte[] ciphertext, byte[] key) {
        // AES-256-GCM 解密，验证 TAG
    }
}
```

### Phase 2: Encryptor (Week 3)

实现 `byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/encryptor/`:

1. **ClassEncryptor.java**: 单个类加密
2. **JarEncryptor.java**: JAR 文件加密
3. **EncryptionMetadata.java**: 元数据模型

### Phase 3: Loader (Week 4)

实现 `byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/loader/`:

1. **DecryptingClassLoader.java**: 自定义 ClassLoader
2. **ClassCache.java**: LRU 缓存
3. **ByteGuardAgent.java**: JavaAgent 入口

### Phase 4: CLI (Week 5)

实现 `byteguard-cli/src/main/java/io/github/ygqygq2/byteguard/cli/`:

1. **Main.java**: CLI 入口
2. **EncryptCommand.java**: 加密命令
3. **AgentCommand.java**: Agent 模式

## Coding Standards

### Java Style

- 使用 Java 8 语法
- 遵循 Google Java Style Guide
- 变量命名：驼峰命名法
- 常量命名：全大写 + 下划线

### Testing

- 单元测试覆盖率 > 80%
- 使用 JUnit 5 + AssertJ
- 每个公共方法至少一个测试

### Documentation

- 公共 API 必须有 Javadoc
- 复杂逻辑添加注释
- README 保持更新

## Debugging

### Enable Debug Logging

```bash
# CLI 调试
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG \
  -jar byteguard-cli.jar encrypt ...

# Agent 调试
java -javaagent:byteguard-cli.jar=-Ddebug=true \
  -jar encrypted-app.jar
```

### Common Issues

1. **ClassNotFoundException**: 检查类名是否在元数据中
2. **BadPaddingException**: 密码错误或文件损坏
3. **OutOfMemoryError**: 增加 JVM 内存 `-Xmx2g`

## Release Process

```bash
# 1. 更新版本号
# 编辑 build.gradle.kts: version = "1.0.0"

# 2. 构建发布
./gradlew build

# 3. 发布到 Maven Central
./gradlew publish

# 4. 创建 Git tag
git tag v1.0.0
git push origin v1.0.0
```

## Resources

- [Gradle User Guide](https://docs.gradle.org/)
- [JCA Reference Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html)
- [NIST Cryptographic Standards](https://csrc.nist.gov/)
