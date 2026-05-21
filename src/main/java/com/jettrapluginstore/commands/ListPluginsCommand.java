package com.jettrapluginstore.commands;

import com.jettrapluginstore.config.CredentialsManager;
import com.jettrapluginstore.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine.Command;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(name = "list", description = "Lists the available plugins from JettraAppStore.")
public class ListPluginsCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Listing plugins...");
        
        System.out.print("Enter passphrase for credentials: ");
        Scanner scanner = new Scanner(System.in);
        String secret = scanner.nextLine();

        String[] creds = CredentialsManager.loadCredentials(secret);
        if (creds == null) {
            System.err.println("Failed to load credentials or incorrect passphrase.");
            return 1;
        }

        Path cloneDir = Files.createTempDirectory("jettra-appstore");
        try (Git git = GitUtils.cloneRepository(cloneDir)) {
            Path dbPath = cloneDir.resolve("db/plugin-db.md");
            if (Files.exists(dbPath)) {
                List<String> lines = Files.readAllLines(dbPath, StandardCharsets.UTF_8);
                System.out.println("---------------------------------------------------------------------------------------------------------");
                System.out.printf("%-20s | %-15s | %-10s | %-30s | %-10s\n", "Name", "ArtifactId", "Version", "Description", "Enabled");
                System.out.println("---------------------------------------------------------------------------------------------------------");
                boolean first = true;
                for (String line : lines) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length >= 11) {
                        System.out.printf("%-20s | %-15s | %-10s | %-30s | %-10s\n", 
                            parts[0], parts[1], parts[3], 
                            parts[7].length() > 30 ? parts[7].substring(0, 27) + "..." : parts[7], 
                            parts[10]);
                    }
                }
                System.out.println("---------------------------------------------------------------------------------------------------------");
            } else {
                System.out.println("No plugins available in the repository.");
            }
        } finally {
            GitUtils.deleteDirectory(cloneDir.toFile());
        }

        return 0;
    }
}
