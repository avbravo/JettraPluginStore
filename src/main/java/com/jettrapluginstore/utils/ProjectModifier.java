package com.jettrapluginstore.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectModifier {

    public static void prefixMessages(Path messagesFile, String artifactId) throws IOException {
        if (!Files.exists(messagesFile)) {
            return;
        }
        List<String> lines = Files.readAllLines(messagesFile, StandardCharsets.UTF_8);
        List<String> updatedLines = new ArrayList<>();
        for (String line : lines) {
            int eqIndex = line.indexOf("=");
            if (eqIndex > 0 && !line.startsWith("#")) {
                String key = line.substring(0, eqIndex).trim();
                String val = line.substring(eqIndex + 1).trim();
                if (!key.startsWith(artifactId + "_")) {
                    updatedLines.add(artifactId + "_" + key + "=" + val);
                } else {
                    updatedLines.add(line);
                }
            } else {
                updatedLines.add(line);
            }
        }
        Files.write(messagesFile, updatedLines, StandardCharsets.UTF_8);
    }

    public static void refactorPackages(Path sourceDir, String oldPackage, String newPackage) throws IOException {
        String oldPkgPath = oldPackage.replace('.', '/');
        String newPkgPath = newPackage.replace('.', '/');
        
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path file : files) {
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                // Replace all fully-qualified class references, imports, and package statements
                content = content.replace(oldPackage, newPackage);
                Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        // Move files to new directory structure
        Path oldPathFull = sourceDir.resolve(oldPkgPath);
        Path newPathFull = sourceDir.resolve(newPkgPath);
        
        if (Files.exists(oldPathFull) && !oldPathFull.equals(newPathFull)) {
            Files.createDirectories(newPathFull.getParent());
            copyDirectory(oldPathFull.toFile(), newPathFull.toFile());
            GitUtils.deleteDirectory(oldPathFull.toFile());
        }
    }

    public static void prefixAllMessages(Path resourcesDir, String artifactId) throws IOException {
        if (!Files.exists(resourcesDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(resourcesDir)) {
            List<Path> propFiles = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("messages") && p.getFileName().toString().endsWith(".properties"))
                    .collect(Collectors.toList());
            for (Path propFile : propFiles) {
                prefixMessages(propFile, artifactId);
            }
        }
    }

    private static void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (sourceDir.isDirectory()) {
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            String[] children = sourceDir.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(sourceDir, child), new File(targetDir, child));
                }
            }
        } else {
            Files.copy(sourceDir.toPath(), targetDir.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
