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

    @Option(names = {"-d", "--dir"}, description = "Target Jettra project directory (defaults to current directory)")
    private String targetDir = ".";

    @Override
    public Integer call() throws Exception {
        System.out.println("Removing plugin: " + artifactId + " from " + targetDir);

        Path baseDir = Paths.get(targetDir);
        Path metaDir = baseDir.resolve(".jettra/plugins/" + artifactId);
        if (!Files.exists(metaDir)) {
            System.err.println("Error: Metadata for plugin " + artifactId + " not found. Is it installed?");
            return 1;
        }

        // 1. Clean messages.properties
        cleanMessages(baseDir);

        // 2. Remove handlers from Main.java
        cleanMain(baseDir, metaDir.resolve("main.md"));

        // 3. Remove menus from DashboardPage.java / DashboardBasePage.java
        cleanDashboard(baseDir, metaDir.resolve("dashboardbasepage.md"));

        // 4. Remove dependencies from pom.xml
        cleanPomXml(baseDir, metaDir.resolve("plugin-descriptor.md"));

        // 5. Remove plugin package directory
        cleanPackageDir(baseDir, metaDir.resolve("plugin-descriptor.md"));

        // 6. Delete metadata folder
        GitUtils.deleteDirectory(metaDir.toFile());

        System.out.println("Plugin " + artifactId + " removed successfully.");
        return 0;
    }

    private void cleanMessages(Path baseDir) throws Exception {
        Path resourcesPath = baseDir.resolve("src/main/resources");
        if (!Files.exists(resourcesPath)) return;

        File[] files = resourcesPath.toFile().listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile() && file.getName().startsWith("messages") && file.getName().endsWith(".properties")) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
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
                    Files.write(file.toPath(), updatedLines, StandardCharsets.UTF_8);
                    System.out.println("Cleaned prefixed keys from message properties: " + file.getName());
                }
            }
        }
    }

    private void cleanMain(Path baseDir, Path mainMdPath) throws Exception {
        if (!Files.exists(mainMdPath)) return;
        List<String> handlers = Files.readAllLines(mainMdPath, StandardCharsets.UTF_8);
        if (handlers.isEmpty()) return;

        Path srcJava = baseDir.resolve("src/main/java");
        File mainFile = findFile(srcJava.toFile(), "Main.java");
        if (mainFile == null) mainFile = findFile(srcJava.toFile(), "WebExampleMain.java");

        if (mainFile == null) return;

        List<String> mainLines = Files.readAllLines(mainFile.toPath(), StandardCharsets.UTF_8);
        List<String> updatedMain = new ArrayList<>();
        
        for (String line : mainLines) {
            boolean shouldRemove = false;
            for (String hLine : handlers) {
                if (line.trim().equals(hLine.trim()) || line.trim().contains("Added by plugin " + artifactId)) {
                    shouldRemove = true;
                    break;
                }
            }
            if (!shouldRemove) {
                updatedMain.add(line);
            }
        }
        Files.write(mainFile.toPath(), updatedMain, StandardCharsets.UTF_8);
        System.out.println("Removed registered handlers from " + mainFile.getName());
    }

    private void cleanDashboard(Path baseDir, Path dashMdPath) throws Exception {
        if (!Files.exists(dashMdPath)) return;
        List<String> lines = Files.readAllLines(dashMdPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return;

        Path srcJava = baseDir.resolve("src/main/java");
        File dashFile = findFile(srcJava.toFile(), "DashboardBasePage.java");
        if (dashFile == null) dashFile = findFile(srcJava.toFile(), "DashboardPage.java");

        if (dashFile == null) return;

        List<String> dashLines = Files.readAllLines(dashFile.toPath(), StandardCharsets.UTF_8);
        List<String> updatedDash = new ArrayList<>();
        
        List<String> menuItems = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("appendMenuItem") || line.contains("addCategory")) {
                menuItems.add(line.trim());
            }
        }

        for (String line : dashLines) {
            boolean shouldRemove = false;
            for (String item : menuItems) {
                if (line.trim().equals(item) || line.trim().contains("Menu options for plugin " + artifactId)) {
                    shouldRemove = true;
                    break;
                }
            }
            if (!shouldRemove) {
                updatedDash.add(line);
            }
        }
        Files.write(dashFile.toPath(), updatedDash, StandardCharsets.UTF_8);
        System.out.println("Removed menu options from " + dashFile.getName());
    }

    private void cleanPomXml(Path baseDir, Path descriptorPath) throws Exception {
        if (!Files.exists(descriptorPath)) return;
        List<String> lines = Files.readAllLines(descriptorPath, StandardCharsets.UTF_8);
        String deps = "";
        for (String line : lines) {
            if (line.startsWith("Dependencies:")) {
                deps = line.substring(13).trim();
            }
        }
        if (deps.isEmpty()) return;

        Path pomPath = baseDir.resolve("pom.xml");
        if (!Files.exists(pomPath)) return;

        String[] depList = deps.split(",");
        List<String> pomLines = Files.readAllLines(pomPath, StandardCharsets.UTF_8);

        for (String dep : depList) {
            String cleanDep = dep.trim();
            if (cleanDep.isEmpty()) continue;

            // Verify if other installed plugins use this dependency
            boolean usedByOther = false;
            Path pluginsDir = baseDir.resolve(".jettra/plugins");
            if (Files.exists(pluginsDir)) {
                File[] pDirs = pluginsDir.toFile().listFiles();
                if (pDirs != null) {
                    for (File pDir : pDirs) {
                        if (pDir.isDirectory() && !pDir.getName().equals(artifactId)) {
                            File descFile = new File(pDir, "plugin-descriptor.md");
                            if (descFile.exists()) {
                                List<String> descLines = Files.readAllLines(descFile.toPath(), StandardCharsets.UTF_8);
                                for (String dLine : descLines) {
                                    if (dLine.startsWith("Dependencies:") && dLine.contains(cleanDep)) {
                                        usedByOther = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!usedByOther) {
                // Remove dependency block from pom.xml
                List<String> updatedPom = new ArrayList<>();
                boolean insideDepBlock = false;
                List<String> tempBlock = new ArrayList<>();
                for (int i = 0; i < pomLines.size(); i++) {
                    String line = pomLines.get(i);
                    if (line.trim().equals("<dependency>")) {
                        insideDepBlock = true;
                        tempBlock.clear();
                        tempBlock.add(line);
                    } else if (insideDepBlock) {
                        tempBlock.add(line);
                        if (line.trim().equals("</dependency>")) {
                            insideDepBlock = false;
                            // Check if this is the dependency to remove
                            boolean isTarget = false;
                            for (String bLine : tempBlock) {
                                if (bLine.contains("<artifactId>" + cleanDep + "</artifactId>")) {
                                    isTarget = true;
                                    break;
                                }
                            }
                            if (!isTarget) {
                                updatedPom.addAll(tempBlock);
                            } else {
                                System.out.println("Removed dependency " + cleanDep + " from pom.xml");
                            }
                        }
                    } else {
                        updatedPom.add(line);
                    }
                }
                pomLines = updatedPom;
            } else {
                System.out.println("Dependency " + cleanDep + " is still used by another plugin. Kept in pom.xml.");
            }
        }
        Files.write(pomPath, pomLines, StandardCharsets.UTF_8);
    }

    private void cleanPackageDir(Path baseDir, Path descriptorPath) throws Exception {
        if (!Files.exists(descriptorPath)) return;
        List<String> lines = Files.readAllLines(descriptorPath, StandardCharsets.UTF_8);
        String pluginPkg = "";
        for (String line : lines) {
            if (line.startsWith("Plugin-Package:")) {
                pluginPkg = line.substring(15).trim();
            }
        }
        if (pluginPkg.isEmpty()) return;

        // E.g. com.jettrapluginstore.webexample -> com/jettrapluginstore/webexample
        String pkgPath = pluginPkg.replace('.', '/');
        Path packageDir = baseDir.resolve("src/main/java").resolve(pkgPath);
        if (Files.exists(packageDir)) {
            GitUtils.deleteDirectory(packageDir.toFile());
            System.out.println("Deleted package folder: " + packageDir.toString());
            
            // Clean empty parent folders if possible
            Path parent = packageDir.getParent();
            while (parent != null && parent.toString().contains("src/main/java") && !parent.endsWith("src/main/java")) {
                File[] children = parent.toFile().listFiles();
                if (children == null || children.length == 0) {
                    Path toDel = parent;
                    parent = parent.getParent();
                    Files.delete(toDel);
                    System.out.println("Cleaned empty parent folder: " + toDel.toString());
                } else {
                    break;
                }
            }
        }
    }

    private File findFile(File dir, String name) {
        if (!dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File found = findFile(file, name);
                    if (found != null) return found;
                } else if (file.getName().equals(name)) {
                    return file;
                }
            }
        }
        return null;
    }
}
