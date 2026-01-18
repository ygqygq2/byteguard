package io.github.ygqygy2.byteguard.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * ByteGuard 加密 Maven 插件
 * 
 * <p>在 package 阶段自动加密 JAR 文件
 * 
 * @author ygqygq2
 */
@Mojo(name = "encrypt", defaultPhase = LifecyclePhase.PACKAGE)
public class EncryptMojo extends AbstractMojo {
    
    /**
     * Maven 项目对象
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    
    /**
     * 输入 JAR 文件
     */
    @Parameter(property = "byteguard.input")
    private File input;
    
    /**
     * 输出 JAR 文件
     */
    @Parameter(property = "byteguard.output")
    private File output;
    
    /**
     * 加密密码（推荐使用环境变量）
     */
    @Parameter(property = "byteguard.password", required = true)
    private String password;
    
    /**
     * 要加密的包列表
     */
    @Parameter(property = "byteguard.packages")
    private List<String> packages;
    
    /**
     * 排除的包列表
     */
    @Parameter(property = "byteguard.excludes")
    private List<String> excludes;
    
    /**
     * 是否跳过加密
     */
    @Parameter(property = "byteguard.skip", defaultValue = "false")
    private boolean skip;
    
    /**
     * 是否替换原始 JAR
     */
    @Parameter(property = "byteguard.replace", defaultValue = "false")
    private boolean replace;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("ByteGuard encryption is skipped");
            return;
        }
        
        try {
            // 1. 确定输入文件
            File inputJar = determineInputJar();
            if (!inputJar.exists()) {
                throw new MojoFailureException("Input JAR not found: " + inputJar);
            }
            
            // 2. 确定输出文件
            File outputJar = determineOutputJar(inputJar);
            
            // 3. 验证密码
            if (password == null || password.trim().isEmpty()) {
                throw new MojoFailureException(
                    "ByteGuard password is required. Set via:\n" +
                    "  - Maven property: -Dbyteguard.password=xxx\n" +
                    "  - Environment variable: export BYTEGUARD_PASSWORD=xxx\n" +
                    "  - Plugin configuration: <password>${env.BYTEGUARD_PASSWORD}</password>"
                );
            }
            
            getLog().info("ByteGuard Encryption");
            getLog().info("  Input:  " + inputJar.getAbsolutePath());
            getLog().info("  Output: " + outputJar.getAbsolutePath());
            if (packages != null && !packages.isEmpty()) {
                getLog().info("  Packages: " + String.join(", ", packages));
            }
            if (excludes != null && !excludes.isEmpty()) {
                getLog().info("  Excludes: " + String.join(", ", excludes));
            }
            
            // 4. 调用加密逻辑（TODO: 实现）
            encryptJar(inputJar, outputJar);
            
            // 5. 替换原始文件（如果配置）
            if (replace) {
                Files.delete(inputJar.toPath());
                Files.move(outputJar.toPath(), inputJar.toPath());
                getLog().info("Replaced original JAR with encrypted version");
            }
            
            getLog().info("ByteGuard encryption completed successfully");
            
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to encrypt JAR", e);
        }
    }
    
    /**
     * 确定输入 JAR 文件
     */
    private File determineInputJar() {
        if (input != null) {
            return input;
        }
        
        // 默认使用项目构建的 JAR
        File buildDir = new File(project.getBuild().getDirectory());
        String finalName = project.getBuild().getFinalName();
        return new File(buildDir, finalName + ".jar");
    }
    
    /**
     * 确定输出 JAR 文件
     */
    private File determineOutputJar(File inputJar) {
        if (output != null) {
            return output;
        }
        
        // 默认添加 -encrypted 后缀
        String inputName = inputJar.getName();
        String outputName = inputName.replace(".jar", "-encrypted.jar");
        return new File(inputJar.getParentFile(), outputName);
    }
    
    /**
     * 执行加密
     * 
     * TODO: 集成 byteguard-core 加密逻辑
     */
    private void encryptJar(File input, File output) throws Exception {
        getLog().warn("Encryption logic not yet implemented - copying file as placeholder");
        
        // TODO: 替换为实际加密逻辑
        // ClassEncryptor encryptor = new ClassEncryptor(password);
        // if (packages != null) {
        //     for (String pkg : packages) {
        //         encryptor.addPackage(pkg);
        //     }
        // }
        // if (excludes != null) {
        //     for (String exclude : excludes) {
        //         encryptor.addExclude(exclude);
        //     }
        // }
        // encryptor.encrypt(input, output);
        
        // 临时实现：复制文件
        Files.copy(input.toPath(), output.toPath());
    }
}
