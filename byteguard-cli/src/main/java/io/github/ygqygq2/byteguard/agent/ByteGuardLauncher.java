package io.github.ygqygq2.byteguard.agent;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * ByteGuard 启动器
 * 
 * <p>用于运行加密的 JAR，替代 java -jar 的入口
 * 
 * @author ygqygq2
 */
public class ByteGuardLauncher {
    
    public static void main(String[] args) {
        System.out.println("[ByteGuard] ByteGuard Launcher");
        
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String password = null;
        String jarPath = null;
        String mainClass = null;
        String[] appArgs = new String[0];
        
        // 解析参数
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--password":
                    password = args[++i];
                    break;
                case "--jar":
                    jarPath = args[++i];
                    break;
                case "--main-class":
                    mainClass = args[++i];
                    break;
                case "--":
                    // 剩余参数传给应用
                    appArgs = new String[args.length - i - 1];
                    System.arraycopy(args, i + 1, appArgs, 0, appArgs.length);
                    i = args.length;
                    break;
            }
        }
        
        if (password == null || jarPath == null) {
            System.err.println("Error: Missing required arguments");
            printUsage();
            System.exit(1);
        }
        
        try {
            // 使用 Agent 方式运行
            System.out.println("[ByteGuard] Use -javaagent instead:");
            System.out.println("  java -javaagent:byteguard.jar=password=" + password + " -jar " + jarPath);
            System.exit(1);
            
        } catch (Exception e) {
            System.err.println("[ByteGuard] Failed to launch: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar byteguard.jar --password <pwd> --jar <encrypted.jar>");
        System.out.println();
        System.out.println("Or use JavaAgent (recommended):");
        System.out.println("  java -javaagent:byteguard.jar=password=<pwd> -jar <encrypted.jar>");
    }
}
