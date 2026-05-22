package com.jettrapluginstore.commands;

import com.jettrapluginstore.config.CredentialsManager;
import com.jettrapluginstore.utils.GitUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Command(name = "installplugin", description = "Installs a plugin to the current project.")
public class InstallPluginCommand implements Callable<Integer> {

    @Option(names = {"-artifactid"}, required = true, description = "Artifact ID of the plugin")
    private String artifactId;

    @Option(names = {"-v"}, required = true, description = "Version of the plugin")
    private String version;

    @Option(names = {"-d", "--dir"}, description = "Target Jettra project directory (defaults to current directory)")
    private String targetDir = ".";

    @Option(names = {"-s", "--secret"}, description = "Passphrase to decrypt credentials")
    private String secret;

    @Override
    public Integer call() throws Exception {
        System.out.println("Installing plugin: " + artifactId + " version " + version + " in " + targetDir);

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

        Path baseDir = Paths.get(targetDir);
        Path cloneDir = Files.createTempDirectory("jettra-appstore-install");
        
        List<String> conflicts = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        try (Git git = GitUtils.cloneRepository(cloneDir)) {
            Path pluginZip = cloneDir.resolve(artifactId).resolve(version).resolve(artifactId + "-" + version + ".zip");
            if (!Files.exists(pluginZip)) {
                System.err.println("Plugin " + artifactId + " v" + version + " not found in repository.");
                return 1;
            }

            // Temp extraction dir
            Path extractDir = Files.createTempDirectory("jettra-extracted");
            System.out.println("Extracting plugin to temporary directory...");
            unzip(pluginZip.toString(), extractDir.toString());

            // Create target metadata directory: .jettra/plugins/<artifactId>
            Path metaDir = baseDir.resolve(".jettra/plugins/" + artifactId);
            Files.createDirectories(metaDir);

            // Copy plugin metadata files to metaDir for removal reference later
            copyIfExists(extractDir.resolve("plugin-descriptor.md"), metaDir.resolve("plugin-descriptor.md"));
            copyIfExists(extractDir.resolve("main.md"), metaDir.resolve("main.md"));
            copyIfExists(extractDir.resolve("dashboardbasepage.md"), metaDir.resolve("dashboardbasepage.md"));

            // 1. Copy source files and assets to target project
            System.out.println("Copying source files to project...");
            copySourceFiles(extractDir.toFile(), baseDir.toFile());

            // 2. Merge pom.xml dependencies
            mergePomXml(baseDir, extractDir.resolve("plugin-descriptor.md"), actions, conflicts);

            // 3. Merge internationalized messages
            mergeMessages(baseDir, extractDir.resolve("src/main/resources"), actions, conflicts);

            // 4. Merge Main handlers
            mergeMain(baseDir, metaDir.resolve("main.md"), actions, conflicts);

            // 5. Merge Dashboard menus
            mergeDashboard(baseDir, metaDir.resolve("dashboardbasepage.md"), actions, conflicts);

            // Clean temp extracted dir
            GitUtils.deleteDirectory(extractDir.toFile());

            // 6. Generate installation report
            generateReport(baseDir, actions, conflicts);

            System.out.println("Plugin installed successfully. See plugin-result.md for details.");
        } finally {
            GitUtils.deleteDirectory(cloneDir.toFile());
        }

        return 0;
    }

