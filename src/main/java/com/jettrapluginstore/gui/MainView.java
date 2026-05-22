package com.jettrapluginstore.gui;

import com.jettrapluginstore.commands.*;
import com.jettrapluginstore.config.CredentialsManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.prefs.Preferences;

public class MainView extends BorderPane {

    // Sidebar Navigation Buttons
    private Button btnDashboard;
    private Button btnPrepare;
    private Button btnCreate;
    private Button btnStarted;
    private Button btnExplorer;
    private Button btnBrowse;
    private Button btnInstalled;
    private Button btnManager;
    private Button btnSettings;
    
    // Core Layout Panels
    private VBox sidebar;
    private StackPane contentArea;
    
    // Selected Target Project
    private File selectedProjectDir = new File(".");
    private Label lblSelectedProjectName;
    private Label lblSelectedProjectPath;
    private Label lblProjectArtifact;
    private Label lblProjectVersion;
    
    // Loaded Credentials
    private String globalPassphrase = "";
    
    public MainView() {
        // Build Sidebar
        sidebar = createSidebar();
        setLeft(sidebar);
        
        // Build Central Content Area
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(24));
        setCenter(contentArea);
        
        // Default to Dashboard Pane
        showDashboardPane();
        
        // Try loading current directory as a project
        Preferences prefs = Preferences.userNodeForPackage(MainView.class);
        String lastProject = prefs.get("lastProjectDir", ".");
        detectProject(new File(lastProject));

