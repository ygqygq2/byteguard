# ByteGuard 文档索引

欢迎使用 ByteGuard - 现代 Java 字节码加密引擎

## 📖 文档导航

### 快速开始

1. **[快速开始](01-quick-start.md)** - 5 分钟上手 ByteGuard
   - CLI 工具使用
   - Maven 集成
   - 基础故障排查

### 深入了解

2. **[架构设计](02-architecture.md)** - 了解加密原理
   - 整体架构和职责划分
   - 密钥派生机制（PBKDF2 + HKDF）
   - 加密流程（AES-256-GCM）
   - 运行时解密（JavaAgent）
   - 性能优化
   - 安全分析

3. **[API 参考](03-api-reference.md)** - 完整配置选项
   - CLI 命令参考
   - Maven Plugin 配置
   - JavaAgent 参数
   - Java API 使用
   - 最佳实践

4. **[测试指南](04-testing.md)** - 端到端测试
   - 快速测试流程
   - 手动测试步骤
   - 故障排查

5. **[项目状态](05-status.md)** - 开发进度
   - 已完成功能
   - 正在开发
   - 路线图

## 🎯 根据场景选择

### 我想快速体验
→ 阅读 [快速开始](01-quick-start.md)

### 我想了解加密原理
→ 阅读 [架构设计](02-architecture.md)

### 我要集成到 Maven 项目
→ 阅读 [API 参考](03-api-reference.md)

### 我遇到了问题
→ 查看 [测试指南 - 故障排查](04-testing.md)

## 🌟 ByteGuard 生态

ByteGuard 开源版本专注于**核心加密引擎**。

**🎯 升级到企业版获得更多保护:**

| 模块 | 类型 | 功能 |
|------|------|------|
| **byteguard** | 🌐 公开 | 核心加密引擎（本文档） |
| **byteguard-website** | 🔒 私有 | 在线加密服务 + License 管理平台 |
| **byteguard-pro** | 🔒 私有 | 反调试、代码混淆、完整性检查等高级保护 |

**企业版亮点功能:**
- **GPG 数字签名验证**: 确保 JAR 文件来源可信
- **硬件绑定授权**: CPU/主板序列号绑定，防止授权滥用
- **在线 License 管理**: 自动生成、续期、吊销
- **反调试保护**: 检测并阻止 JDB、JDWP 等调试工具
- **代码混淆**: 字符串加密、控制流平坦化
- **运行时完整性检查**: 实时监控代码篡改

**了解更多**: [https://byteguard-pro.ygqygq2.com](https://byteguard-pro.ygqygq2.com)

---

**最后更新**: 2026-01-18
