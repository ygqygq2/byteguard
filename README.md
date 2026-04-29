# ByteGuard

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%2B-brightgreen.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/Gradle-8.11-blue.svg)](https://gradle.org/)

> 🔐 面向现代 Java 的字节码保护工具：类级加密、运行时解密、可工程化集成

## 🎯 什么是 ByteGuard？

ByteGuard 是一个**开源的 Java 字节码保护工具**，用于在 JAR 分发场景下提升 Java 应用的反编译门槛。

它聚焦一件事：把需要保护的类以**类级别加密**方式写入产物中，并在运行时通过 Agent 完成解密与加载。

如果你希望：

- 保护已经打包完成的 Java 应用核心实现
- 用较低接入成本验证“加密后还能否正常运行”
- 再逐步接入构建流程或自动化发布

那么 ByteGuard 的公开仓库就是为这条核心链路准备的。

### 核心特性

- ✅ **类级保护**: 针对 `.class` 逐个加密，适合保护核心业务实现
- ✅ **标准密码学方案**: AES-256-GCM + PBKDF2（100k）+ HKDF
- ✅ **上下文绑定认证**: 使用 GCM AAD 绑定类名，降低密文被错误上下文接受的风险- ✅ **元数据完整性保护**: HMAC-SHA256 保护元数据，防止篡改加密类列表和配置- ✅ **现代 Java 场景**: 已覆盖 Lambda / 方法引用等典型字节码场景
- ✅ **渐进式接入**: 提供 CLI、Java Agent、Maven Plugin，便于从验证到工程集成
- ✅ **边界清晰**: 适合作为反编译防护的一层，而不是宣称“绝对安全”

### 它解决什么问题？

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

也就是说：**你分发出去的是受保护的产物，用户运行时拿到的是恢复后的字节码执行链路。**

## 🚀 5 分钟快速开始

推荐按下面这条最短路径体验：

1. 先用 CLI 加密一个已有 JAR
2. 再用 Agent 启动验证运行
3. 验证没问题后，再考虑 Maven 集成

### 1. 下载 ByteGuard CLI

```bash
# 从 GitHub Releases 下载
wget https://github.com/ygqygq2/byteguard/releases/latest/download/byteguard-cli.jar

# 或从源码构建
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard
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
java -Dbyteguard.license=/path/to/license.lic \
  -javaagent:byteguard-cli.jar=password=your_secure_password \
  -jar your-app-encrypted.jar
```

如果你已将 `license.lic` 放在当前目录或 `~/.byteguard/license.lic`，可省略 `-Dbyteguard.license`。

完成后你可以验证两件事：

- 直接查看产物时，核心类不再以可读字节码形式暴露
- 运行时通过 Agent 正常恢复执行链路

如果这两点都成立，就说明核心加密链路已经跑通。

## 📦 Maven 集成

当你已经确认 CLI + Agent 链路可用后，再接入 Maven 会更稳。

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
| 上下文认证 | GCM AAD 绑定类名 |
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

- 商业软件分发时提升反编译门槛
- 核心算法或业务规则保护
- 希望先做最小侵入式保护，再逐步补齐更多防护
- 需要通过 CLI / Maven 接入构建流程的 Java 项目

### ⚠️ 不适合

- 把它当成单一安全手段的场景
- 对运行时内存明文零暴露有强要求的场景
- 需要对所有动态生成 / 动态加载类做同等保护的场景

## 🔒 项目定位

本公开仓库只提供 **ByteGuard 核心加密功能的开源实现**，包括：

- 类级加密
- 运行时解密
- CLI 工具
- Maven 集成

本文档聚焦公开仓库中已经提供的能力、接入方式和实现边界。

如需商务或定制化沟通，可通过邮箱联系： [ygqygq2@qq.com](mailto:ygqygq2@qq.com)

## 🤝 贡献

欢迎贡献！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

### 开发设置

```bash
# 克隆仓库
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard

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

建议把它放进更完整的工程防护体系中，例如：
- 其他适合你业务场景的防护措施
- 构建与发布过程中的密钥管理
- 必要的服务端校验与审计机制

## 📊 运行特性

- 采用**按需解密**，不会在启动时预先展开全部类
- 每个类派生独立密钥，降低单点暴露影响
- 运行开销与类数量、磁盘性能、JVM 环境和目标应用结构相关
- 建议在目标应用上自行做启动与内存压测，再决定生产参数与保护范围

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
