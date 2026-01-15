# ByteGuard

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%2B-brightgreen.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/Gradle-8.5-blue.svg)](https://gradle.org/)

> Modern Java bytecode encryption with 100% Lambda support and military-grade security.

## 🎯 Why ByteGuard?

ByteGuard is a next-generation Java bytecode encryption tool designed from the ground up to support modern Java features:

- ✅ **100% Coverage**: Encrypts all classes including Lambda, Method References, Records, Sealed Classes
- ✅ **Military-Grade Security**: AES-256-GCM + PBKDF2 (100k iterations) + HKDF per-class keys
- ✅ **Modern Architecture**: Custom ClassLoader-based decryption, no Javassist limitations
- ✅ **Zero Dependencies**: Pure Java Crypto API, minimal JAR size
- ✅ **Performance**: LRU cache, lazy decryption, ~200ms startup overhead

## 🚀 Quick Start

### CLI Usage

```bash
# Encrypt a JAR
java -jar byteguard-cli.jar encrypt \
  --input app.jar \
  --output app-encrypted.jar \
  --packages com.example \
  --password yourpassword

# Run encrypted JAR
java -javaagent:byteguard-cli.jar=password=yourpassword \
  -jar app-encrypted.jar
```

### Maven Plugin

```xml
<plugin>
    <groupId>io.github.ygqygq2</groupId>
    <artifactId>byteguard-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>encrypt</goal>
            </goals>
            <configuration>
                <password>${env.BYTEGUARD_PASSWORD}</password>
                <packages>
                    <package>com.example</package>
                </packages>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 📦 Features

### Security

- **AES-256-GCM**: Authenticated encryption with Galois/Counter Mode
- **PBKDF2**: 100,000 iterations key derivation
- **HKDF**: Per-class key derivation using class name as context
- **Argon2id**: Password verification hash (memory-hard, GPU-resistant)
- **Random Salt**: Unique 32-byte salt per JAR
- **Random IV**: 12-byte initialization vector per class

### Compatibility

- **JDK**: 8, 11, 17, 21+
- **Frameworks**: Spring Boot, Quarkus, Micronaut
- **Build Tools**: Maven, Gradle
- **Containers**: Docker, Kubernetes, Podman

### Modern Java Support

- ✅ Lambda Expressions (`() -> {}`)
- ✅ Method References (`String::length`)
- ✅ Stream API
- ✅ Records (Java 16+)
- ✅ Sealed Classes (Java 17+)
- ✅ Pattern Matching
- ✅ Switch Expressions

## 🏗️ Architecture

```
Password → PBKDF2(100k) → Master Key → HKDF(className) → Class Key
                                              ↓
                                    AES-256-GCM Encrypt
                                              ↓
                                    [IV(12) + Ciphertext + TAG(16)]
```

### Storage Format

```
encrypted-app.jar
├── META-INF/
│   ├── byteguard-metadata.json  # Encryption metadata
│   └── .encrypted/              # Encrypted classes
│       └── com/example/UserService.class  # [IV + Ciphertext + TAG]
├── static/                      # Static resources (not encrypted)
└── application.yml
```

## 📚 Documentation

- [Architecture Design](docs/architecture.md)
- [Security Analysis](docs/security.md)
- [User Guide](docs/user-guide.md)
- [API Documentation](docs/api.md)

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting PRs.

### Development Setup

```bash
# Clone repository
git clone https://github.com/ygqygq2/byteguard.git
cd byteguard

# Build
./gradlew build

# Run tests
./gradlew test

# Run integration tests
./gradlew integrationTest
```

## 📄 License

Apache License 2.0 - see [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by [ClassFinal](https://github.com/roseboy/classfinal)
- Built with modern cryptographic best practices from NIST and OWASP

## 📧 Contact

- **Author**: ygqygq2
- **GitHub**: [@ygqygq2](https://github.com/ygqygq2)
- **Issues**: [GitHub Issues](https://github.com/ygqygq2/byteguard/issues)

---

**⚠️ Security Notice**: ByteGuard provides strong encryption but is not a silver bullet. For maximum security:
- Use strong passwords (16+ characters)
- Combine with license verification
- Consider hardware binding
- Protect core algorithms on server-side
