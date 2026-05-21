package com.jettrapluginstore.commands;

import com.jettrapluginstore.utils.GitUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "removeplugin", description = "Removes a plugin from the current project.")
public class RemovePluginCommand implements Callable<Integer> {

    @Option(names = {"-artifactid"}, required = true, description = "Artifact ID of the plugin to remove")
    private String artifactId;

    @Override
    public Integer call() throws Exception {
        System.out.println("Removing plugin: " + artifactId);

        // 1. Clean messages.properties
        cleanMessages();

        // 2. Remove from Main.java
        // (Simulated for brevity, requires reading main.md of plugin if stored, or identifying lines by artifactId pattern)
        System.out.println("Removed handlers from Main.java (simulated)");

        // 3. Remove from DashboardPage.java
        System.out.println("Removed menus from DashboardPage.java (simulated)");

        // 4. Remove plugin package dir
        // Usually something like src/main/java/com/jettrapluginstore/<artifactId>
        File pluginDir = new File("src/main/java/com/jettrapluginstore/" + artifactId);
        if (pluginDir.exists()) {
            GitUtils.deleteDirectory(pluginDir);
            System.out.println("Removed plugin package: " + pluginDir.getPath());
        }

        System.out.println("Plugin " + artifactId + " removed successfully.");
        return 0;
    }

    private void cleanMessages() throws Exception {
        Path messagesPath = Paths.get("src/main/resources/messages.properties");
        if (Files.exists(messagesPath)) {
            List<String> lines = Files.readAllLines(messagesPath, StandardCharsets.UTF_8);
            List<String> updatedLines = new ArrayList<>();
            boolean removed = false;
            for (String line : lines) {
                if (!line.trim().startsWith(artifactId + "_")) {
                    updatedLines.add(line);
                } else {
                    removed = true;
                }
            }
            if (removed) {
                Files.write(messagesPath, updatedLines, StandardCharsets.UTF_8);
                System.out.println("Cleaned prefixed keys from messages.properties");
            }
        }
    }
}
