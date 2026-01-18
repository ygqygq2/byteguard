# ByteGuard

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%2B-brightgreen.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/Gradle-8.11-blue.svg)](https://gradle.org/)

> 🔐 现代 Java 字节码加密引擎 - 军事级安全 + 100% Lambda 支持

## 🎯 什么是 ByteGuard？

ByteGuard 是一个**开源的 Java 字节码加密库**，为你的 Java 应用提供类级别的加密保护。

### 核心特性

- ✅ **现代 Java 支持**: Lambda、方法引用、Record、Sealed Classes
- ✅ **军事级加密**: AES-256-GCM + PBKDF2 (100k 迭代) + HKDF
- ✅ **零依赖**: 纯 Java Crypto API
- ✅ **高性能**: 启动开销 < 200ms，LRU 缓存，按需解密
- ✅ **易于集成**: CLI 工具 + Maven Plugin

### 它能做什么？

```java
// 原始代码
public class BusinessLogic {
    private String secretAlgorithm() {
        return "My secret sauce";
    }
}
```

加密后，反编译器看到的：
```
// 加密的字节码（无法读取）
[Encrypted bytecode: 0x7A 0x8F 0x3E ...]
```

运行时，ByteGuard Agent 会：
1. ✅ 验证密码
2. ✅ 动态解密类文件
3. ✅ 正常执行你的代码

## 🚀 5 分钟快速开始

### 1. 下载 ByteGuard CLI

```bash
# 从 GitHub Releases 下载
wget https://github.com/ygqygq2/byteguard/releases/latest/download/byteguard-cli.jar

# 或从源码构建
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard/byteguard
./gradlew :byteguard-cli:jar
```

### 2. 加密你的 JAR

```bash
java -jar byteguard-cli.jar encrypt \
  --input your-app.jar \
  --output your-app-encrypted.jar \
  --password your_secure_password
```

### 3. 运行加密后的应用

```bash
java -javaagent:byteguard-cli.jar=password=your_secure_password \
  -jar your-app-encrypted.jar
```

就这么简单！你的代码现在受到 AES-256-GCM 保护。

## 📦 Maven 集成

在 `pom.xml` 中添加：

```xml
<plugin>
  <groupId>io.github.ygqygq2</groupId>
  <artifactId>byteguard-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>encrypt</goal>
      </goals>
      <configuration>
        <password>${env.BYTEGUARD_PASSWORD}</password>
      </configuration>
    </execution>
  </executions>
</plugin>
```

构建时自动加密：

```bash
export BYTEGUARD_PASSWORD="your_password"
mvn clean package
```

## 🏗️ 架构概览

```
Password → PBKDF2 (100k) → Master Key → HKDF → Per-Class Key
                                              ↓
                                       AES-256-GCM
                                              ↓
                                  [IV + Ciphertext + TAG]
```

### 安全特性

| 特性 | 实现 |
|------|------|
| 加密算法 | AES-256-GCM (认证加密) |
| 密钥派生 | PBKDF2-SHA256 (100,000 迭代) |
| 每类独立密钥 | HKDF-SHA256 (类名作为 context) |
| 随机化 | 每 JAR 唯一 salt，每类唯一 IV |
| 完整性保护 | GCM 认证标签 (防篡改) |

## 🛠️ 项目结构

```
byteguard/
├── byteguard-core/          # 核心加密引擎
│   ├── crypto/              # AES-GCM, PBKDF2, HKDF
│   ├── encryptor/           # 类加密器
│   └── loader/              # 类解密器
├── byteguard-cli/           # 命令行工具 + JavaAgent
├── byteguard-maven-plugin/  # Maven 插件
└── docs/                    # 文档
```

## 📚 文档

- [快速开始](docs/01-quick-start.md) - 详细使用指南
- [架构设计](docs/02-architecture.md) - 加密原理和安全分析
- [API 参考](docs/03-api-reference.md) - 完整配置选项
- [测试指南](docs/04-testing.md) - 端到端测试

## 🌟 应用场景

### ✅ 适合

- 商业软件保护（防止反编译）
- 核心算法保护
- 知识产权保护
- 客户端软件分发

### ⚠️ 不适合

- 安全关键应用（仅作为辅助手段）
- 需要代码审计的场景
- 高频动态类加载

## 🔒 企业级增强

ByteGuard 开源版本提供核心加密功能。企业级增强请参考：

- **ByteGuard Pro**: 反调试、代码混淆、完整性检查
- **ByteGuard Website**: 在线加密服务、用户管理
- **License Server**: GPG License 管理、机器绑定

联系: [contact@example.com](mailto:contact@example.com)

## 🤝 贡献

欢迎贡献！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

### 开发设置

```bash
# 克隆仓库
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard/byteguard

# 构建
./gradlew build

# 运行测试
./gradlew test

# 生成 JAR
./gradlew :byteguard-cli:jar
```

## 📄 许可证

Apache License 2.0 - 详见 [LICENSE](LICENSE) 文件

## ⚠️ 免责声明

ByteGuard 是**防御性工具**，用于保护合法软件的知识产权。不保证 100% 安全，仅作为多层防护的一部分。

建议配合使用：
- 代码混淆
- 运行时完整性检查
- License 验证
- 服务端验证

## 📊 性能基准

| 场景 | 开销 |
|------|------|
| 首次启动 (100 类) | ~150ms |
| 单类解密 | ~2ms |
| 内存占用 | < 1MB (元数据) |
| JAR 体积 | +5-10% |

## 🙏 致谢

- NIST 加密标准
- OWASP 安全最佳实践
- Bouncy Castle 密码库

## 📧 联系方式

- 作者: ygqygq2
- GitHub: https://github.com/ygqygq2/byteguard
- 问题反馈: https://github.com/ygqygq2/byteguard/issues

---

**⭐ 如果觉得有用，请给个 Star！**