        // Load saved theme preference when Scene is available
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                String currentTheme = prefs.get("theme", "dark");
                applyTheme(currentTheme);
            }
        });
    }
    
    private VBox createSidebar() {
        VBox sidebarBox = new VBox(12);
        sidebarBox.getStyleClass().add("sidebar");
        sidebarBox.setPrefWidth(240);
        
        Label lblBrand = new Label("🚀 JETTRA STORE");
        lblBrand.getStyleClass().add("sidebar-brand");
        
        btnDashboard = createSidebarButton("📊 Dashboard");
        btnPrepare = createSidebarButton("🛠️ Prepare Descriptor");
        btnStarted = createSidebarButton("🚀 Started");
        btnCreate = createSidebarButton("📦 Package & Upload");
        btnExplorer = createSidebarButton("📁 File Explorer");
        btnBrowse = createSidebarButton("🌐 Browse AppStore");
        btnInstalled = createSidebarButton("💾 Installed Plugins");
        btnManager = createSidebarButton("🛠️ Manager");
        btnSettings = createSidebarButton("⚙️ Credentials & Keys");
        
        // Navigation Actions
        btnDashboard.setOnAction(e -> showDashboardPane());
        btnStarted.setOnAction(e -> showStartedPane());
        btnPrepare.setOnAction(e -> showPreparePane());
        btnCreate.setOnAction(e -> showCreatePane());
        btnExplorer.setOnAction(e -> showExplorerPane());
        btnBrowse.setOnAction(e -> showBrowsePane());
        btnInstalled.setOnAction(e -> showInstalledPane());
        btnManager.setOnAction(e -> showManagerPane());
        btnSettings.setOnAction(e -> showSettingsPane());
        
        // Theme Selector
        Preferences prefs = Preferences.userNodeForPackage(MainView.class);
        String savedTheme = prefs.get("theme", "dark");
        
        ComboBox<String> cmbTheme = new ComboBox<>();
        cmbTheme.getItems().addAll("🌙 Dark Theme", "☀️ White Theme");
        cmbTheme.setValue(savedTheme.equals("dark") ? "🌙 Dark Theme" : "☀️ White Theme");
        cmbTheme.setMaxWidth(Double.MAX_VALUE);
        cmbTheme.setOnAction(e -> {
            String themeVal = cmbTheme.getValue().contains("Dark") ? "dark" : "white";
            prefs.put("theme", themeVal);
            applyTheme(themeVal);
        });

        // Add spacer to push theme selector to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        sidebarBox.getChildren().addAll(lblBrand, btnDashboard, btnStarted, btnExplorer, btnPrepare, btnCreate, btnBrowse, btnInstalled, btnManager, new Separator(), btnSettings, spacer, new Label("Appearance:"), cmbTheme);
        return sidebarBox;
    }

    public void applyTheme(String theme) {
        javafx.scene.Scene scene = getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            String cssName = theme.equals("dark") ? "PluginStoreStyle.css" : "PluginStoreLightStyle.css";
            try {
                String cssPath = getClass().getResource("/css/" + cssName).toExternalForm();
                scene.getStylesheets().add(cssPath);
            } catch (Exception e) {
                System.err.println("Could not load JFX stylesheet: " + e.getMessage());
                try {
                    scene.getStylesheets().add("css/" + cssName);
                } catch (Exception ex) {
                    System.err.println("Fallback CSS loading failed too.");
                }
            }
        }
    }
    
    private Button createSidebarButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }
    
    private void setSidebarActive(Button activeButton) {
        btnDashboard.getStyleClass().remove("sidebar-btn-active");
        if (btnStarted != null) btnStarted.getStyleClass().remove("sidebar-btn-active");
        btnPrepare.getStyleClass().remove("sidebar-btn-active");
        btnCreate.getStyleClass().remove("sidebar-btn-active");
        if (btnExplorer != null) btnExplorer.getStyleClass().remove("sidebar-btn-active");
        btnBrowse.getStyleClass().remove("sidebar-btn-active");
        btnInstalled.getStyleClass().remove("sidebar-btn-active");
        if (btnManager != null) btnManager.getStyleClass().remove("sidebar-btn-active");
        btnSettings.getStyleClass().remove("sidebar-btn-active");
        
        activeButton.getStyleClass().add("sidebar-btn-active");
    }
    
    private void switchContent(Node newContent) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(newContent);
    }
    
    private void detectProject(File dir) {
        selectedProjectDir = dir;
        Preferences prefs = Preferences.userNodeForPackage(MainView.class);
        if (dir != null) {
            prefs.put("lastProjectDir", dir.getAbsolutePath());
            File pomFile = new File(dir, "pom.xml");
            if (pomFile.exists()) {
                lblSelectedProjectName.setText(dir.getName().toUpperCase() + " (Maven Project)");
                lblSelectedProjectPath.setText(dir.getAbsolutePath());
                
                // simple parsing of artifactId and version
                try {
                    String pomText = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);
                    String artId = pomText.contains("<artifactId>") ? 
                            pomText.substring(pomText.indexOf("<artifactId>") + 12, pomText.indexOf("</artifactId>")) : "unknown";
                    String ver = pomText.contains("<version>") ? 
                            pomText.substring(pomText.indexOf("<version>") + 9, pomText.indexOf("</version>")) : "1.0-SNAPSHOT";
                    
                    lblProjectArtifact.setText(artId.trim());
                    lblProjectVersion.setText(ver.trim());
                } catch (Exception e) {
                    lblProjectArtifact.setText("unknown");
                    lblProjectVersion.setText("1.0-SNAPSHOT");
                }
            } else {
                lblSelectedProjectName.setText("No Jettra project selected");
                lblSelectedProjectPath.setText("Click the button below to browse and select a valid Jettra Maven project directory.");
                lblProjectArtifact.setText("-");
                lblProjectVersion.setText("-");
            }
        } else {
            lblSelectedProjectName.setText("No Jettra project selected");
            lblSelectedProjectPath.setText("Click the button below to browse and select a valid Jettra Maven project directory.");
            lblProjectArtifact.setText("-");
            lblProjectVersion.setText("-");
        }
    }
    
    // --- 1. Dashboard Pane ---
    private void showDashboardPane() {
        setSidebarActive(btnDashboard);
        
        VBox root = new VBox(20);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("Core Project Overview");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Immersive status report and workspace selector.");
        subtitle.getStyleClass().add("label-subtitle");
        
        VBox infoCard = new VBox(15);
        infoCard.getStyleClass().add("glass-card");
        
        lblSelectedProjectName = new Label("No Jettra project selected");
        lblSelectedProjectName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #06b6d4;");
        
        lblSelectedProjectPath = new Label("Not selected");
        lblSelectedProjectPath.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        lblSelectedProjectPath.setWrapText(true);
        
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));
        
        Label artLabel = new Label("Artifact ID:");
        artLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
        lblProjectArtifact = new Label("-");
        lblProjectArtifact.setStyle("-fx-text-fill: #ffffff;");
        
        Label verLabel = new Label("Version:");
        verLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
        lblProjectVersion = new Label("-");
        lblProjectVersion.setStyle("-fx-text-fill: #ffffff;");
        
        grid.add(artLabel, 0, 0);
        grid.add(lblProjectArtifact, 1, 0);
        grid.add(verLabel, 0, 1);
        grid.add(lblProjectVersion, 1, 1);
        
        infoCard.getChildren().addAll(lblSelectedProjectName, lblSelectedProjectPath, new Separator(), grid);
        
        Button btnBrowseProj = new Button("📂 Select Project Directory");
        btnBrowseProj.getStyleClass().add("cyber-btn");
        btnBrowseProj.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Target Jettra Project");
            File file = chooser.showDialog(new Stage());
            if (file != null) {
                detectProject(file);
            }
        });
        
        Button btnClearProj = new Button("🧹 Limpiar Sesión");
        btnClearProj.getStyleClass().add("cyber-btn-danger");
        btnClearProj.setOnAction(e -> {
            Preferences prefs = Preferences.userNodeForPackage(MainView.class);
            prefs.remove("lastProjectDir");
            detectProject(null);
        });
        
        HBox dashButtons = new HBox(15, btnBrowseProj, btnClearProj);
        
        root.getChildren().addAll(title, subtitle, infoCard, dashButtons);
        switchContent(root);
    }
    
    // --- 1.5 Started Pane ---
    private void showStartedPane() {
        setSidebarActive(btnStarted);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("Jettra Starter");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Generate a new optimized Jettra web application.");
        subtitle.getStyleClass().add("label-subtitle");
        
        ScrollPane formScroll = new ScrollPane();
        formScroll.setFitToWidth(true);
        VBox form = new VBox(15);
        form.setPadding(new Insets(10));
        
        TextField txtGroup = new TextField("com.example");
        TextField txtArtifact = new TextField("mywebapp");
        TextField txtVersion = new TextField("1.0-SNAPSHOT");
        TextField txtJavaVersion = new TextField("25");
        TextField txtDeps = new TextField("JettraServer, JettraWUI, JettraReport");
        
        form.getChildren().addAll(
                new Label("GroupId:"), txtGroup,
                new Label("ArtifactId:"), txtArtifact,
                new Label("Version:"), txtVersion,
                new Label("Java Version:"), txtJavaVersion,
                new Label("Dependencies (comma separated):"), txtDeps,
                new Separator()
        );
        
        form.getChildren().add(new Label("--- jettra-config.properties ---"));
        
        TextField txtTitle = new TextField("Jettra Web 3D Future Dashboard");
        TextField txtShortTitle = new TextField("J");
        TextField txtPort = new TextField("8080");
        TextField txtContext = new TextField("/mywebapp");
        
        // Auto-update context path based on ArtifactId
        txtArtifact.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.isEmpty()) {
                txtContext.setText("/" + newV.toLowerCase().replaceAll("[^a-z0-9]", ""));
            }
        });
        
        CheckBox chkCompact = new CheckBox("Compact Header");
        chkCompact.setSelected(true);
        TextField txtTimeout = new TextField("0");
        TextField txtLang = new TextField("es");
        TextField txtTheme = new TextField("3d");
        CheckBox chkAnimated = new CheckBox("Animated");
        chkAnimated.setSelected(false);
        CheckBox chkHotReload = new CheckBox("Hot Reload");
        chkHotReload.setSelected(true);
        
        form.getChildren().addAll(
                new Label("App Title:"), txtTitle,
                new Label("Short Title:"), txtShortTitle,
                new Label("Server Port:"), txtPort,
                new Label("Context Path:"), txtContext,
                chkCompact,
                new Label("Session Timeout:"), txtTimeout,
                new Label("Language:"), txtLang,
                new Label("Theme:"), txtTheme,
                chkAnimated,
                chkHotReload
        );
        
        form.getChildren().add(new Separator());
        
        Label lblTargetLabel = new Label("Output Directory:");
        lblTargetLabel.setStyle("-fx-font-weight: bold;");
        
        File[] targetDirectory = new File[1];
        if (selectedProjectDir != null && selectedProjectDir.exists()) {
            targetDirectory[0] = selectedProjectDir;
        }
        
        Label lblTargetDir = new Label(targetDirectory[0] != null ? targetDirectory[0].getAbsolutePath() : "No directory selected");
        lblTargetDir.setStyle("-fx-text-fill: #94a3b8;");
        lblTargetDir.setWrapText(true);
        
        Button btnSelectDir = new Button("📂 Select Output Directory");
        btnSelectDir.getStyleClass().add("cyber-btn-secondary");
        btnSelectDir.setOnAction(ev -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Output Directory for New Project");
            File dir = chooser.showDialog(new Stage());
            if (dir != null) {
                targetDirectory[0] = dir;
                lblTargetDir.setText(dir.getAbsolutePath());
            }
        });
        
        form.getChildren().addAll(lblTargetLabel, lblTargetDir, btnSelectDir);
        
        formScroll.setContent(form);
        
        Button btnGenerate = new Button("🚀 Generate Project");
        btnGenerate.getStyleClass().add("cyber-btn");
        
        Label lblStatus = new Label("");
        lblStatus.setStyle("-fx-font-weight: bold;");
        
        btnGenerate.setOnAction(e -> {
            try {
                if (targetDirectory[0] == null || !targetDirectory[0].exists() || !targetDirectory[0].isDirectory()) {
                    lblStatus.setText("❌ Error: You must select a valid output directory.");
                    lblStatus.setTextFill(Color.RED);
                    return;
                }
                
                Map<String, String> props = new HashMap<>();
                props.put("app.title", txtTitle.getText());
                props.put("app.shorttitle", txtShortTitle.getText());
                props.put("server.port", txtPort.getText());
                props.put("server.contextpath", txtContext.getText());
                props.put("server.compactheader", String.valueOf(chkCompact.isSelected()));
                props.put("server.session.timeout", txtTimeout.getText());
                props.put("app.language", txtLang.getText());
                props.put("app.theme", txtTheme.getText());
                props.put("app.animated", String.valueOf(chkAnimated.isSelected()));
                props.put("server.hotreload", String.valueOf(chkHotReload.isSelected()));
                
                com.jettrapluginstore.utils.ProjectGenerator.generateProject(
                        targetDirectory[0], 
                        txtGroup.getText(), 
                        txtArtifact.getText(), 
                        txtVersion.getText(), 
                        txtJavaVersion.getText(), 
                        txtDeps.getText(), 
                        props
                );
                
                File generatedProject = new File(targetDirectory[0], txtArtifact.getText());
                detectProject(generatedProject);
                
                lblStatus.setText("✅ Project " + txtArtifact.getText() + " generated successfully!");
                lblStatus.setTextFill(Color.web("#10b981"));
            } catch (Exception ex) {
                lblStatus.setText("❌ Error: " + ex.getMessage());
                lblStatus.setTextFill(Color.RED);
            }
        });
        
        root.getChildren().addAll(title, subtitle, formScroll, btnGenerate, lblStatus);
        switchContent(root);
    }
    
    // --- 2. Prepare Pane ---
    private void showPreparePane() {
        setSidebarActive(btnPrepare);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("Prepare Plugin Descriptor");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Configures and exports the plugin-descriptor.md file.");
        subtitle.getStyleClass().add("label-subtitle");
        
        // Meta Fields form
        ScrollPane formScroll = new ScrollPane();
        formScroll.setFitToWidth(true);
        VBox form = new VBox(15);
        form.setPadding(new Insets(5, 5, 5, 5));
        
        TextField txtName = new TextField();
        txtName.setPromptText("Example: Custom Facturacion Component");
        TextField txtAuthor = new TextField("avbravo");
        TextField txtEmail = new TextField("avbravo@example.com");
        TextField txtWeb = new TextField("https://github.com/avbravo");
        TextArea txtDesc = new TextArea("Módulo interactivo para gestión avanzada de facturas en Jettra.");
        txtDesc.setPrefHeight(80);
        
        String initialDeps = "jettraServer, JettraReport, JettraWUI";
        if (selectedProjectDir != null && new File(selectedProjectDir, "pom.xml").exists()) {
            initialDeps = parseDependenciesFromPom(new File(selectedProjectDir, "pom.xml"));
        }
        TextField txtDeps = new TextField(initialDeps);
        
        form.getChildren().addAll(
                new Label("Plugin Name:"), txtName,
                new Label("Author:"), txtAuthor,
                new Label("Email:"), txtEmail,
                new Label("Website:"), txtWeb,
                new Label("Description:"), txtDesc,
                new Label("Dependencies (Comma separated):"), txtDeps
        );
        formScroll.setContent(form);
        
        Button btnGenerate = new Button("🛠️ Generate plugin-descriptor.md");
        btnGenerate.getStyleClass().add("cyber-btn");
        
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-weight: bold;");
        
        btnGenerate.setOnAction(e -> {
            if (!new File(selectedProjectDir, "pom.xml").exists()) {
                statusLabel.setText("❌ Error: No valid project selected!");
                statusLabel.setTextFill(Color.RED);
                return;
            }
            try {
                // Call PreparePluginCommand programmatically or write descriptor directly
                // To keep it highly customizable, we will write it directly in the selected folder!
                StringBuilder sb = new StringBuilder();
                sb.append("Name: ").append(txtName.getText().isEmpty() ? lblProjectArtifact.getText() : txtName.getText()).append("\n");
                sb.append("ArtifactId: ").append(lblProjectArtifact.getText().toLowerCase()).append("\n");
                sb.append("Plugin-Package: ").append("com.jettrapluginstore.").append(lblProjectArtifact.getText().toLowerCase()).append("\n");
                sb.append("Versión: ").append(lblProjectVersion.getText()).append("\n");
                sb.append("Autor: ").append(txtAuthor.getText()).append("\n");
                sb.append("Email: ").append(txtEmail.getText()).append("\n");
                sb.append("WebSite: ").append(txtWeb.getText()).append("\n");
                sb.append("Description: ").append(txtDesc.getText()).append("\n");
                sb.append("Dependencies: ").append(txtDeps.getText()).append("\n");
                sb.append("DateTime: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
                
                Files.write(selectedProjectDir.toPath().resolve("plugin-descriptor.md"), sb.toString().getBytes(StandardCharsets.UTF_8));
                
                statusLabel.setText("✅ plugin-descriptor.md created successfully!");
                statusLabel.setTextFill(Color.web("#10b981"));
            } catch (Exception ex) {
                statusLabel.setText("❌ Error: " + ex.getMessage());
                statusLabel.setTextFill(Color.RED);
            }
        });
        
        root.getChildren().addAll(title, subtitle, formScroll, btnGenerate, statusLabel);
        switchContent(root);
    }
    
    private String parseDependenciesFromPom(File pomFile) {
        try {
            String content = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);
            int depsStart = content.indexOf("<dependencies>");
            int depsEnd = content.indexOf("</dependencies>");
            if (depsStart > -1 && depsEnd > depsStart) {
                String depsSection = content.substring(depsStart, depsEnd);
                List<String> deps = new ArrayList<>();
                int idx = 0;
                while ((idx = depsSection.indexOf("<artifactId>", idx)) > -1) {
                    int endIdx = depsSection.indexOf("</artifactId>", idx);
                    if (endIdx > idx) {
                        deps.add(depsSection.substring(idx + 12, endIdx).trim());
                        idx = endIdx;
                    } else {
                        break;
                    }
                }
                if (!deps.isEmpty()) {
                    return String.join(", ", deps);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "jettraServer, JettraReport, JettraWUI";
    }
    
    // --- 3. Create/Upload Pane ---
    private void showCreatePane() {
        setSidebarActive(btnCreate);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("Package & Upload Jettra Plugin");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Refactors, message-prefixes and syncs plugin with github repository.");
        subtitle.getStyleClass().add("label-subtitle");
        
        Label lblInfo = new Label("ℹ️ All existing packages will be included in the plugin automatically.\n" +
                                  "   Classes Main.java, DashboardBasePage.java and DashboardPage.java will be excluded.");
        lblInfo.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 5px;");
        
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Master passphrase for decryption");
        txtPass.setText(globalPassphrase);
        
        TextArea txtConsole = new TextArea("Cyber Terminal ready...\n");
        txtConsole.getStyleClass().add("cyber-terminal");
        txtConsole.setEditable(false);
        txtConsole.setPrefHeight(220);
        
        Button btnRun = new Button("🚀 Compile, Refactor & Upload");
        btnRun.getStyleClass().add("cyber-btn");
        
        btnRun.setOnAction(e -> {
            String secretPhrase = txtPass.getText();
            if (secretPhrase.isEmpty()) {
                txtConsole.appendText("[Error] Secret passphrase is required!\n");
                return;
            }
            globalPassphrase = secretPhrase; // save temporarily
            
            txtConsole.appendText("\n[Process] Initializing plugin packaging...\n");
            
            // Run CreatePluginCommand in background to not block UI
            Thread thread = new Thread(() -> {
                try {
                    // Redirect standard output to catch logs
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos, true, "UTF-8");
                    PrintStream oldOut = System.out;
                    PrintStream oldErr = System.err;
                    System.setOut(ps);
                    System.setErr(ps);
                    
                    try {
                        CreatePluginCommand cmd = new CreatePluginCommand();
                        // Inject parameters
                        java.lang.reflect.Field pkgField = CreatePluginCommand.class.getDeclaredField("packageToConvert");
                        pkgField.setAccessible(true);
                        pkgField.set(cmd, null); // Set to null to take all packages
                        
                        java.lang.reflect.Field dirField = CreatePluginCommand.class.getDeclaredField("targetDir");
                        dirField.setAccessible(true);
                        dirField.set(cmd, selectedProjectDir.getAbsolutePath());
                        
                        java.lang.reflect.Field secField = CreatePluginCommand.class.getDeclaredField("secret");
                        secField.setAccessible(true);
                        secField.set(cmd, secretPhrase);
                        
                        cmd.call();
                    } finally {
                        System.setOut(oldOut);
                        System.setErr(oldErr);
                    }
                    
                    String logs = baos.toString("UTF-8");
                    Platform.runLater(() -> {
                        txtConsole.appendText(logs);
                        txtConsole.appendText("[Process] Completed successfully!\n");
                    });
                    
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        txtConsole.appendText("[Error] " + ex.toString() + "\n");
                    });
                }
            });
            thread.setDaemon(true);
            thread.start();
        });
        
        root.getChildren().addAll(
                lblInfo,
                new Label("Master Passphrase:"), txtPass,
                new Label("Execution Terminal:"), txtConsole,
                btnRun
        );
        switchContent(root);
    }
    
    private void scanPackagesRecursive(File file, String currentPkg, List<String> packages) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            boolean hasJava = false;
            if (children != null) {
                for (File child : children) {
                    if (child.isFile() && child.getName().endsWith(".java")) {
                        hasJava = true;
                    } else if (child.isDirectory()) {
                        String nextPkg = currentPkg.isEmpty() ? child.getName() : currentPkg + "." + child.getName();
                        scanPackagesRecursive(child, nextPkg, packages);
                    }
                }
            }
            if (hasJava && !currentPkg.isEmpty()) {
                packages.add(currentPkg);
            }
        }
    }
    
    // --- 4. Browse AppStore Pane ---
    private void showBrowsePane() {
        setSidebarActive(btnBrowse);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("GitHub Plugin Repository Store");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Select and integrate Jettra plugins directly from github.");
        subtitle.getStyleClass().add("label-subtitle");
        
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Enter passphrase to clone and load the AppStore");
        txtPass.setText(globalPassphrase);
        
        Button btnLoad = new Button("🌐 Load AppStore");
        btnLoad.getStyleClass().add("cyber-btn");
        
        ScrollPane cardsScroll = new ScrollPane();
        cardsScroll.setFitToWidth(true);
        cardsScroll.setPrefHeight(350);
        
        VBox cardsContainer = new VBox(15);
        cardsScroll.setContent(cardsContainer);
        
        btnLoad.setOnAction(e -> {
            String passphrase = txtPass.getText();
            if (passphrase.isEmpty()) {
                showAlert("Passphrase required", "Enter the master passphrase to decrypt and connect with GitHub!");
                return;
            }
            globalPassphrase = passphrase;
            cardsContainer.getChildren().clear();
            cardsContainer.getChildren().add(new Label("🔄 Connecting and cloning GitHub JettraAppStore..."));
            
            Thread thread = new Thread(() -> {
                try {
                    List<Map<String, String>> plugins = ListPluginsCommand.fetchPlugins(passphrase);
                    Platform.runLater(() -> {
                        cardsContainer.getChildren().clear();
                        if (plugins.isEmpty()) {
                            cardsContainer.getChildren().add(new Label("No available plugins found in GitHub store repository."));
                            return;
                        }
                        
                        for (Map<String, String> plugin : plugins) {
                            VBox card = new VBox(8);
                            card.getStyleClass().add("glass-card");
                            
                            Label lblName = new Label(plugin.getOrDefault("Name", "Unnamed Plugin"));
                            lblName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #06b6d4;");
                            
                            Label lblMeta = new Label("Artifact ID: " + plugin.getOrDefault("Artifactid", "") + 
                                    " | Version: " + plugin.getOrDefault("Versión", "") + 
                                    " | Author: " + plugin.getOrDefault("Autor", ""));
                            lblMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                            
                            Label lblDesc = new Label(plugin.getOrDefault("Description", "No description provided."));
                            lblDesc.setWrapText(true);
                            lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1f5f9;");
                            
                            Label lblDeps = new Label("Dependencies: " + plugin.getOrDefault("Dependencies", ""));
                            lblDeps.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #bd93f9;");
                            
                            Button btnInstall = new Button("📥 One-Click Install");
                            btnInstall.getStyleClass().add("cyber-btn");
                            
                            btnInstall.setOnAction(ev -> {
                                btnInstall.setDisable(true);
                                btnInstall.setText("Installing...");
                                
                                Thread installThread = new Thread(() -> {
                                    try {
                                        InstallPluginCommand cmd = new InstallPluginCommand();
                                        
                                        java.lang.reflect.Field artField = InstallPluginCommand.class.getDeclaredField("artifactId");
                                        artField.setAccessible(true);
                                        artField.set(cmd, plugin.get("Artifactid"));
                                        
                                        java.lang.reflect.Field verField = InstallPluginCommand.class.getDeclaredField("version");
                                        verField.setAccessible(true);
                                        verField.set(cmd, plugin.get("Versión"));
                                        
                                        java.lang.reflect.Field dirField = InstallPluginCommand.class.getDeclaredField("targetDir");
                                        dirField.setAccessible(true);
                                        dirField.set(cmd, selectedProjectDir.getAbsolutePath());
                                        
                                        java.lang.reflect.Field secField = InstallPluginCommand.class.getDeclaredField("secret");
                                        secField.setAccessible(true);
                                        secField.set(cmd, passphrase);
                                        
                                        cmd.call();
                                        
                                        // Read report result
                                        Path reportPath = selectedProjectDir.toPath().resolve("plugin-result.md");
                                        String report = Files.exists(reportPath) ? 
                                                new String(Files.readAllBytes(reportPath), StandardCharsets.UTF_8) : "";
                                                
                                        Platform.runLater(() -> {
                                            btnInstall.setText("Successfully Installed");
                                            btnInstall.setStyle("-fx-background-color: #10b981;");
                                            
                                            // Display report in detail dialog
                                            TextArea taReport = new TextArea(report);
                                            taReport.setEditable(false);
                                            taReport.getStyleClass().add("cyber-terminal");
                                            
                                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                            alert.setTitle("Installation Complete!");
                                            alert.setHeaderText("Plugin integration report (plugin-result.md):");
                                            alert.getDialogPane().setContent(taReport);
                                            alert.getDialogPane().setPrefWidth(650);
                                            alert.getDialogPane().setPrefHeight(450);
                                            alert.showAndWait();
                                        });
                                        
                                    } catch (Exception ex) {
                                        Platform.runLater(() -> {
                                            btnInstall.setDisable(false);
                                            btnInstall.setText("📥 One-Click Install");
                                            showAlert("Installation Failed", ex.getMessage());
                                        });
                                    }
                                });
                                installThread.setDaemon(true);
                                installThread.start();
                            });
                            
                            card.getChildren().addAll(lblName, lblMeta, lblDesc, lblDeps, btnInstall);
                            cardsContainer.getChildren().add(card);
                        }
                    });
                    
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        cardsContainer.getChildren().clear();
                        cardsContainer.getChildren().add(new Label("❌ Error: " + ex.getMessage()));
                    });
                }
            });
            thread.setDaemon(true);
            thread.start();
        });
        
        root.getChildren().addAll(title, subtitle, new Label("Enter passphrase:"), txtPass, btnLoad, cardsScroll);
        switchContent(root);
    }
    
    // --- 4.5 Explorer Pane ---
    private void showExplorerPane() {
        setSidebarActive(btnExplorer);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("Project File Explorer");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Explore and edit the structure of the selected project.");
        subtitle.getStyleClass().add("label-subtitle");
        
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.3);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        TreeView<File> treeView = new TreeView<>();
        treeView.setCellFactory(tv -> new javafx.scene.control.TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        
        if (selectedProjectDir != null && selectedProjectDir.exists() && selectedProjectDir.isDirectory()) {
            TreeItem<File> rootItem = createTreeItem(selectedProjectDir);
            treeView.setRoot(rootItem);
        } else {
            treeView.setRoot(new TreeItem<>(new File("No project selected")));
        }
        
        VBox rightPane = new VBox(10);
        Label lblEditing = new Label("No file selected.");
        lblEditing.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        
        TextArea txtEditor = new TextArea();
        txtEditor.getStyleClass().add("cyber-textarea");
        txtEditor.setStyle("-fx-font-family: monospace;");
        VBox.setVgrow(txtEditor, Priority.ALWAYS);
        
        Button btnSave = new Button("💾 Save Changes");
        btnSave.getStyleClass().add("cyber-btn");
        btnSave.setDisable(true);
        
        File[] currentEditingFile = new File[1];
        
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<File> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue() != null && item.getValue().isFile()) {
                    File file = item.getValue();
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        txtEditor.setText(content);
                        lblEditing.setText("Editing: " + file.getAbsolutePath());
                        currentEditingFile[0] = file;
                        btnSave.setDisable(false);
                    } catch (IOException ex) {
                        showAlert("Error", "Could not read file: " + ex.getMessage());
                    }
                }
            }
        });
        
        btnSave.setOnAction(e -> {
            if (currentEditingFile[0] != null) {
                try {
                    Files.write(currentEditingFile[0].toPath(), txtEditor.getText().getBytes(StandardCharsets.UTF_8));
                    showAlert("Success", "File saved successfully.");
                } catch (IOException ex) {
                    showAlert("Error", "Could not save file: " + ex.getMessage());
                }
            }
        });
        
        rightPane.getChildren().addAll(lblEditing, txtEditor, btnSave);
        splitPane.getItems().addAll(treeView, rightPane);
        
        root.getChildren().addAll(title, subtitle, splitPane);
        switchContent(root);
    }
    
    private TreeItem<File> createTreeItem(File f) {
        TreeItem<File> item = new TreeItem<>(f);
        if (f.isDirectory()) {
            item.setExpanded(true);
            File[] files = f.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : files) {
                    item.getChildren().add(createTreeItem(child));
                }
            }
        }
        return item;
    }
    
    // --- 5. Installed Manager Pane ---
    private void showInstalledPane() {
        setSidebarActive(btnInstalled);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("Installed Plugins Control Pane");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Examine and remove integrated modules safely.");
        subtitle.getStyleClass().add("label-subtitle");
        
        VBox listContainer = new VBox(10);
        
        // Scan for targetDir/.jettra/plugins/*
        File pluginsDir = new File(selectedProjectDir, ".jettra/plugins");
        if (pluginsDir.exists() && pluginsDir.isDirectory()) {
            File[] files = pluginsDir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String pluginId = file.getName();
                        
                        HBox row = new HBox(15);
                        row.getStyleClass().add("glass-card");
                        row.setAlignment(Pos.CENTER_LEFT);
                        
                        VBox textCol = new VBox(5);
                        Label name = new Label(pluginId.toUpperCase());
                        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #06b6d4; -fx-font-size: 15px;");
                        
                        Label desc = new Label("Installed and active in Jettra ecosystem.");
                        desc.setStyle("-fx-text-fill: #cbd5e1;");
                        
                        textCol.getChildren().addAll(name, desc);
                        
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        
                        Button btnViewResult = new Button("📄 View integration report");
                        btnViewResult.getStyleClass().add("cyber-btn-secondary");
                        btnViewResult.setOnAction(e -> {
                            Path resultPath = selectedProjectDir.toPath().resolve("plugin-result.md");
                            if (Files.exists(resultPath)) {
                                try {
                                    String text = new String(Files.readAllBytes(resultPath), StandardCharsets.UTF_8);
                                    TextArea ta = new TextArea(text);
                                    ta.getStyleClass().add("cyber-terminal");
                                    ta.setEditable(false);
                                    
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle(pluginId + " Integration Report");
                                    alert.setHeaderText("Report:");
                                    alert.getDialogPane().setContent(ta);
                                    alert.getDialogPane().setPrefWidth(600);
                                    alert.getDialogPane().setPrefHeight(400);
                                    alert.showAndWait();
                                } catch (Exception ex) {
                                    showAlert("Error", "Could not load report.");
                                }
                            } else {
                                showAlert("Report not found", "No plugin-result.md report file exists.");
                            }
                        });
                        
                        Button btnRemove = new Button("🗑️ Remove");
                        btnRemove.getStyleClass().add("cyber-btn-danger");
                        btnRemove.setOnAction(e -> {
                            btnRemove.setDisable(true);
                            btnRemove.setText("Removing...");
                            
                            Thread thread = new Thread(() -> {
                                try {
                                    RemovePluginCommand cmd = new RemovePluginCommand();
                                    
                                    java.lang.reflect.Field artField = RemovePluginCommand.class.getDeclaredField("artifactId");
                                    artField.setAccessible(true);
                                    artField.set(cmd, pluginId);
                                    
                                    java.lang.reflect.Field dirField = RemovePluginCommand.class.getDeclaredField("targetDir");
                                    dirField.setAccessible(true);
                                    dirField.set(cmd, selectedProjectDir.getAbsolutePath());
                                    
                                    cmd.call();
                                    
                                    Platform.runLater(() -> {
                                        showAlert("Removed Successfully", "Plugin " + pluginId + " was uninstalled and all configuration stripped.");
                                        showInstalledPane(); // reload
                                    });
                                } catch (Exception ex) {
                                    Platform.runLater(() -> {
                                        btnRemove.setDisable(false);
                                        btnRemove.setText("🗑️ Remove");
                                        showAlert("Uninstall Failed", ex.getMessage());
                                    });
                                }
                            });
                            thread.setDaemon(true);
                            thread.start();
                        });
                        
                        row.getChildren().addAll(textCol, spacer, btnViewResult, btnRemove);
                        listContainer.getChildren().add(row);
                    }
                }
            } else {
                listContainer.getChildren().add(new Label("No plugins currently installed."));
            }
        } else {
            listContainer.getChildren().add(new Label("No plugins currently installed."));
        }
        
        root.getChildren().addAll(title, subtitle, listContainer);
        switchContent(root);
    }
    
    // --- 5.5 AppStore Manager Pane ---
    private void showManagerPane() {
        setSidebarActive(btnManager);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("AppStore Manager");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Manage plugins in the GitHub repository.");
        subtitle.getStyleClass().add("label-subtitle");
        
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Enter passphrase to clone the AppStore");
        txtPass.setText(globalPassphrase);
        
        Button btnLoad = new Button("🌐 Load AppStore");
        btnLoad.getStyleClass().add("cyber-btn");
        
        ScrollPane cardsScroll = new ScrollPane();
        cardsScroll.setFitToWidth(true);
        cardsScroll.setPrefHeight(350);
        
        VBox cardsContainer = new VBox(15);
        cardsScroll.setContent(cardsContainer);
        
        btnLoad.setOnAction(e -> {
            String passphrase = txtPass.getText();
            if (passphrase.isEmpty()) {
                showAlert("Passphrase required", "Enter the master passphrase!");
                return;
            }
            globalPassphrase = passphrase;
            cardsContainer.getChildren().clear();
            cardsContainer.getChildren().add(new Label("🔄 Connecting and cloning GitHub JettraAppStore..."));
            
            Thread thread = new Thread(() -> {
                try {
                    List<Map<String, String>> plugins = ListPluginsCommand.fetchPlugins(passphrase);
                    Platform.runLater(() -> {
                        cardsContainer.getChildren().clear();
                        if (plugins.isEmpty()) {
                            cardsContainer.getChildren().add(new Label("No available plugins found."));
                            return;
                        }
                        
                        for (Map<String, String> plugin : plugins) {
                            VBox card = new VBox(8);
                            card.getStyleClass().add("glass-card");
                            
                            Label lblName = new Label(plugin.getOrDefault("Name", "Unnamed Plugin"));
                            lblName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #06b6d4;");
                            
                            Label lblMeta = new Label("Artifact ID: " + plugin.getOrDefault("Artifactid", "") + 
                                    " | Version: " + plugin.getOrDefault("Versión", ""));
                            lblMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                            
                            Button btnDelete = new Button("🗑️ Delete from AppStore");
                            btnDelete.getStyleClass().add("cyber-btn-danger");
                            
                            btnDelete.setOnAction(ev -> {
                                btnDelete.setDisable(true);
                                btnDelete.setText("Deleting...");
                                
                                Thread deleteThread = new Thread(() -> {
                                    try {
                                        deletePluginFromAppStore(plugin.get("Artifactid"), plugin.get("Versión"), passphrase);
                                        Platform.runLater(() -> {
                                            btnLoad.fire(); // Reload
                                            showAlert("Deleted", "Plugin deleted successfully from AppStore.");
                                        });
                                    } catch (Exception ex) {
                                        Platform.runLater(() -> {
                                            btnDelete.setDisable(false);
                                            btnDelete.setText("🗑️ Delete from AppStore");
                                            showAlert("Deletion Failed", ex.getMessage());
                                        });
                                    }
                                });
                                deleteThread.setDaemon(true);
                                deleteThread.start();
                            });
                            
                            card.getChildren().addAll(lblName, lblMeta, btnDelete);
                            cardsContainer.getChildren().add(card);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        cardsContainer.getChildren().clear();
                        cardsContainer.getChildren().add(new Label("❌ Error: " + ex.getMessage()));
                    });
                }
            });
            thread.setDaemon(true);
            thread.start();
        });
        
        root.getChildren().addAll(title, subtitle, new Label("Enter passphrase:"), txtPass, btnLoad, cardsScroll);
        switchContent(root);
    }

    private void deletePluginFromAppStore(String artifactId, String version, String passphrase) throws Exception {
        String[] creds = CredentialsManager.loadCredentials(passphrase);
        if (creds == null) throw new Exception("Invalid credentials");
        String user = creds[0];
        String pat = creds[1];

        Path cloneDir = Files.createTempDirectory("jettra-appstore-del");
        try (org.eclipse.jgit.api.Git git = com.jettrapluginstore.utils.GitUtils.cloneRepository(cloneDir)) {
            // Remove the directory
            Path pluginDir = cloneDir.resolve(artifactId).resolve(version);
            if (Files.exists(pluginDir)) {
                com.jettrapluginstore.utils.GitUtils.deleteDirectory(pluginDir.toFile());
            }

            // Remove from db/plugin-db.md
            Path dbPath = cloneDir.resolve("db/plugin-db.md");
            if (Files.exists(dbPath)) {
                List<String> lines = Files.readAllLines(dbPath, StandardCharsets.UTF_8);
                List<String> newLines = new ArrayList<>();
                if (!lines.isEmpty()) newLines.add(lines.get(0)); // header
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String[] parts = line.split(",");
                    if (parts.length > 3) {
                        String lineArtifact = parts[1].trim();
                        String lineVersion = parts[3].trim();
                        if (!lineArtifact.equalsIgnoreCase(artifactId) || !lineVersion.equalsIgnoreCase(version)) {
                            newLines.add(line);
                        }
                    } else {
                        newLines.add(line);
                    }
                }
                Files.write(dbPath, newLines, StandardCharsets.UTF_8);
            }

            com.jettrapluginstore.utils.GitUtils.commitAndPush(git, user, pat, "Delete plugin " + artifactId + " v" + version);
        } finally {
            com.jettrapluginstore.utils.GitUtils.deleteDirectory(cloneDir.toFile());
        }
    }
    
    // --- 6. Settings Pane ---
    private void showSettingsPane() {
        setSidebarActive(btnSettings);
        
        VBox root = new VBox(15);
        root.getStyleClass().add("glass-pane");
        
        Label title = new Label("Credentials Crypt Locker");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Encrypt and manage your GitHub tokens safely.");
        subtitle.getStyleClass().add("label-subtitle");
        
        TextField txtUser = new TextField();
        txtUser.setPromptText("GitHub username");
        
        PasswordField txtPat = new PasswordField();
        txtPat.setPromptText("GitHub Personal Access Token (PAT)");
        
        PasswordField txtSecret = new PasswordField();
        txtSecret.setPromptText("Passphrase phrase (to encrypt/decrypt locally)");
        txtSecret.setText(globalPassphrase);
        
        Button btnSave = new Button("💾 Encrypt and Save Credentials");
        btnSave.getStyleClass().add("cyber-btn");
        
        Label status = new Label("");
        status.setStyle("-fx-font-weight: bold;");
        
        btnSave.setOnAction(e -> {
            String user = txtUser.getText();
            String pat = txtPat.getText();
            String secret = txtSecret.getText();
            
            if (user.isEmpty() || pat.isEmpty() || secret.isEmpty()) {
                status.setText("❌ All fields must be filled!");
                status.setTextFill(Color.RED);
                return;
            }
            
            try {
                CredentialsManager.saveCredentials(user, pat, secret);
                globalPassphrase = secret;
                status.setText("✅ Encrypted and saved to jettraappstore.md!");
                status.setTextFill(Color.web("#10b981"));
            } catch (Exception ex) {
                status.setText("❌ Encryption failed: " + ex.getMessage());
                status.setTextFill(Color.RED);
            }
        });
        
        Button btnDecrypt = new Button("🔓 Decrypt & Verify Saved Locker");
        btnDecrypt.getStyleClass().add("cyber-btn-secondary");
        btnDecrypt.setOnAction(e -> {
            String secret = txtSecret.getText();
            if (secret.isEmpty()) {
                status.setText("❌ Enter passphrase first!");
                status.setTextFill(Color.RED);
                return;
            }
            try {
                String[] creds = CredentialsManager.loadCredentials(secret);
                if (creds != null) {
                    txtUser.setText(creds[0]);
                    txtPat.setText(creds[1]);
                    globalPassphrase = secret;
                    status.setText("✅ Credentials successfully decrypted and loaded!");
                    status.setTextFill(Color.web("#10b981"));
                } else {
                    status.setText("❌ Decryption failed! Wrong passphrase.");
                    status.setTextFill(Color.RED);
                }
            } catch (Exception ex) {
                status.setText("❌ Verification failed.");
                status.setTextFill(Color.RED);
            }
        });
        
        root.getChildren().addAll(
                title, subtitle,
                new Label("GitHub Username:"), txtUser,
                new Label("GitHub Token/Password:"), txtPat,
                new Label("Passphrase Key:"), txtSecret,
                btnSave, btnDecrypt, status
        );
        switchContent(root);
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
