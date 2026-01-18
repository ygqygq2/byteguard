package io.github.ygqygq2.byteguard.cli;

import io.github.ygqygq2.byteguard.cli.command.EncryptCommand;
import io.github.ygqygq2.byteguard.cli.command.LicenseCommand;

/**
 * CLI 主入口
 * 
 * @author ygqygq2
 */
public class Main {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);
        
        try {
            switch (command) {
                case "encrypt":
                    new EncryptCommand().execute(commandArgs);
                    break;
                    
                case "license":
                    new LicenseCommand().execute(commandArgs);
                    break;
                    
                case "help":
                case "--help":
                case "-h":
                    printUsage();
                    break;
                    
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("ByteGuard - Java Bytecode Encryption Tool");
        System.out.println();
        System.out.println("Usage: java -jar byteguard.jar <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  encrypt    Encrypt a JAR file");
        System.out.println("  license    Generate or manage licenses");
        System.out.println("  help       Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Encrypt a JAR");
        System.out.println("  java -jar byteguard.jar encrypt \\");
        System.out.println("    --input app.jar \\");
        System.out.println("    --output app-encrypted.jar \\");
        System.out.println("    --password mypassword");
        System.out.println();
        System.out.println("  # Generate a license");
        System.out.println("  java -jar byteguard.jar license generate \\");
        System.out.println("    --issued-to \"Company ABC\" \\");
        System.out.println("    --expire 2027-12-31 \\");
        System.out.println("    --output license.lic");
        System.out.println();
        System.out.println("For more information: https://github.com/ygqygq2/byteguard");
    }
}
