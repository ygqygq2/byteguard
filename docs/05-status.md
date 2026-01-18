# ByteGuard 开发状态

> 最后更新: 2026-01-16

## 📊 当前进度

### ✅ 已完成功能

#### 1. 核心加密引擎
- ✅ AES-256-GCM 加密/解密 (`AESGCMCipher.java`)
- ✅ PBKDF2 密钥派生 (100,000 次迭代)
- ✅ HKDF 类专用密钥派生 (`KeyDerivation.java`)
- ✅ 零外部依赖（纯 JDK Crypto API）

#### 2. License 系统
- ✅ License 数据模型 (`License.java`)
  - TRIAL / STANDARD / ENTERPRISE 三种类型
  - 机器绑定模式：NONE / OPTIONAL / STRICT
  - 支持自定义特性列表
- ✅ RSA-2048 数字签名 (`RSASignature.java`)
- ✅ License 验证器 (`LicenseValidator.java`)
- ✅ JSON 序列化/反序列化 (`LicenseSerializer.java`)

#### 3. ClassLoader
- ✅ 自定义 ClassLoader (`DecryptingClassLoader.java`)
- ✅ 类解密器 (`ClassDecryptor.java`)
- ✅ LRU 缓存 (ConcurrentHashMap, 1000 entries)

#### 4. CLI 工具
- ✅ 命令行框架 (`Main.java`)
- ✅ JAR 加密命令 (`EncryptCommand.java`)
  - 读取 JAR 文件
  - 扫描 .class 文件
  - 加密每个类（派生独立密钥）
  - 存储到 `META-INF/.encrypted/`
  - 生成元数据 `META-INF/.byteguard/metadata.json`
- ✅ License 生成命令 (`LicenseCommand.java`)
  - 生成 License ID
  - RSA 签名
  - JSON 格式保存

#### 5. JavaAgent
- ✅ Agent 入口实现 (`ByteGuardAgent.java`)
- ✅ 参数解析 (password=xxx)
- ✅ 配置加载框架
- ⏳ License 验证集成（TODO）
- ⏳ ClassLoader 注入（TODO）

#### 6. 测试验证
- ✅ 编译通过：21 个 .class 文件
- ✅ JAR 打包：byteguard.jar (36KB)
- ✅ 加密测试：成功加密 simple-app.jar (3 classes)
- ✅ License 生成：成功生成 license.lic
- ✅ 元数据验证：JSON 格式正确

### 🚧 进行中

#### JavaAgent 完善
- [ ] 从 JAR 读取加密元数据
- [ ] License 文件查找和加载
- [ ] 集成 DecryptingClassLoader
- [ ] 处理 ClassNotFoundException

### 📋 待开发

#### 高优先级
1. **JavaAgent 运行时验证**
   - [ ] 完整的加密 JAR 运行测试
   - [ ] 性能测试（启动时间、内存占用）
   
2. **Maven 插件**
   - [ ] 创建 byteguard-maven-plugin 模块
   - [ ] 集成到构建流程

3. **文档完善**
   - [ ] 用户手册（示例、FAQ）
   - [ ] API 文档（JavaDoc）

#### 中优先级
4. **高级功能**
   - [ ] 包选择性加密 (--packages)
   - [ ] 排除规则 (--exclude)
   - [ ] 机器绑定验证
   - [ ] License 实例数限制

5. **工具增强**
   - [ ] Gradle 插件
   - [ ] License 密钥对管理工具
   - [ ] 解密性能分析工具

#### 低优先级
6. **优化**
   - [ ] ClassDecryptor 缓存策略优化
   - [ ] 并行加密（多线程）
   - [ ] 元数据压缩

7. **扩展**
   - [ ] License 服务器集成
   - [ ] 云端 License 验证
   - [ ] 监控和日志系统

## 📈 代码统计

```
Java 源文件:      16 个
编译 .class 文件: 21 个 (含内部类)
JAR 大小:         36 KB
测试应用:         2 个 (simple-app, lambda-app)
```

### 模块结构
```
byteguard-core/
  ├── crypto/          (3 files) - 加密引擎
  ├── license/         (5 files) - License 系统  
  ├── loader/          (2 files) - ClassLoader
  └── model/           (2 files) - 数据模型

byteguard-cli/
  ├── cli/             (1 file)  - 主程序
  ├── cli/command/     (2 files) - 命令实现
  └── agent/           (1 file)  - JavaAgent

byteguard-maven-plugin/
  └── (未开发)
```

## 🎯 下一步计划

1. **完成 JavaAgent 集成** (1-2 小时)
   - 从加密 JAR 读取元数据
   - 实例化 DecryptingClassLoader
   - 测试运行加密后的 simple-app

2. **创建集成测试** (1 小时)
   - 端到端加密→运行流程
   - 验证 Lambda、方法引用等特性

3. **Maven 插件开发** (2-3 小时)
   - 基础框架
   - 集成到 pom.xml

4. **文档和示例** (2 小时)
   - README 完善
   - 快速开始指南
   - FAQ

## 🔍 已知问题

- [ ] JavaAgent 尚未完整集成
- [ ] 机器绑定功能未实现
- [ ] License 实例数限制未实现
- [ ] 缺少单元测试

## 📝 技术决策记录

### 选择 License 文件而非硬件绑定
**原因:** 
- 用户体验更好（云环境友好）
- 灵活性高（支持多台机器）
- 可选机器绑定（三种模式）

### 零依赖设计
**原因:**
- 减少依赖冲突
- 提高安全性（无外部库漏洞）
- 简化部署

### JSON 手动解析
**原因:**
- 避免引入 Gson/Jackson
- License 数据结构简单
- 性能足够

### PBKDF2 + HKDF 双重密钥派生
**原因:**
- PBKDF2: 防止密码爆破
- HKDF: 每个类独立密钥（增加安全性）

## 🚀 发布计划

### v0.1.0 (MVP)
- [x] 核心加密功能
- [x] License 生成
- [ ] JavaAgent 运行时解密
- [ ] 基础文档

### v0.2.0
- [ ] Maven 插件
- [ ] 完整文档
- [ ] 集成测试

### v1.0.0
- [ ] 所有核心功能完成
- [ ] 性能优化
- [ ] 生产环境验证