    private void copyIfExists(Path src, Path dest) {
        try {
            if (Files.exists(src)) {
                Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("Failed to copy metadata: " + e.getMessage());
        }
    }

    private void unzip(String zipFilePath, String destDir) throws Exception {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();
        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + ze.getName());
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
        }
    }

    private void copySourceFiles(File source, File target) throws Exception {
        if (source.isDirectory()) {
            String name = source.getName();
            // Skip metadata files at the root
            if (name.equals(".jettra") || name.equals("target") || name.equals(".git")) return;
            
            // Create folder in target if it is under src or resources
            String relative = source.getPath();
            if (relative.contains("src/main/java") || relative.contains("src/main/resources")) {
                if (!target.exists()) target.mkdirs();
            }
            
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    File targetChild = new File(target, child.getName());
                    copySourceFiles(child, targetChild);
                }
            }
        } else {
            String name = source.getName();
            if (name.equals("plugin-descriptor.md") || name.equals("main.md") || name.equals("dashboardbasepage.md")) {
                return; // don't copy to root of project
            }
            if (source.getPath().contains("src/main/java") || source.getPath().contains("src/main/resources")) {
                if (!target.getParentFile().exists()) {
                    target.getParentFile().mkdirs();
                }
                Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void mergePomXml(Path baseDir, Path descriptorPath, List<String> actions, List<String> conflicts) throws Exception {
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

        String pomContent = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
        String[] depList = deps.split(",");
        
        StringBuilder newDeps = new StringBuilder();
        for (String dep : depList) {
            String cleanDep = dep.trim();
            if (cleanDep.isEmpty()) continue;
            // Check if dependency already exists in pom.xml
            if (!pomContent.contains("<artifactId>" + cleanDep + "</artifactId>") && !pomContent.contains(cleanDep.toLowerCase())) {
                newDeps.append("        <dependency>\n");
                newDeps.append("            <groupId>com.jettra</groupId>\n");
                newDeps.append("            <artifactId>").append(cleanDep).append("</artifactId>\n");
                newDeps.append("            <version>1.0-SNAPSHOT</version>\n");
                newDeps.append("        </dependency>\n");
                actions.add("Added dependency " + cleanDep + " to pom.xml");
            } else {
                actions.add("Dependency " + cleanDep + " is already present. Skipped to prevent duplicates.");
            }
        }

        if (newDeps.length() > 0) {
            int endDepsIdx = pomContent.indexOf("</dependencies>");
            if (endDepsIdx > 0) {
                String updatedPom = pomContent.substring(0, endDepsIdx) + newDeps.toString() + pomContent.substring(endDepsIdx);
                Files.write(pomPath, updatedPom.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void mergeMessages(Path baseDir, Path pluginResources, List<String> actions, List<String> conflicts) throws Exception {
        if (!Files.exists(pluginResources)) return;
        Path targetResources = baseDir.resolve("src/main/resources");
        if (!Files.exists(targetResources)) Files.createDirectories(targetResources);

        File[] files = pluginResources.toFile().listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile() && file.getName().startsWith("messages") && file.getName().endsWith(".properties")) {
                Path targetPropFile = targetResources.resolve(file.getName());
                List<String> newLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                
                List<String> targetLines = Files.exists(targetPropFile) ? 
                        Files.readAllLines(targetPropFile, StandardCharsets.UTF_8) : new ArrayList<>();
                
                boolean updated = false;
                for (String line : newLines) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf("=");
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        // Check if key is already in targetPropFile
                        boolean exists = false;
                        for (String tLine : targetLines) {
                            if (tLine.trim().startsWith(key + "=")) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            targetLines.add(line);
                            updated = true;
                        }
                    }
                }
                if (updated) {
                    Files.write(targetPropFile, targetLines, StandardCharsets.UTF_8);
                    actions.add("Merged keys in messages properties: " + file.getName());
                }
            }
        }
    }

    private void mergeMain(Path baseDir, Path mainMdPath, List<String> actions, List<String> conflicts) throws Exception {
        if (!Files.exists(mainMdPath)) return;
        List<String> handlers = Files.readAllLines(mainMdPath, StandardCharsets.UTF_8);
        if (handlers.isEmpty()) return;

        // Locate Main file
        Path srcJava = baseDir.resolve("src/main/java");
        File mainFile = findFile(srcJava.toFile(), "Main.java");
        if (mainFile == null) mainFile = findFile(srcJava.toFile(), "WebExampleMain.java");

        if (mainFile == null) {
            conflicts.add("Could not locate Main.java or WebExampleMain.java to register handlers.");
            return;
        }

        String mainContent = new String(Files.readAllBytes(mainFile.toPath()), StandardCharsets.UTF_8);
        StringBuilder cleanHandlers = new StringBuilder();
        
        for (String handlerLine : handlers) {
            String line = handlerLine.trim();
            if (line.isEmpty()) continue;
            
            // Extract route to check conflict
            // server.addHandler("/clock", ClockPage.class);
            int startQuote = line.indexOf("\"");
            int endQuote = line.indexOf("\"", startQuote + 1);
            if (startQuote > 0 && endQuote > startQuote) {
                String route = line.substring(startQuote, endQuote + 1); // e.g. "/clock"
                if (mainContent.contains("server.addHandler(" + route)) {
                    // Conflict!
                    conflicts.add("Route Conflict: Route " + route + " is already registered in " + mainFile.getName() + ".");
                    String suggestedRoute = route.substring(0, route.length() - 1) + "_" + artifactId + "\"";
                    conflicts.add("Suggestion: Update the handler path to " + suggestedRoute + " to avoid conflicts.");
                } else {
                    cleanHandlers.append("        ").append(line).append("\n");
                    actions.add("Registered handler: " + line);
                }
            } else {
                cleanHandlers.append("        ").append(line).append("\n");
                actions.add("Registered handler (no conflict check): " + line);
            }
        }

        if (cleanHandlers.length() > 0) {
            int startServer = mainContent.indexOf("server.start();");
            if (startServer > 0) {
                String updatedContent = mainContent.substring(0, startServer) +
                        "        // Added by plugin " + artifactId + "\n" +
                        cleanHandlers.toString() + "\n" +
                        mainContent.substring(startServer);
                Files.write(mainFile.toPath(), updatedContent.getBytes(StandardCharsets.UTF_8));
            } else {
                conflicts.add("Failed to find 'server.start();' in Main file. Handlers were not automatically injected.");
            }
        }
    }

    private void mergeDashboard(Path baseDir, Path dashMdPath, List<String> actions, List<String> conflicts) throws Exception {
        if (!Files.exists(dashMdPath)) return;
        List<String> lines = Files.readAllLines(dashMdPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return;

        Path srcJava = baseDir.resolve("src/main/java");
        File dashFile = findFile(srcJava.toFile(), "DashboardBasePage.java");
        if (dashFile == null) dashFile = findFile(srcJava.toFile(), "DashboardPage.java");

        if (dashFile == null) {
            conflicts.add("Could not locate DashboardBasePage.java or DashboardPage.java to inject menus.");
            return;
        }

        String dashContent = new String(Files.readAllBytes(dashFile.toPath()), StandardCharsets.UTF_8);
        
        // Find menu items inside dashboardbasepage.md
        List<String> newMenuItems = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("appendMenuItem") || line.contains("addCategory")) {
                newMenuItems.add(line.trim());
            }
        }

        if (newMenuItems.isEmpty()) return;

        StringBuilder menuInject = new StringBuilder();
        for (String item : newMenuItems) {
            if (dashContent.contains(item)) {
                actions.add("Menu item " + item + " already exists. Skipped.");
            } else {
                menuInject.append("        ").append(item).append("\n");
                actions.add("Injected menu item: " + item);
            }
        }

        if (menuInject.length() > 0) {
            int finishMenuIdx = dashContent.indexOf("finishMenuBuilder(left);");
            if (finishMenuIdx > 0) {
                String updatedDash = dashContent.substring(0, finishMenuIdx) +
                        "        // Menu options for plugin " + artifactId + "\n" +
                        menuInject.toString() + "\n" +
                        dashContent.substring(finishMenuIdx);
                Files.write(dashFile.toPath(), updatedDash.getBytes(StandardCharsets.UTF_8));
            } else {
                conflicts.add("Failed to find 'finishMenuBuilder(left);' in Dashboard page. Menu items were not automatically injected.");
            }
        }
    }

    private void generateReport(Path baseDir, List<String> actions, List<String> conflicts) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# Jettra Plugin Integration Report\n\n");
        sb.append("- **Plugin**: ").append(artifactId).append("\n");
        sb.append("- **Version**: ").append(version).append("\n");
        sb.append("- **Status**: ").append(conflicts.isEmpty() ? "Success" : "Completed with Conflicts").append("\n\n");

        sb.append("## Actions Performed\n");
        for (String act : actions) {
            sb.append("- [x] ").append(act).append("\n");
        }
        sb.append("\n");

        sb.append("## Integration Conflicts & Warnings\n");
        if (conflicts.isEmpty()) {
            sb.append("*No conflicts detected. All systems operating at peak performance.*\n");
        } else {
            for (String conf : conflicts) {
                sb.append("- [!] ").append(conf).append("\n");
            }
        }

        Files.write(baseDir.resolve("plugin-result.md"), sb.toString().getBytes(StandardCharsets.UTF_8));
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
