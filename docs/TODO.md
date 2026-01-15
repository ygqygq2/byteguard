# ByteGuard Implementation Checklist

## 项目状态: 初始化完成 ✅

### ✅ 已完成

- [x] 项目结构创建
- [x] Gradle 配置
- [x] README.md
- [x] 架构文档
- [x] 开发指南
- [x] Apache 2.0 License

### 📋 待实现 (按优先级)

## Milestone 1: Core Crypto Module

**目标**: 实现核心加密解密功能

### byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/

#### crypto/ 包
- [ ] `SaltGenerator.java` - 生成安全随机盐
- [ ] `KeyDerivation.java` - PBKDF2 + HKDF 密钥派生
- [ ] `AESGCMCipher.java` - AES-256-GCM 加密解密
- [ ] `EncryptionMetadata.java` - 元数据模型
- [ ] `PasswordHasher.java` - Argon2id 密码哈希

#### model/ 包
- [ ] `EncryptedClass.java` - 加密类模型
- [ ] `EncryptionConfig.java` - 加密配置模型

#### util/ 包
- [ ] `ByteUtils.java` - 字节数组工具
- [ ] `Base64Utils.java` - Base64 编解码

### 测试
- [ ] `SaltGeneratorTest.java`
- [ ] `KeyDerivationTest.java`
- [ ] `AESGCMCipherTest.java`
- [ ] `PasswordHasherTest.java`

---

## Milestone 2: Encryptor

**目标**: 实现 JAR 文件加密

### byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/encryptor/

- [ ] `ClassEncryptor.java` - 单个类加密
- [ ] `JarEncryptor.java` - JAR 文件加密
- [ ] `EncryptionStrategy.java` - 加密策略接口
- [ ] `MetadataWriter.java` - 元数据写入

### byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/analyzer/

- [ ] `ClassAnalyzer.java` - 类文件分析
- [ ] `JarAnalyzer.java` - JAR 文件分析

### 测试
- [ ] `ClassEncryptorTest.java`
- [ ] `JarEncryptorTest.java`
- [ ] 创建测试用 JAR 文件

---

## Milestone 3: Loader & Agent

**目标**: 实现运行时解密

### byteguard-core/src/main/java/io/github/ygqygq2/byteguard/core/loader/

- [ ] `DecryptingClassLoader.java` - 自定义类加载器
- [ ] `ClassCache.java` - LRU 缓存 (使用 Caffeine)
- [ ] `EncryptedClassRepository.java` - 加密类仓库
- [ ] `MetadataReader.java` - 元数据读取

### byteguard-cli/src/main/java/io/github/ygqygq2/byteguard/agent/

- [ ] `ByteGuardAgent.java` - JavaAgent 入口
- [ ] `AgentTransformer.java` - 类转换器

### 测试
- [ ] `DecryptingClassLoaderTest.java`
- [ ] `ClassCacheTest.java`
- [ ] Agent 集成测试

---

## Milestone 4: CLI Tool

**目标**: 实现命令行工具

### byteguard-cli/src/main/java/io/github/ygqygq2/byteguard/cli/

- [ ] `Main.java` - CLI 入口
- [ ] `EncryptCommand.java` - 加密命令
- [ ] `VerifyCommand.java` - 验证命令
- [ ] `VersionCommand.java` - 版本命令

### byteguard-cli/src/main/java/io/github/ygqygq2/byteguard/cli/option/

- [ ] `EncryptOptions.java` - 加密选项
- [ ] `PasswordProvider.java` - 密码提供者

### 测试
- [ ] CLI 集成测试
- [ ] 端到端测试

---

## Milestone 5: Maven Plugin

**目标**: Maven 构建集成

### byteguard-maven-plugin/src/main/java/io/github/ygqygq2/byteguard/maven/

- [ ] `ByteGuardMojo.java` - Maven 插件主类
- [ ] Plugin descriptor 配置

---

## 开发顺序建议

### Week 1-2: 核心加密
1. 实现 `SaltGenerator`
2. 实现 `KeyDerivation` (PBKDF2)
3. 实现 `AESGCMCipher`
4. 实现 `PasswordHasher` (Argon2id)
5. 编写单元测试

### Week 3: 加密器
1. 实现 `ClassEncryptor`
2. 实现 `JarEncryptor`
3. 实现 `EncryptionMetadata`
4. 创建测试 JAR

### Week 4: 类加载器
1. 实现 `DecryptingClassLoader`
2. 实现 `ClassCache`
3. 实现 `ByteGuardAgent`
4. 集成测试

### Week 5: CLI 工具
1. 实现 `Main` 和命令
2. 集成 Picocli
3. 端到端测试

### Week 6: 打磨
1. 性能优化
2. 文档完善
3. 示例项目
4. 发布准备

---

## 当前优先级

**现在开始**: Milestone 1 - Core Crypto Module

**第一个任务**: 实现 `SaltGenerator.java`

```java
package io.github.ygqygq2.byteguard.core.crypto;

import java.security.SecureRandom;

public class SaltGenerator {
    private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits
    
    public static byte[] generate() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
}
```

**测试文件**: `SaltGeneratorTest.java`

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SaltGeneratorTest {
    @Test
    void shouldGenerateSaltOfCorrectLength() {
        byte[] salt = SaltGenerator.generate();
        assertThat(salt).hasSize(32);
    }
    
    @Test
    void shouldGenerateRandomSalts() {
        byte[] salt1 = SaltGenerator.generate();
        byte[] salt2 = SaltGenerator.generate();
        assertThat(salt1).isNotEqualTo(salt2);
    }
}
```

---

## 下一步行动

用 VSCode 打开 `/data/git/ygqygq2/classfinal/byteguard`，然后：

1. 查看 `docs/` 目录理解架构
2. 开始实现 `byteguard-core` 模块
3. 从 `SaltGenerator` 开始
4. 逐步完成 Milestone 1

**需要帮助时**: 查看 `docs/development.md` 和 `docs/architecture.md`
