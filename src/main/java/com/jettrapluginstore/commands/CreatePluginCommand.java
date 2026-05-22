package com.jettrapluginstore.commands;

import com.jettrapluginstore.config.CredentialsManager;
import com.jettrapluginstore.utils.GitUtils;
import com.jettrapluginstore.utils.ProjectModifier;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Command(name = "createplugin", description = "Converts a Maven project to a Jettra plugin and uploads it to GitHub.")
public class CreatePluginCommand implements Callable<Integer> {

    @Option(names = {"-p", "--package"}, description = "Paquete a ser convertido en plugin")
    private String packageToConvert;

    @Option(names = {"-d", "--dir"}, description = "Target Jettra project directory (defaults to current directory)")
    private String targetDir = ".";

    @Option(names = {"-s", "--secret"}, description = "Passphrase to decrypt credentials")
    private String secret;

    @Override
    public Integer call() throws Exception {
        System.out.println("Starting createplugin process...");
        File baseDir = new File(targetDir);
        File descriptorFile = new File(baseDir, "plugin-descriptor.md");
        if (!descriptorFile.exists()) {
            System.err.println("Error: plugin-descriptor.md not found in " + baseDir.getAbsolutePath() + ". Run prepareplugin first.");
            return 1;
        }

        Map<String, String> descriptor = new HashMap<>();
        for (String line : Files.readAllLines(descriptorFile.toPath(), StandardCharsets.UTF_8)) {
            int eq = line.indexOf(":");
            if (eq > 0) {
                descriptor.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }

        String artifactId = descriptor.get("ArtifactId");
        String version = descriptor.get("Versión");
        String pluginPackage = descriptor.get("Plugin-Package");
        
        if (artifactId == null || version == null || pluginPackage == null) {
            System.err.println("Error: plugin-descriptor.md is missing required fields (ArtifactId, Versión, Plugin-Package).");
            return 1;
        }

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

        String[] creds = CredentialsManager.loadCredentials(finalSecret);
        if (creds == null) {
            System.err.println("Failed to load credentials or incorrect passphrase.");
            return 1;
        }
        String user = creds[0];
        String pat = creds[1];

        Path tempProjectDir = Files.createTempDirectory("jettra-plugin-" + artifactId);
        System.out.println("Copying project to temporary directory: " + tempProjectDir);
        copyProject(baseDir, tempProjectDir.toFile());

        // 1. Refactor packages
        System.out.println("Refactoring packages...");
        Path srcMainJava = tempProjectDir.resolve("src/main/java");
        if (Files.exists(srcMainJava)) {
            // Exclude com.jettrapluginstore package if it exists
            Path storePkg = srcMainJava.resolve("com/jettrapluginstore");
            if (Files.exists(storePkg)) {
                System.out.println("Excluding package com.jettrapluginstore from the compiled plugin...");
                GitUtils.deleteDirectory(storePkg.toFile());
            }

            String oldPackage = packageToConvert != null ? packageToConvert : findBasePackage(srcMainJava.toFile());
            System.out.println("Detected base package to convert: " + oldPackage);
            if (oldPackage != null) {
                if (packageToConvert != null) {
                    keepOnlyPackage(srcMainJava, packageToConvert);
                }
                ProjectModifier.refactorPackages(srcMainJava, oldPackage, pluginPackage);
            }
        }

        // 2. Modificar messages.properties and other messages_*.properties
        Path resourcesPath = tempProjectDir.resolve("src/main/resources");
        if (Files.exists(resourcesPath)) {
            ProjectModifier.prefixAllMessages(resourcesPath, artifactId);
        }

        // 3. Extract handlers to main.md
        extractAndRemoveMain(srcMainJava, tempProjectDir);

        // 4. Extract menus to dashboardbasepage.md
        extractAndRemoveDashboard(srcMainJava, tempProjectDir);

        // Explicitly exclude and clean Main.java, DashboardBasePage.java, DashboardPage.java recursively
        deleteFileWithName(srcMainJava.toFile(), "Main.java");
        deleteFileWithName(srcMainJava.toFile(), "DashboardBasePage.java");
        deleteFileWithName(srcMainJava.toFile(), "DashboardPage.java");

        // 5. Crear Zip
        File zipFile = new File(tempProjectDir.toFile(), artifactId + "-" + version + ".zip");
        zipDirectory(tempProjectDir.toFile(), zipFile);

        // 6. Subir a GitHub
        Path cloneDir = Files.createTempDirectory("jettra-appstore");
        try (Git git = GitUtils.cloneRepository(cloneDir)) {
            Path pluginDir = cloneDir.resolve(artifactId).resolve(version);
            Files.createDirectories(pluginDir);
            
            Path targetZip = pluginDir.resolve(zipFile.getName());
            Files.copy(zipFile.toPath(), targetZip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Copy plugin-descriptor.md, main.md, dashboardbasepage.md to release dir as well for index
            copyFileIfExists(tempProjectDir.resolve("plugin-descriptor.md"), pluginDir.resolve("plugin-descriptor.md"));
            copyFileIfExists(tempProjectDir.resolve("main.md"), pluginDir.resolve("main.md"));
            copyFileIfExists(tempProjectDir.resolve("dashboardbasepage.md"), pluginDir.resolve("dashboardbasepage.md"));

            updatePluginDb(cloneDir, descriptor);
            updateMainDb(cloneDir, artifactId, version, tempProjectDir.resolve("main.md"));

            GitUtils.commitAndPush(git, user, pat, "Add plugin " + artifactId + " v" + version);
        } finally {
            GitUtils.deleteDirectory(cloneDir.toFile());
            GitUtils.deleteDirectory(tempProjectDir.toFile());
        }

        System.out.println("Plugin created and uploaded successfully.");
        return 0;
    }

    private void copyFileIfExists(Path source, Path dest) {
        try {
            if (Files.exists(source)) {
                Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("Failed to copy metadata file: " + e.getMessage());
        }
    }

    private void extractAndRemoveMain(Path srcMainJava, Path targetRoot) throws Exception {
        File mainFile = findFile(srcMainJava.toFile(), "Main.java");
        if (mainFile == null) mainFile = findFile(srcMainJava.toFile(), "WebExampleMain.java");
        
        if (mainFile != null) {
            String content = new String(Files.readAllBytes(mainFile.toPath()), StandardCharsets.UTF_8);
            StringBuilder handlers = new StringBuilder();
            for (String line : content.split("\n")) {
                if (line.contains("server.addHandler")) {
                    handlers.append(line.trim()).append("\n");
                }
            }
            Files.write(targetRoot.resolve("main.md"), handlers.toString().getBytes(StandardCharsets.UTF_8));
            mainFile.delete();
        }
    }

    private void extractAndRemoveDashboard(Path srcMainJava, Path targetRoot) throws Exception {
        File dashFile = findFile(srcMainJava.toFile(), "DashboardBasePage.java");
        if (dashFile == null) dashFile = findFile(srcMainJava.toFile(), "DashboardPage.java");
        
        if (dashFile != null) {
            String content = new String(Files.readAllBytes(dashFile.toPath()), StandardCharsets.UTF_8);
            Files.write(targetRoot.resolve("dashboardbasepage.md"), content.getBytes(StandardCharsets.UTF_8));
            dashFile.delete();
        }
    }

    private void updatePluginDb(Path cloneDir, Map<String, String> descriptor) throws Exception {
        Path dbPath = cloneDir.resolve("db/plugin-db.md");
        if (!Files.exists(dbPath)) {
            Files.createDirectories(dbPath.getParent());
            Files.write(dbPath, "Name,Artifactid,Plugin-Package,Versión,Autor,Email,WebSite,Description,Dependencies,DateTime,Enabled\n".getBytes(StandardCharsets.UTF_8));
        }
        
        String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,true\n",
                descriptor.getOrDefault("Name", ""),
                descriptor.getOrDefault("ArtifactId", ""),
                descriptor.getOrDefault("Plugin-Package", ""),
                descriptor.getOrDefault("Versión", ""),
                descriptor.getOrDefault("Autor", ""),
                descriptor.getOrDefault("Email", ""),
                descriptor.getOrDefault("WebSite", ""),
                descriptor.getOrDefault("Description", ""),
                descriptor.getOrDefault("Dependencies", ""),
                descriptor.getOrDefault("DateTime", "")
        );
        
        Files.write(dbPath, line.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
    }

    private void updateMainDb(Path cloneDir, String artifactId, String version, Path mainMdPath) throws Exception {
        Path dbPath = cloneDir.resolve("db/main-db.md");
        if (!Files.exists(dbPath)) {
            Files.createDirectories(dbPath.getParent());
            Files.write(dbPath, "# Jettra Plugins Registered Handlers Database\n\n".getBytes(StandardCharsets.UTF_8));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("## Plugin: ").append(artifactId).append(" | Version: ").append(version).append("\n");
        if (Files.exists(mainMdPath)) {
            String handlers = new String(Files.readAllBytes(mainMdPath), StandardCharsets.UTF_8);
            if (!handlers.trim().isEmpty()) {
                sb.append("```java\n").append(handlers).append("```\n\n");
            } else {
                sb.append("*No handlers registered.*\n\n");
            }
        } else {
            sb.append("*No handlers registered.*\n\n");
        }
        
        Files.write(dbPath, sb.toString().getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
    }

    private void copyProject(File source, File target) throws Exception {
        if (source.isDirectory()) {
            if (source.getName().equals("target") || source.getName().equals(".git")) return;
            if (!target.exists()) target.mkdirs();
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    copyProject(new File(source, child), new File(target, child));
                }
            }
        } else {
            Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

    private String findBasePackage(File dir) {
        List<String> packages = new ArrayList<>();
        findPackagesWithJava(dir, "", packages);
        if (packages.isEmpty()) {
            return "com.jettra.example";
        }
        String common = packages.get(0);
        for (int i = 1; i < packages.size(); i++) {
            common = getCommonPrefix(common, packages.get(i));
        }
        if (common.endsWith(".")) {
            common = common.substring(0, common.length() - 1);
        }
        return common.isEmpty() ? "com.jettra.example" : common;
    }

    private void findPackagesWithJava(File file, String currentPkg, List<String> packages) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            boolean hasJava = false;
            if (children != null) {
                for (File child : children) {
                    if (child.isFile() && child.getName().endsWith(".java")) {
                        hasJava = true;
                    } else if (child.isDirectory()) {
                        String nextPkg = currentPkg.isEmpty() ? child.getName() : currentPkg + "." + child.getName();
                        findPackagesWithJava(child, nextPkg, packages);
                    }
                }
            }
            if (hasJava && !currentPkg.isEmpty()) {
                packages.add(currentPkg);
            }
        }
    }

    private String getCommonPrefix(String p1, String p2) {
        String[] parts1 = p1.split("\\.");
        String[] parts2 = p2.split("\\.");
        StringBuilder sb = new StringBuilder();
        int min = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < min; i++) {
            if (parts1[i].equals(parts2[i])) {
                sb.append(parts1[i]).append(".");
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private void keepOnlyPackage(Path srcMainJava, String pkg) throws Exception {
        String pkgPath = pkg.replace('.', '/');
        Path targetDir = srcMainJava.resolve(pkgPath);
        
        if (Files.exists(targetDir)) {
            Path tempSafe = Files.createTempDirectory("safe_pkg");
            copyProject(targetDir.toFile(), tempSafe.toFile());
            
            GitUtils.deleteDirectory(srcMainJava.toFile());
            Files.createDirectories(targetDir);
            
            copyProject(tempSafe.toFile(), targetDir.toFile());
            GitUtils.deleteDirectory(tempSafe.toFile());
        } else {
            System.err.println("Warning: Package " + pkg + " not found in src/main/java.");
        }
    }

    private void zipDirectory(File dir, File zipFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFile(dir, dir, zos);
        }
    }

    private void zipFile(File root, File sourceFile, ZipOutputStream zos) throws Exception {
        if (sourceFile.isHidden() || sourceFile.getName().endsWith(".zip")) return;
        if (sourceFile.isDirectory()) {
            for (File file : sourceFile.listFiles()) {
                zipFile(root, file, zos);
            }
        } else {
            byte[] buffer = new byte[1024];
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                String name = root.toURI().relativize(sourceFile.toURI()).getPath();
                zos.putNextEntry(new ZipEntry(name));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }

    private void deleteFileWithName(File dir, String name) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFileWithName(file, name);
                } else if (file.getName().equalsIgnoreCase(name)) {
                    file.delete();
                    System.out.println("Excluded " + name + " from packaged plugin.");
                }
            }
        }
    }
}
