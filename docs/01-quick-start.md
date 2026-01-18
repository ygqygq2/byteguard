# ByteGuard 快速开始

## 🎯 5 分钟快速体验

### 前置要求

- JDK 8+
- Maven 或 Gradle

### 1. 下载 ByteGuard CLI

```bash
# 下载最新版本
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
  --password yourpassword
```

### 3. 运行加密后的应用

```bash
java -javaagent:byteguard-cli.jar=password=yourpassword \
  -jar your-app-encrypted.jar
```

> **💡 提示**: 开源版本提供强大的 AES-256-GCM 加密引擎。  
> 需要 **GPG 数字签名**、**硬件绑定授权**、**在线 License 管理**?  
> 👉 查看 [ByteGuard Pro](https://byteguard-pro.ygqygq2.com) 企业版功能

## 🔧 Maven 集成

在 `pom.xml` 中添加：

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.ygqygy2</groupId>
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

运行应用时，ByteGuard Agent 会动态解密类文件。

## 🆘 故障排查

### 问题 1: `ClassNotFoundException`

- 可能某些类未正确加密
- 检查是否排除了必要的类
- 查看 `META-INF/byteguard-metadata.json`

## 📖 下一步

- [架构设计](02-architecture.md) - 了解加密原理
- [API 参考](03-api-reference.md) - 详细配置选项
- [测试指南](04-testing.md) - 完整测试流程
