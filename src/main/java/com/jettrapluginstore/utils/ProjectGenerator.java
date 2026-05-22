package com.jettrapluginstore.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ProjectGenerator {

    public static void generateProject(File parentDir, String groupId, String artifactId, String version, String javaVersion, String dependencies, Map<String, String> properties) throws IOException {
        File projectDir = new File(parentDir, artifactId);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        // 1. pom.xml
        generatePom(projectDir, groupId, artifactId, version, javaVersion, dependencies);

        // 2. Dockerfile
        generateDockerfile(projectDir, artifactId, version);

        // 3. jettra-config.properties & messages.properties
        File resourcesDir = new File(projectDir, "src/main/resources");
        resourcesDir.mkdirs();
        generateProperties(resourcesDir, properties);
        generateMessages(resourcesDir);

        // 4. Java Sources
        String packagePath = groupId.replace('.', '/');
        File basePackageDir = new File(projectDir, "src/main/java/" + packagePath);
        basePackageDir.mkdirs();

        // Main.java
        generateMain(basePackageDir, groupId);

        // dashboard package
        File dashboardDir = new File(basePackageDir, "dashboard");
        dashboardDir.mkdirs();
        generateDashboardBase(dashboardDir, groupId);
        generateDashboard(dashboardDir, groupId);

        // pages package
        File pagesDir = new File(basePackageDir, "pages");
        pagesDir.mkdirs();
        generateLogin(pagesDir, groupId);
        generateLoginAdvanced(pagesDir, groupId);
        
        // README.md
        generateReadme(projectDir, artifactId);
    }

    private static void generatePom(File projectDir, String groupId, String artifactId, String version, String javaVersion, String dependencies) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("    <modelVersion>4.0.0</modelVersion>\n\n");
        sb.append("    <groupId>").append(groupId).append("</groupId>\n");
        sb.append("    <artifactId>").append(artifactId).append("</artifactId>\n");
        sb.append("    <version>").append(version).append("</version>\n\n");
        sb.append("    <properties>\n");
        sb.append("        <maven.compiler.source>").append(javaVersion).append("</maven.compiler.source>\n");
        sb.append("        <maven.compiler.target>").append(javaVersion).append("</maven.compiler.target>\n");
        sb.append("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n");
        sb.append("    </properties>\n\n");
        sb.append("    <dependencies>\n");

        String[] depsList = dependencies.split(",");
        for (String dep : depsList) {
            String d = dep.trim();
            if (!d.isEmpty()) {
                String depGroupId = d.equalsIgnoreCase("JettraWUI") ? "io.jettra.wui" : "com.jettra";
                String depVersion = d.equalsIgnoreCase("JettraWUI") ? "1.0.0-SNAPSHOT" : "1.0-SNAPSHOT";
                sb.append("        <dependency>\n");
                sb.append("            <groupId>").append(depGroupId).append("</groupId>\n");
                sb.append("            <artifactId>").append(d).append("</artifactId>\n");
                sb.append("            <version>").append(depVersion).append("</version>\n");
                sb.append("        </dependency>\n");
            }
        }
        sb.append("    </dependencies>\n\n");
        sb.append("    <build>\n");
        sb.append("        <plugins>\n");
        sb.append("            <plugin>\n");
        sb.append("                <groupId>org.apache.maven.plugins</groupId>\n");
        sb.append("                <artifactId>maven-compiler-plugin</artifactId>\n");
        sb.append("                <version>3.11.0</version>\n");
        sb.append("            </plugin>\n");
        sb.append("            <plugin>\n");
        sb.append("                <groupId>org.apache.maven.plugins</groupId>\n");
        sb.append("                <artifactId>maven-jar-plugin</artifactId>\n");
        sb.append("                <version>3.3.0</version>\n");
        sb.append("                <configuration>\n");
        sb.append("                    <archive>\n");
        sb.append("                        <manifest>\n");
        sb.append("                            <mainClass>").append(groupId).append(".Main</mainClass>\n");
        sb.append("                        </manifest>\n");
        sb.append("                    </archive>\n");
        sb.append("                </configuration>\n");
        sb.append("            </plugin>\n");
        sb.append("            <plugin>\n");
        sb.append("                <groupId>org.apache.maven.plugins</groupId>\n");
        sb.append("                <artifactId>maven-shade-plugin</artifactId>\n");
        sb.append("                <version>3.5.1</version>\n");
        sb.append("                <executions>\n");
        sb.append("                    <execution>\n");
        sb.append("                        <phase>package</phase>\n");
        sb.append("                        <goals>\n");
        sb.append("                            <goal>shade</goal>\n");
        sb.append("                        </goals>\n");
        sb.append("                        <configuration>\n");
        sb.append("                            <createDependencyReducedPom>false</createDependencyReducedPom>\n");
        sb.append("                            <transformers>\n");
        sb.append("                                <transformer implementation=\"org.apache.maven.plugins.shade.resource.ManifestResourceTransformer\">\n");
        sb.append("                                    <mainClass>").append(groupId).append(".Main</mainClass>\n");
        sb.append("                                </transformer>\n");
        sb.append("                            </transformers>\n");
        sb.append("                        </configuration>\n");
        sb.append("                    </execution>\n");
        sb.append("                </executions>\n");
        sb.append("            </plugin>\n");
        sb.append("        </plugins>\n");
        sb.append("    </build>\n");
        sb.append("</project>\n");

        Files.write(new File(projectDir, "pom.xml").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateDockerfile(File projectDir, String artifactId, String version) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("FROM bellsoft/liberica-runtime-container:jre-25-alpaquita\n\n");
        sb.append("LABEL maintainer=\"Generated by JettraPluginStore\"\n");
        sb.append("LABEL description=\"Optimized container for ").append(artifactId).append("\"\n\n");
        sb.append("WORKDIR /app\n\n");
        sb.append("COPY target/").append(artifactId).append("-").append(version).append(".jar /app/").append(artifactId).append(".jar\n\n");
        sb.append("# Train JVM for AOT Cache (AppCDS)\n");
        sb.append("RUN timeout 10s java -XX:ArchiveClassesAtExit=app.jsa -jar ").append(artifactId).append(".jar || true\n\n");
        sb.append("EXPOSE 8080\n\n");
        sb.append("ENV JAVA_OPTS=\"-XX:+UseZGC -XX:+ZGenerational -Xmx512m -XX:MaxRAMPercentage=75.0 -XX:SharedArchiveFile=app.jsa\"\n\n");
        sb.append("ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar ").append(artifactId).append(".jar\"]\n");

        Files.write(new File(projectDir, "Dockerfile").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateProperties(File resourcesDir, Map<String, String> properties) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        Files.write(new File(resourcesDir, "jettra-config.properties").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateMessages(File resourcesDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Jettra App standard messages\n");
        sb.append("app.welcome=Welcome to your Jettra App\n");
        sb.append("btn.save=Save\n");
        sb.append("btn.cancel=Cancel\n");
        sb.append("btn.edit=Edit\n");
        sb.append("btn.delete=Delete\n");
        Files.write(new File(resourcesDir, "messages.properties").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateMain(File pkgDir, String groupId) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(groupId).append(";\n\n");
        sb.append("import ").append(groupId).append(".pages.LoginPage;\n");
        sb.append("import ").append(groupId).append(".pages.LoginAdvancedPage;\n");
        sb.append("import ").append(groupId).append(".dashboard.DashboardPage;\n");
        sb.append("import com.jettra.server.JettraServer;\n");
        sb.append("import com.jettra.server.config.JettraConfigProperty;\n");
        sb.append("import com.jettra.server.config.ConfigInjector;\n\n");
        sb.append("public class Main {\n\n");
        sb.append("    @JettraConfigProperty(name = \"app.title\")\n");
        sb.append("    private String appTitle;\n");
        sb.append("    @JettraConfigProperty(name = \"server.port\")\n");
        sb.append("    private String port;\n");
        sb.append("    @JettraConfigProperty(name = \"server.contextpath\")\n");
        sb.append("    private String contextpath;\n\n");
        sb.append("    public void initUI() {\n");
        sb.append("        ConfigInjector.inject(this);\n");
        sb.append("        System.out.println(\"Starting Web Application: \" + appTitle);\n");
        sb.append("    }\n\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        Main app = new Main();\n");
        sb.append("        app.initUI();\n\n");
        sb.append("        io.jettra.wui.complex.ErrorPage.path = \"http://localhost:\" + app.port + app.contextpath;\n\n");
        sb.append("        System.out.println(\"Starting JettraServer...\");\n");
        sb.append("        JettraServer server = new JettraServer();\n");
        sb.append("        server.setErrorPage(\"/error\");\n\n");
        sb.append("        server.addHandler(\"/error\", io.jettra.wui.complex.ErrorPage.class);\n");
        sb.append("        server.addHandler(\"/\", LoginPage.class);\n");
        sb.append("        server.addHandler(\"/login\", LoginPage.class);\n");
        sb.append("        server.addHandler(\"/logout\", LoginPage.class);\n");
        sb.append("        server.addHandler(\"/loginadvanced\", LoginAdvancedPage.class);\n");
        sb.append("        server.addHandler(\"/dashboard\", DashboardPage.class);\n\n");
        sb.append("        server.start();\n");
        sb.append("    }\n");
        sb.append("}\n");
        Files.write(new File(pkgDir, "Main.java").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateDashboardBase(File pkgDir, String groupId) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(groupId).append(".dashboard;\n\n");
        sb.append("import io.jettra.wui.complex.Left;\n");
        sb.append("import io.jettra.wui.core.JettraDashboardPage;\n\n");
        sb.append("public abstract class DashboardBasePage extends JettraDashboardPage {\n\n");
        sb.append("    public DashboardBasePage(String title) {\n");
        sb.append("        super(title);\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    protected void setupLeft(Left left, String username) {\n");
        sb.append("        initMenuBuilder();\n\n");
        sb.append("        String compIcon = \"<svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='#0ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' style='opacity:0.7;'><path d='M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z'></path></svg>\";\n\n");
        sb.append("        addCategory(\"Navigation\", new String[]{}, \"\");\n");
        sb.append("        appendMenuItem(\"Main Dashboard\", \"/dashboard\", \"<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='#0ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='4 17 10 11 4 5'></polyline><line x1='12' y1='19' x2='20' y2='19'></line></svg>\");\n\n");
        sb.append("        menuHtmlBuilder.append(\"<div style='margin-top:20px;'></div>\");\n");
        sb.append("        appendMenuItem(\"Logout\", \"/logout\", \"<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='#0ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'></path><polyline points='16 17 21 12 16 7'></polyline></svg>\");\n\n");
        sb.append("        finishMenuBuilder(left);\n");
        sb.append("    }\n");
        sb.append("}\n");
        Files.write(new File(pkgDir, "DashboardBasePage.java").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateDashboard(File pkgDir, String groupId) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(groupId).append(".dashboard;\n\n");
        sb.append("import ").append(groupId).append(".dashboard.DashboardBasePage;\n");
        sb.append("import io.jettra.wui.complex.Center;\n\n");
        sb.append("public class DashboardPage extends DashboardBasePage {\n\n");
        sb.append("    public DashboardPage() {\n");
        sb.append("        super(\"Dashboard\");\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    protected void initCenter(Center center, String username) {\n");
        sb.append("        center.setContent(\"<h1>Main Workspace</h1><p>Status: All systems online. Jettra native servers operating at normal capacity. Welcome to the future of immersive dashboard control systems.</p>\");\n");
        sb.append("    }\n");
        sb.append("}\n");
        Files.write(new File(pkgDir, "DashboardPage.java").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateLogin(File pkgDir, String groupId) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(groupId).append(".pages;\n\n");
        sb.append("import java.io.IOException;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("import com.jettra.server.JettraServer;\n");
        sb.append("import io.jettra.wui.components.Login;\n");
        sb.append("import io.jettra.wui.components.Notification;\n");
        sb.append("import io.jettra.wui.core.Page;\n\n");
        sb.append("public class LoginPage extends Page {\n\n");
        sb.append("    private Login loginForm;\n\n");
        sb.append("    public LoginPage() {\n");
        sb.append("        super(\"Login\");\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    protected void onInit(Map<String, String> params) {\n");
        sb.append("        // Handle Logout\n");
        sb.append("        if (params.containsKey(\"logout\") || \"/logout\".equals(getRequestPath())) {\n");
        sb.append("            logout();\n");
        sb.append("            return;\n");
        sb.append("        }\n\n");
        sb.append("        loginForm = new Login(JettraServer.resolvePath(\"/login\"));\n");
        sb.append("        loginForm.setApplicationName(\"Jettra Web Example\");\n");
        sb.append("        add(loginForm);\n\n");
        sb.append("        if (params.containsKey(\"error\")) {\n");
        sb.append("            Notification notif = new Notification();\n");
        sb.append("            notif.setType(\"error\");\n");
        sb.append("            notif.showMessage(\"Error: Username y/o password no válidos\");\n");
        sb.append("            add(notif);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    protected void onPost(Map<String, String> params) {\n");
        sb.append("        String user = params.get(\"username\");\n");
        sb.append("        String pass = params.get(\"password\");\n\n");
        sb.append("        System.out.println(\"[LoginPage] POST login attempt: \" + user);\n\n");
        sb.append("        if (isValidUser(user, pass)) {\n");
        sb.append("            String cPath = JettraServer.getContextPath();\n");
        sb.append("            if (cPath == null || cPath.isEmpty()) cPath = \"/\";\n\n");
        sb.append("            System.out.println(\"[LoginPage] Success: user=\" + user + \" | cookiePath=\" + cPath);\n");
        sb.append("            setSessionCookie(user, cPath);\n\n");
        sb.append("            try {\n");
        sb.append("                redirect(currentExchange, JettraServer.resolvePath(\"/dashboard\"));\n");
        sb.append("            } catch (IOException e) {\n");
        sb.append("                e.printStackTrace();\n");
        sb.append("            }\n");
        sb.append("        } else {\n");
        sb.append("            System.out.println(\"[LoginPage] Failure: user=\" + user);\n");
        sb.append("            try {\n");
        sb.append("                redirect(currentExchange, JettraServer.resolvePath(\"/login?error=invalid_credentials\"));\n");
        sb.append("            } catch (IOException e) {\n");
        sb.append("                e.printStackTrace();\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private String getRequestPath() {\n");
        sb.append("        return currentExchange.getRequestURI().getPath();\n");
        sb.append("    }\n\n");
        sb.append("    private void logout() {\n");
        sb.append("        String cPath = JettraServer.getContextPath();\n");
        sb.append("        currentExchange.getResponseHeaders().set(\"Set-Cookie\", \"username=; Path=\" + cPath + \"; Max-Age=0\");\n");
        sb.append("        try {\n");
        sb.append("            redirect(currentExchange, JettraServer.resolvePath(\"/login\"));\n");
        sb.append("        } catch (IOException e) {\n");
        sb.append("            e.printStackTrace();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private void setSessionCookie(String user, String path) {\n");
        sb.append("        currentExchange.getResponseHeaders().set(\"Set-Cookie\", \"username=\" + user + \"; Path=\" + path);\n");
        sb.append("    }\n\n");
        sb.append("    private boolean isValidUser(String user, String pass) {\n");
        sb.append("        return (\"admin\".equals(user) && \"admin\".equals(pass)) || \n");
        sb.append("               (\"demo\".equals(user) && \"demo\".equals(pass)) || \n");
        sb.append("               (\"avbravo\".equals(user) && \"avbravo\".equals(pass));\n");
        sb.append("    }\n");
        sb.append("}\n");
        Files.write(new File(pkgDir, "LoginPage.java").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateLoginAdvanced(File pkgDir, String groupId) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(groupId).append(".pages;\n\n");
        sb.append("import ").append(groupId).append(".dashboard.DashboardBasePage;\n");
        sb.append("import io.jettra.wui.complex.Center;\n");
        sb.append("import io.jettra.wui.components.Div;\n");
        sb.append("import io.jettra.wui.components.Header;\n");
        sb.append("import io.jettra.wui.components.Paragraph;\n");
        sb.append("import io.jettra.wui.components.Button;\n\n");
        sb.append("public class LoginAdvancedPage extends DashboardBasePage {\n\n");
        sb.append("    public LoginAdvancedPage() {\n");
        sb.append("        super(\"LoginAdvanced Component\");\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    protected void initCenter(Center center, String username) {\n");
        sb.append("        Div container = new Div();\n");
        sb.append("        container.setStyle(\"padding\", \"30px\");\n\n");
        sb.append("        Div headerRow = new Div();\n");
        sb.append("        headerRow.setStyle(\"display\", \"flex\").setStyle(\"justify-content\", \"space-between\").setStyle(\"align-items\", \"center\").setStyle(\"margin-bottom\", \"15px\");\n\n");
        sb.append("        Header h1 = new Header(1, \"LoginAdvanced Component\");\n");
        sb.append("        h1.setStyle(\"margin\", \"0\");\n");
        sb.append("        headerRow.add(h1);\n\n");
        sb.append("        Button codeBtn = new Button(\"Code\");\n");
        sb.append("        codeBtn.addClass(\"j-btn\");\n");
        sb.append("        codeBtn.setStyle(\"border-color\", \"var(--jettra-accent)\").setStyle(\"color\", \"var(--jettra-accent)\");\n");
        sb.append("        codeBtn.setProperty(\"onclick\", \"document.getElementById('code-modal-logina').style.display = 'block'\");\n");
        sb.append("        headerRow.add(codeBtn);\n\n");
        sb.append("        container.add(headerRow);\n\n");
        sb.append("        // --- Code Modal Dialog for Java Code ---\n");
        sb.append("        io.jettra.wui.complex.Modal codeModal = new io.jettra.wui.complex.Modal(\"code-modal-logina\");\n");
        sb.append("        codeModal.setStyle(\"display\", \"none\").setStyle(\"background\", \"var(--jettra-glass)\")\n");
        sb.append("                 .setStyle(\"backdrop-filter\", \"blur(10px)\")\n");
        sb.append("                 .setStyle(\"padding\", \"20px\").setStyle(\"border-radius\", \"8px\")\n");
        sb.append("                 .setStyle(\"width\", \"90%\").setStyle(\"max-width\", \"800px\")\n");
        sb.append("                 .setStyle(\"border\", \"1px solid var(--jettra-border)\");\n\n");
        sb.append("        codeModal.add(new Header(3, \"Java Code Examples\").setStyle(\"margin-top\", \"0\").setStyle(\"color\", \"var(--jettra-accent)\"));\n\n");
        sb.append("        Div codeContainer = new Div();\n");
        sb.append("        codeContainer.setStyle(\"background\", \"rgba(0,0,0,0.4)\").setStyle(\"padding\", \"15px\")\n");
        sb.append("                     .setStyle(\"border-radius\", \"4px\").setStyle(\"overflow-x\", \"auto\")\n");
        sb.append("                     .setStyle(\"margin-bottom\", \"20px\").setStyle(\"border\", \"1px solid rgba(255,255,255,0.1)\");\n\n");
        sb.append("        String javaCode = \"LoginAdvanced login = new LoginAdvanced(\\\"/loginSubmit\\\", \\\"/forgotPassword\\\", \\\"/register\\\", \\\"images/banner.png\\\", \\\"Title\\\");\\\\n\" +\n");
        sb.append("                          \"page.add(login);\";\n\n");
        sb.append("        io.jettra.wui.core.UIComponent pre = new io.jettra.wui.core.UIComponent(\"pre\") {};\n");
        sb.append("        pre.setStyle(\"margin\", \"0\");\n");
        sb.append("        io.jettra.wui.core.UIComponent codeTag = new io.jettra.wui.core.UIComponent(\"code\") {};\n");
        sb.append("        codeTag.setProperty(\"id\", \"java-code-logina\");\n");
        sb.append("        codeTag.setStyle(\"color\", \"#a5d6ff\").setStyle(\"font-family\", \"monospace\").setStyle(\"font-size\", \"0.9rem\");\n");
        sb.append("        codeTag.setContent(javaCode.replace(\"<\", \"&lt;\").replace(\">\", \"&gt;\"));\n\n");
        sb.append("        pre.add(codeTag);\n");
        sb.append("        codeContainer.add(pre);\n");
        sb.append("        codeModal.add(codeContainer);\n\n");
        sb.append("        Div modalActions = new Div();\n");
        sb.append("        modalActions.setStyle(\"display\", \"flex\").setStyle(\"justify-content\", \"flex-end\").setStyle(\"gap\", \"10px\");\n\n");
        sb.append("        Button copyBtn = new Button(\"Copy\");\n");
        sb.append("        copyBtn.addClass(\"j-btn\");\n");
        sb.append("        copyBtn.setProperty(\"onclick\", \"navigator.clipboard.writeText(document.getElementById('java-code-logina').innerText).then(() => { this.innerText='Copied!'; setTimeout(() => this.innerText='Copy', 2000); })\");\n\n");
        sb.append("        Button closeBtn = new Button(\"Close\");\n");
        sb.append("        closeBtn.addClass(\"j-btn\");\n");
        sb.append("        closeBtn.setStyle(\"background\", \"transparent\").setStyle(\"border-color\", \"var(--jettra-border)\");\n");
        sb.append("        closeBtn.setProperty(\"onclick\", \"document.getElementById('code-modal-logina').style.display = 'none'\");\n\n");
        sb.append("        modalActions.add(closeBtn).add(copyBtn);\n");
        sb.append("        codeModal.add(modalActions);\n\n");
        sb.append("        container.add(codeModal);\n\n");
        sb.append("        // --- Actual Component Demo ---\n");
        sb.append("        container.add(new Header(2, \"LoginAdvanced Example\").setStyle(\"margin-top\", \"30px\"));\n");
        sb.append("        container.add(new Paragraph(\"A comprehensive login form with a side banner and links. We will not render it inline as it's a full-page layout component.\").setStyle(\"margin-bottom\", \"20px\"));\n\n");
        sb.append("        center.add(container);\n");
        sb.append("    }\n");
        sb.append("}\n");
        Files.write(new File(pkgDir, "LoginAdvancedPage.java").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateReadme(File projectDir, String artifactId) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(artifactId).append("\n\n");
        sb.append("This project was generated using Jettra Starter. It is configured to run on Java 25 with the Jettra WUI and Report engines.\n\n");
        sb.append("## Compilation & Execution\n\n");
        sb.append("To compile the project and build the standalone Fat JAR, run:\n");
        sb.append("```bash\n");
        sb.append("mvn clean package\n");
        sb.append("```\n\n");
        sb.append("To run the project locally:\n");
        sb.append("```bash\n");
        sb.append("java -jar target/").append(artifactId).append("-1.0-SNAPSHOT.jar\n");
        sb.append("```\n\n");
        sb.append("## Docker & Optimizations\n\n");
        sb.append("This project comes with a pre-configured `Dockerfile` optimized for minimal footprint and maximum startup performance using BellSoft Liberica JRE and Alpaquita Linux.\n\n");
        sb.append("To build the Docker image, run:\n");
        sb.append("```bash\n");
        sb.append("docker build -t ").append(artifactId.toLowerCase()).append(" .\n");
        sb.append("```\n\n");
        sb.append("To run the container:\n");
        sb.append("```bash\n");
        sb.append("docker run -p 8080:8080 ").append(artifactId.toLowerCase()).append("\n");
        sb.append("```\n\n");
        sb.append("### Advanced JVM Optimizations (AOT Cache & Java Object Compact)\n\n");
        sb.append("The Dockerfile is uniquely designed to train the JVM by running a short simulated startup during the build process (`timeout 10s java -XX:ArchiveClassesAtExit=app.jsa ...`).\n");
        sb.append("This produces a highly compressed **AppCDS (Application Class-Data Sharing) Archive** (`app.jsa`), also leveraging Java Object Compact techniques under Java 25. When the container starts normally, it uses `-XX:SharedArchiveFile=app.jsa` to load classes instantly from the cache, drastically reducing memory overhead and startup times.\n");
        
        Files.write(new File(projectDir, "README.md").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
