package com.jettrapluginstore.commands;

import com.jettrapluginstore.config.CredentialsManager;
import com.jettrapluginstore.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(name = "list", description = "Lists the available plugins from JettraAppStore.")
public class ListPluginsCommand implements Callable<Integer> {

    @Option(names = {"-s", "--secret"}, description = "Passphrase to decrypt credentials")
    private String secret;

    @Override
    public Integer call() throws Exception {
        System.out.println("Listing plugins...");
        
        String finalSecret = secret;
        if (finalSecret == null || finalSecret.isEmpty()) {
            System.out.print("Enter passphrase for credentials: ");
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNextLine()) {
                finalSecret = scanner.nextLine();
            } else {
                System.err.println("Error: secret passphrase is required but standard input is empty.");
                return 1;
            }
        }

        try {
            List<Map<String, String>> plugins = fetchPlugins(finalSecret);
            if (plugins == null || plugins.isEmpty()) {
                System.out.println("No plugins available in the repository.");
                return 0;
            }

            System.out.println("---------------------------------------------------------------------------------------------------------");
            System.out.printf("%-20s | %-15s | %-10s | %-30s | %-10s\n", "Name", "ArtifactId", "Version", "Description", "Enabled");
            System.out.println("---------------------------------------------------------------------------------------------------------");
            for (Map<String, String> plugin : plugins) {
                String desc = plugin.getOrDefault("Description", "");
                if (desc.length() > 30) {
                    desc = desc.substring(0, 27) + "...";
                }
                System.out.printf("%-20s | %-15s | %-10s | %-30s | %-10s\n", 
                        plugin.getOrDefault("Name", ""), 
                        plugin.getOrDefault("ArtifactId", ""), 
                        plugin.getOrDefault("Versión", ""), 
                        desc, 
                        plugin.getOrDefault("Enabled", "true"));
            }
            System.out.println("---------------------------------------------------------------------------------------------------------");
        } catch (Exception e) {
            System.err.println("Failed to fetch plugins: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    public static List<Map<String, String>> fetchPlugins(String secretPassphrase) throws Exception {
        String[] creds = CredentialsManager.loadCredentials(secretPassphrase);
        if (creds == null) {
            throw new Exception("Failed to load credentials or incorrect passphrase.");
        }

        List<Map<String, String>> plugins = new ArrayList<>();
        Path cloneDir = Files.createTempDirectory("jettra-appstore-list");
        try (Git git = GitUtils.cloneRepository(cloneDir)) {
            Path dbPath = cloneDir.resolve("db/plugin-db.md");
            if (Files.exists(dbPath)) {
                List<String> lines = Files.readAllLines(dbPath, StandardCharsets.UTF_8);
                if (lines.size() <= 1) {
                    return plugins;
                }
                String[] headers = lines.get(0).split(",");
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    Map<String, String> map = new HashMap<>();
                    for (int j = 0; j < headers.length; j++) {
                        if (j < parts.length) {
                            map.put(headers[j].trim(), parts[j].trim());
                        } else {
                            map.put(headers[j].trim(), "");
                        }
                    }
                    plugins.add(map);
                }
            }
        } finally {
            GitUtils.deleteDirectory(cloneDir.toFile());
        }
        return plugins;
    }
}
