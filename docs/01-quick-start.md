# ByteGuard 快速开始

## 🎯 5 分钟快速体验

这份 Quick Start 只解决一个问题：**怎样在最短时间内确认 ByteGuard 的核心加密链路对你的 JAR 是可用的。**

### 前置要求

- JDK 8+
- Maven 或 Gradle
- 运行验证时准备有效的 `license.lic`

完成本页后，你应该能确认两件事：

- 你的 JAR 可以被成功加密
- 加密后的应用可以通过 Agent 正常启动

### 1. 下载 ByteGuard CLI

```bash
# 下载最新版本
wget https://github.com/ygqygq2/byteguard/releases/latest/download/byteguard-cli.jar

# 或从源码构建
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard
./gradlew :byteguard-cli:jar
```

### 可选：安装为本地命令

```bash
# 安装到 ~/.local/bin/byteguard
bash ./scripts/install-cli.sh

# 或安装到自定义目录
bash ./scripts/install-cli.sh --prefix /tmp/byteguard-install

# 或仅生成分发目录（含 Unix / Windows 启动脚本）
./gradlew :byteguard-cli:installDist
```

安装后可直接使用：

```bash
byteguard encrypt --input your-app.jar --output your-app-encrypted.jar --password yourpassword
```

如果你只是第一次验证，直接使用 `java -jar byteguard-cli.jar` 也完全可以，不必先安装到系统路径。

源码构建后的启动脚本位于：

- `byteguard-cli/build/install/byteguard-cli/bin/byteguard-cli`
- `byteguard-cli/build/install/byteguard-cli/bin/byteguard-cli.bat`

### 2. 加密你的 JAR

```bash
java -jar byteguard-cli.jar encrypt \
  --input your-app.jar \
  --output your-app-encrypted.jar \
  --password yourpassword
```

### 3. 运行加密后的应用

```bash
java -Dbyteguard.license=/path/to/license.lic \
  -javaagent:byteguard-cli.jar=password=yourpassword \
  -jar your-app-encrypted.jar
```

如果 `license.lic` 已放在当前目录或 `~/.byteguard/license.lic`，可省略 `-Dbyteguard.license`。

> **💡 提示**: 本页只聚焦公开仓库中核心加密链路的接入与验证方式。

到这一步，如果应用能正常启动，就说明核心链路已经成立；后续再进入 Maven 集成会更稳。

## 🔧 Maven 集成

在 `pom.xml` 中添加：

```xml
<build>
  <plugins>
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
            <packages>
              <package>com.yourcompany</package>
            </packages>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

运行：

```bash
export BYTEGUARD_PASSWORD="yourpassword"
mvn clean package
```

加密后的 JAR 位于 `target/your-app-encrypted.jar`

## ✅ 验证加密

加密成功后，尝试直接反编译：

```bash
jar xf your-app-encrypted.jar
javap com/yourcompany/YourClass.class
# 输出：加密的字节码（无法读取）
```

运行应用时，ByteGuard Agent 会先校验 License，再动态解密类文件。

## 🆘 故障排查

### 问题 1: `ClassNotFoundException`

- 可能某些类未正确加密
- 检查是否排除了必要的类
- 查看 `META-INF/byteguard-metadata.json`

## 📖 下一步

- [架构设计](02-architecture.md) - 了解密钥派生、类加密和运行时解密原理
- [API 参考](03-api-reference.md) - 查看 CLI、Maven Plugin 和 Agent 参数
- [测试指南](04-testing.md) - 给自己的改动补上验证步骤
