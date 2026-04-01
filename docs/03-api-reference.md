# ByteGuard API 参考

## 📦 CLI 命令

### encrypt 命令

加密 JAR 文件。

#### 基本用法

```bash
java -jar byteguard-cli.jar encrypt \
  --input <input.jar> \
  --output <output.jar> \
  --password <password>
```

#### 完整选项

```bash
java -jar byteguard-cli.jar encrypt \
  --input app.jar \
  --output app-encrypted.jar \
  --password ${BYTEGUARD_PASSWORD} \
  --packages com.example,com.myapp \
  --exclude com.example.tests \
  --threads 4 \
  --report build/byteguard-report.json
```

| 选项 | 必需 | 说明 | 示例 |
|------|------|------|------|
| `--input` | 是 | 输入 JAR 文件路径 | `app.jar` |
| `--output` | 是 | 输出 JAR 文件路径 | `app-encrypted.jar` |
| `--password` | 否* | 加密密码；未提供时回退读取 `BYTEGUARD_PASSWORD` | `${BYTEGUARD_PASSWORD}` |
| `--packages` | 否 | 要加密的包（逗号分隔） | `com.example,com.myapp` |
| `--exclude` | 否 | 排除的包前缀（逗号分隔） | `com.example.tests` |
| `--threads` | 否 | 并发加密线程数 | `4` |
| `--report` | 否 | 输出 JSON 加密报告 | `build/byteguard-report.json` |

> `*` `--password` 与环境变量 `BYTEGUARD_PASSWORD` 二选一即可。

#### 环境变量

```bash
export BYTEGUARD_PASSWORD="your_secure_password"
java -jar byteguard-cli.jar encrypt --input app.jar --output app-encrypted.jar
```

#### 本地安装

```bash
# 安装到 ~/.local/bin
bash ./scripts/install-cli.sh

# 安装到 /usr/local/bin（可能需要 sudo）
sudo bash ./scripts/install-cli.sh --system
```

安装完成后可直接运行：

```bash
byteguard version
byteguard encrypt --input app.jar --output app-encrypted.jar
```

#### 输出行为

- 默认在终端显示加密进度条和已处理类数量
- 使用 `--report <file>` 可将本次加密摘要写入 JSON 报告
- 当过滤规则未命中任何类时，会明确提示 `Nothing to encrypt`

## 🔌 Maven Plugin

### 基本配置

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
    </execution>
  </executions>
  <configuration>
    <password>${env.BYTEGUARD_PASSWORD}</password>
  </configuration>
</plugin>
```

### 完整配置选项

```xml
<configuration>
  <!-- 必需：加密密码 -->
  <password>${env.BYTEGUARD_PASSWORD}</password>
  
  <!-- 可选：输入文件（默认：${project.build.finalName}.jar） -->
  <input>${project.build.directory}/myapp.jar</input>
  
  <!-- 可选：输出文件（默认：${finalName}-encrypted.jar） -->
  <output>${project.build.directory}/myapp-secure.jar</output>
  
  <!-- 可选：要加密的包 -->
  <packages>
    <package>com.example.core</package>
    <package>com.example.service</package>
  </packages>
  
  <!-- 可选：排除的模式 -->
  <excludes>
    <exclude>**/*Test.class</exclude>
    <exclude>**/TestHelper.class</exclude>
  </excludes>
  
  <!-- 可选：是否替换原始 JAR（默认：false） -->
  <replace>false</replace>
  
  <!-- 可选：是否跳过加密（默认：false） -->
  <skip>false</skip>
</configuration>
```

### Profile 配置

开发环境跳过加密，生产环境启用：

```xml
<profiles>
  <profile>
    <id>dev</id>
    <activation>
      <activeByDefault>true</activeByDefault>
    </activation>
    <properties>
      <byteguard.skip>true</byteguard.skip>
    </properties>
  </profile>
  
  <profile>
    <id>prod</id>
    <properties>
      <byteguard.skip>false</byteguard.skip>
    </properties>
  </profile>
</profiles>
```

使用：

```bash
# 开发模式（不加密）
mvn clean package

# 生产模式（加密）
export BYTEGUARD_PASSWORD="prod_password"
mvn clean package -Pprod
```

## 🚀 JavaAgent 参数

### 基本用法

```bash
java -javaagent:byteguard-cli.jar=password=xxx -jar app.jar
```

### 完整参数

```bash
java -javaagent:byteguard-cli.jar=password=xxx \
  -Dbyteguard.debug=true \
  -jar app.jar
```

| 参数 | 必需 | 说明 | 示例 |
|------|------|------|------|
| `password` | 是 | 解密密码 | `password=xxx` |

### 系统属性

| 属性 | 说明 | 示例 |
|------|------|------|
| `byteguard.debug` | 启用调试日志 | `-Dbyteguard.debug=true` |

### 环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `BYTEGUARD_PASSWORD` | 默认密码 | `export BYTEGUARD_PASSWORD=xxx` |

> **🏢 需要企业级授权管理?**  
> ByteGuard Pro 提供完整的 License 生命周期管理:在线生成、硬件绑定、自动续期、使用统计等。  
> 了解更多:[https://byteguard-pro.ygqygq2.com](https://byteguard-pro.ygqygq2.com)

## 🔧 Java API

### 加密类

```java
import io.github.ygqygy2.byteguard.core.encryptor.ClassEncryptor;
import io.github.ygqygy2.byteguard.core.crypto.KeyDerivation;

// 1. 派生主密钥
KeyDerivation kd = new KeyDerivation();
byte[] salt = kd.generateSalt();
byte[] masterKey = kd.deriveMasterKey("password", salt);

// 2. 加密类
ClassEncryptor encryptor = new ClassEncryptor(masterKey);
byte[] encrypted = encryptor.encrypt("com.example.Main", classBytes);
```

### 解密类

```java
import io.github.ygqygy2.byteguard.core.loader.ClassDecryptor;

// 解密单个类
ClassDecryptor decryptor = new ClassDecryptor(masterKey);
byte[] decrypted = decryptor.decrypt("com.example.Main", encryptedBytes);
```

## 🎯 最佳实践

### 1. 密码管理

**❌ 不要硬编码密码：**

```xml
<!-- 错误 -->
<password>mypassword123</password>
```

**✅ 使用环境变量：**

```xml
<!-- 正确 -->
<password>${env.BYTEGUARD_PASSWORD}</password>
```

```bash
export BYTEGUARD_PASSWORD=$(cat /secure/location/password.txt)
```

### 2. 包选择

**最小化加密范围**（仅核心业务代码）：

```xml
<packages>
  <package>com.example.core</package>
  <package>com.example.business</package>
</packages>
```

**排除测试代码**：

```xml
<excludes>
  <exclude>**/*Test.class</exclude>
  <exclude>**/test/**</exclude>
</excludes>
```

### 3. CI/CD 集成

**GitHub Actions 示例：**

```yaml
- name: Encrypt JAR
  env:
    BYTEGUARD_PASSWORD: ${{ secrets.BYTEGUARD_PASSWORD }}
  run: |
    mvn clean package -Pprod
```

**Jenkins 示例：**

```groovy
withCredentials([string(credentialsId: 'byteguard-password', variable: 'BYTEGUARD_PASSWORD')]) {
    sh 'mvn clean package -Pprod'
}
```

## 📚 相关文档

- [快速开始](01-quick-start.md)
- [架构设计](02-architecture.md)
- [测试指南](04-testing.md)
