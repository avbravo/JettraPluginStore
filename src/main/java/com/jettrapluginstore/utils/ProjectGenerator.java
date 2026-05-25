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

        boolean genFront = properties.containsKey("gen.frontend") ? Boolean.parseBoolean(properties.get("gen.frontend")) : true;
        boolean genBack = properties.containsKey("gen.backend") ? Boolean.parseBoolean(properties.get("gen.backend")) : true;

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
        generateMain(basePackageDir, groupId, genFront, genBack);

        if (genFront) {
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

            // pages/admin package
            File adminPagesDir = new File(pagesDir, "admin");
            adminPagesDir.mkdirs();
            generateAdminPages(adminPagesDir, groupId);
        }

        if (genFront || genBack) {
            // entity package
            File entityDir = new File(basePackageDir, "entity");
            entityDir.mkdirs();
            generateEntities(entityDir, groupId);

            // model package
            File modelDir = new File(basePackageDir, "model");
            modelDir.mkdirs();
            generateModels(modelDir, groupId);

            // repository package
            File repositoryDir = new File(basePackageDir, "repository");
            repositoryDir.mkdirs();
            generateRepositories(repositoryDir, groupId);
        }

        if (genBack) {
            // controller package
            File controllerDir = new File(basePackageDir, "controller");
            controllerDir.mkdirs();
            generateControllers(controllerDir, groupId);

            // seguridad package
            File securityDir = new File(basePackageDir, "seguridad");
            securityDir.mkdirs();
            generateSecurity(securityDir, groupId);
        }
        
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

    private static void generateMain(File pkgDir, String groupId, boolean genFront, boolean genBack) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(groupId).append(";\n\n");
        if (genFront) {
            sb.append("import ").append(groupId).append(".pages.LoginPage;\n");
            sb.append("import ").append(groupId).append(".pages.LoginAdvancedPage;\n");
            sb.append("import ").append(groupId).append(".dashboard.DashboardPage;\n");
            sb.append("import ").append(groupId).append(".pages.admin.PermisoPage;\n");
            sb.append("import ").append(groupId).append(".pages.admin.RolPage;\n");
            sb.append("import ").append(groupId).append(".pages.admin.PerfilPage;\n");
            sb.append("import ").append(groupId).append(".pages.admin.UsuarioPage;\n");
        }
        if (genBack) {
            sb.append("import ").append(groupId).append(".controller.AuthController;\n");
            sb.append("import ").append(groupId).append(".controller.UsuarioController;\n");
        }
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
        if (genFront) {
            sb.append("        io.jettra.wui.complex.ErrorPage.path = \"http://localhost:\" + app.port + app.contextpath;\n\n");
        }
        sb.append("        System.out.println(\"Starting JettraServer...\");\n");
        sb.append("        JettraServer server = new JettraServer();\n");
        if (genFront) {
            sb.append("        server.setErrorPage(\"/error\");\n\n");
            sb.append("        server.addHandler(\"/error\", io.jettra.wui.complex.ErrorPage.class);\n");
            sb.append("        server.addHandler(\"/\", LoginPage.class);\n");
            sb.append("        server.addHandler(\"/login\", LoginPage.class);\n");
            sb.append("        server.addHandler(\"/logout\", LoginPage.class);\n");
            sb.append("        server.addHandler(\"/loginadvanced\", LoginAdvancedPage.class);\n");
            sb.append("        server.addHandler(\"/dashboard\", DashboardPage.class);\n\n");
            sb.append("        server.addHandler(\"/permiso\", PermisoPage.class);\n");
            sb.append("        server.addHandler(\"/rol\", RolPage.class);\n");
            sb.append("        server.addHandler(\"/perfil\", PerfilPage.class);\n");
            sb.append("        server.addHandler(\"/usuario\", UsuarioPage.class);\n\n");
        } else {
            sb.append("        server.addHandler(\"/\", exchange -> {\n");
            sb.append("            String response = \"{\\\"message\\\":\\\"Welcome to Jettra Backend REST API\\\",\\\"status\\\":\\\"online\\\"}\";\n");
            sb.append("            exchange.getResponseHeaders().add(\"Content-Type\", \"application/json\");\n");
            sb.append("            exchange.sendResponseHeaders(200, response.length());\n");
            sb.append("            try (java.io.OutputStream os = exchange.getResponseBody()) {\n");
            sb.append("                os.write(response.getBytes());\n");
            sb.append("            }\n");
            sb.append("        });\n\n");
        }
        if (genBack) {
            sb.append("        server.addHandler(\"/api/auth\", AuthController.class);\n");
            sb.append("        server.addHandler(\"/api/usuarios\", UsuarioController.class);\n\n");
        }
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
        sb.append("        addCategory(\"Administration\", new String[]{}, \"\");\n");
        sb.append("        appendMenuItem(\"Permisos\", \"/permiso\", \"<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='#0ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'></path></svg>\");\n");
        sb.append("        appendMenuItem(\"Roles\", \"/rol\", \"<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='#0ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'></path><circle cx='9' cy='7' r='4'></circle></svg>\");\n");
        sb.append("        appendMenuItem(\"Perfiles\", \"/perfil\", \"<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='#0ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='11' width='18' height='11' rx='2' ry='2'></rect><path d='M7 11V7a5 5 0 0 1 10 0v4'></path></svg>\");\n");
        sb.append("        appendMenuItem(\"Usuarios\", \"/usuario\", \"<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='#0ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'></path><circle cx='9' cy='7' r='4'></circle><path d='M23 21v-2a4 4 0 0 0-3-3.87'></path><path d='M16 3.13a4 4 0 0 1 0 7.75'></path></svg>\");\n\n");
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

    private static void generateEntities(File pkgDir, String groupId) throws IOException {
        String base = "package " + groupId + ".entity;\n\n";
        Files.write(new File(pkgDir, "Permiso.java").toPath(), (base + "public record Permiso(String nombre, String endpoint, String destinoJava, String accion) {\n    public Permiso {\n        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException(\"Nombre requerido\");\n        if (endpoint == null || endpoint.isBlank()) throw new IllegalArgumentException(\"Endpoint requerido\");\n        if (destinoJava == null || destinoJava.isBlank()) throw new IllegalArgumentException(\"Destino Java requerido\");\n        if (!endpoint.startsWith(\"/\")) {\n            endpoint = \"/\" + endpoint;\n        }\n    }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "Rol.java").toPath(), (base + "import java.util.Set;\n\npublic record Rol(String nombre, Set<Permiso> permisos) {\n    public Rol {\n        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException(\"Nombre de rol requerido\");\n        permisos = Set.copyOf(permisos);\n    }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "Perfil.java").toPath(), (base + "public record Perfil(String nombreCompleto, String avatarUrl, String zonaHoraria) {\n    public Perfil(String nombreCompleto) {\n        this(nombreCompleto, null, \"UTC\");\n    }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "Usuario.java").toPath(), (base + "import java.time.Instant;\nimport java.util.Set;\nimport java.util.UUID;\n\npublic record Usuario(UUID id, String username, String nombre, String password, String email, boolean activo, Perfil perfil, Set<Rol> roles, Instant fechaCreacion) {\n    public Usuario {\n        if (id == null) id = UUID.randomUUID();\n        if (username == null || username.isBlank()) throw new IllegalArgumentException(\"Username requerido\");\n        if (email == null || email.isBlank()) throw new IllegalArgumentException(\"Email requerido\");\n        if (fechaCreacion == null) fechaCreacion = Instant.now();\n        roles = Set.copyOf(roles);\n    }\n    public boolean tieneAccesoAEndpoint(String ruta) {\n        return roles.stream().flatMap(rol -> rol.permisos().stream()).anyMatch(permiso -> permiso.endpoint().equalsIgnoreCase(ruta));\n    }\n    public boolean tieneAccesoAComponente(String claseDestino) {\n        return roles.stream().flatMap(rol -> rol.permisos().stream()).anyMatch(permiso -> permiso.destinoJava().equalsIgnoreCase(claseDestino));\n    }\n}\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void generateModels(File pkgDir, String groupId) throws IOException {
        String base = "package " + groupId + ".model;\n\nimport io.jettra.wui.core.annotations.JettraViewModel;\nimport io.jettra.wui.core.annotations.PropertiesLabel;\nimport io.jettra.wui.validations.NotNull;\n\n";
        Files.write(new File(pkgDir, "PermisoModel.java").toPath(), (base + "@JettraViewModel\npublic class PermisoModel {\n    @NotNull @PropertiesLabel(value = \"permiso.nombre\", label = \"Nombre\") private String nombre;\n    @NotNull @PropertiesLabel(value = \"permiso.endpoint\", label = \"Endpoint\") private String endpoint;\n    @NotNull @PropertiesLabel(value = \"permiso.destino\", label = \"Destino Java\") private String destinoJava;\n    @NotNull @PropertiesLabel(value = \"permiso.accion\", label = \"Acción\") private String accion;\n    public PermisoModel() {}\n    public PermisoModel(String nombre, String endpoint, String destinoJava, String accion) { this.nombre = nombre; this.endpoint = endpoint; this.destinoJava = destinoJava; this.accion = accion; }\n    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; } public String getEndpoint() { return endpoint; } public void setEndpoint(String endpoint) { this.endpoint = endpoint; } public String getDestinoJava() { return destinoJava; } public void setDestinoJava(String destinoJava) { this.destinoJava = destinoJava; } public String getAccion() { return accion; } public void setAccion(String accion) { this.accion = accion; }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "RolModel.java").toPath(), (base + "@JettraViewModel\npublic class RolModel {\n    @NotNull @PropertiesLabel(value = \"rol.nombre\", label = \"Nombre del Rol\") private String nombre;\n    @PropertiesLabel(value = \"rol.permisos\", label = \"Permisos (separados por coma)\") private String permisos;\n    public RolModel() {}\n    public RolModel(String nombre, String permisos) { this.nombre = nombre; this.permisos = permisos; }\n    public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; } public String getPermisos() { return permisos; } public void setPermisos(String permisos) { this.permisos = permisos; }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "PerfilModel.java").toPath(), (base + "@JettraViewModel\npublic class PerfilModel {\n    @NotNull @PropertiesLabel(value = \"perfil.nombreCompleto\", label = \"Nombre Completo\") private String nombreCompleto;\n    @PropertiesLabel(value = \"perfil.avatarUrl\", label = \"URL del Avatar\") private String avatarUrl;\n    @PropertiesLabel(value = \"perfil.zonaHoraria\", label = \"Zona Horaria\") private String zonaHoraria;\n    public PerfilModel() {}\n    public PerfilModel(String nombreCompleto, String avatarUrl, String zonaHoraria) { this.nombreCompleto = nombreCompleto; this.avatarUrl = avatarUrl; this.zonaHoraria = zonaHoraria; }\n    public String getNombreCompleto() { return nombreCompleto; } public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; } public String getAvatarUrl() { return avatarUrl; } public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; } public String getZonaHoraria() { return zonaHoraria; } public void setZonaHoraria(String zonaHoraria) { this.zonaHoraria = zonaHoraria; }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "UsuarioModel.java").toPath(), (base + "@JettraViewModel\npublic class UsuarioModel {\n    @NotNull @PropertiesLabel(value = \"usuario.id\", label = \"ID\") private String id;\n    @NotNull @PropertiesLabel(value = \"usuario.username\", label = \"Username\") private String username;\n    @NotNull @PropertiesLabel(value = \"usuario.nombre\", label = \"Nombre\") private String nombre;\n    @NotNull @PropertiesLabel(value = \"usuario.email\", label = \"Email\") private String email;\n    @PropertiesLabel(value = \"usuario.activo\", label = \"Activo\") private Boolean activo;\n    @PropertiesLabel(value = \"usuario.perfil\", label = \"Perfil\") private String perfil;\n    @PropertiesLabel(value = \"usuario.roles\", label = \"Roles\") private String roles;\n    public UsuarioModel() {}\n    public UsuarioModel(String id, String username, String nombre, String email, Boolean activo, String perfil, String roles) { this.id = id; this.username = username; this.nombre = nombre; this.email = email; this.activo = activo; this.perfil = perfil; this.roles = roles; }\n    public String getId() { return id; } public void setId(String id) { this.id = id; } public String getUsername() { return username; } public void setUsername(String username) { this.username = username; } public String getNombre() { return nombre; } public void setNombre(String nombre) { this.nombre = nombre; } public String getEmail() { return email; } public void setEmail(String email) { this.email = email; } public Boolean getActivo() { return activo; } public void setActivo(Boolean activo) { this.activo = activo; } public String getPerfil() { return perfil; } public void setPerfil(String perfil) { this.perfil = perfil; } public String getRoles() { return roles; } public void setRoles(String roles) { this.roles = roles; }\n}\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void generateRepositories(File pkgDir, String groupId) throws IOException {
        String base = "package " + groupId + ".repository;\n\nimport java.util.ArrayList;\nimport java.util.List;\nimport java.util.Optional;\nimport java.util.UUID;\n\n";
        Files.write(new File(pkgDir, "PermisoRepository.java").toPath(), (base + "import " + groupId + ".model.PermisoModel;\n\npublic class PermisoRepository {\n    private static final List<PermisoModel> list = new ArrayList<>();\n    static {\n        list.add(new PermisoModel(\"LEER_USUARIOS\", \"/usuario\", \"UsuarioPage.class\", \"LEER\"));\n        list.add(new PermisoModel(\"ESCRIBIR_USUARIOS\", \"/usuario\", \"UsuarioPage.class\", \"ESCRIBIR\"));\n        list.add(new PermisoModel(\"LEER_ROLES\", \"/rol\", \"RolPage.class\", \"LEER\"));\n        list.add(new PermisoModel(\"ESCRIBIR_ROLES\", \"/rol\", \"RolPage.class\", \"ESCRIBIR\"));\n    }\n    public static List<PermisoModel> findAll() { return list; }\n    public static Optional<PermisoModel> findById(String nombre) { return list.stream().filter(x -> x.getNombre() != null && x.getNombre().equals(nombre)).findFirst(); }\n    public static void save(PermisoModel model) {\n        if (model.getNombre() != null) { delete(model.getNombre()); list.add(model); }\n    }\n    public static void delete(String nombre) { list.removeIf(x -> x.getNombre() != null && x.getNombre().equals(nombre)); }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "RolRepository.java").toPath(), (base + "import " + groupId + ".model.RolModel;\n\npublic class RolRepository {\n    private static final List<RolModel> list = new ArrayList<>();\n    static {\n        list.add(new RolModel(\"ADMIN\", \"LEER_USUARIOS,ESCRIBIR_USUARIOS,LEER_ROLES,ESCRIBIR_ROLES\"));\n        list.add(new RolModel(\"USER\", \"LEER_USUARIOS\"));\n    }\n    public static List<RolModel> findAll() { return list; }\n    public static Optional<RolModel> findById(String nombre) { return list.stream().filter(x -> x.getNombre() != null && x.getNombre().equals(nombre)).findFirst(); }\n    public static void save(RolModel model) {\n        if (model.getNombre() != null) { delete(model.getNombre()); list.add(model); }\n    }\n    public static void delete(String nombre) { list.removeIf(x -> x.getNombre() != null && x.getNombre().equals(nombre)); }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "PerfilRepository.java").toPath(), (base + "import " + groupId + ".model.PerfilModel;\n\npublic class PerfilRepository {\n    private static final List<PerfilModel> list = new ArrayList<>();\n    static {\n        list.add(new PerfilModel(\"Aristides Villarreal\", \"/images/avatar1.png\", \"America/Panama\"));\n        list.add(new PerfilModel(\"Demo User\", \"/images/avatar2.png\", \"UTC\"));\n    }\n    public static List<PerfilModel> findAll() { return list; }\n    public static Optional<PerfilModel> findById(String nombreCompleto) { return list.stream().filter(x -> x.getNombreCompleto() != null && x.getNombreCompleto().equals(nombreCompleto)).findFirst(); }\n    public static void save(PerfilModel model) {\n        if (model.getNombreCompleto() != null) { delete(model.getNombreCompleto()); list.add(model); }\n    }\n    public static void delete(String nombreCompleto) { list.removeIf(x -> x.getNombreCompleto() != null && x.getNombreCompleto().equals(nombreCompleto)); }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "UsuarioRepository.java").toPath(), (base + "import " + groupId + ".model.UsuarioModel;\n\npublic class UsuarioRepository {\n    private static final List<UsuarioModel> list = new ArrayList<>();\n    static {\n        list.add(new UsuarioModel(UUID.randomUUID().toString(), \"admin\", \"Administrador del Sistema\", \"admin@jettra.com\", true, \"Aristides Villarreal\", \"ADMIN\"));\n        list.add(new UsuarioModel(UUID.randomUUID().toString(), \"demo\", \"Usuario de Demostración\", \"demo@jettra.com\", true, \"Demo User\", \"USER\"));\n    }\n    public static List<UsuarioModel> findAll() { return list; }\n    public static Optional<UsuarioModel> findById(String id) { return list.stream().filter(x -> x.getId() != null && x.getId().equals(id)).findFirst(); }\n    public static void save(UsuarioModel model) {\n        if (model.getId() == null || model.getId().isEmpty()) { model.setId(UUID.randomUUID().toString()); } else { delete(model.getId()); }\n        list.add(model);\n    }\n    public static void delete(String id) { list.removeIf(x -> x.getId() != null && x.getId().equals(id)); }\n}\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void generateAdminPages(File pkgDir, String groupId) throws IOException {
        String base = "package " + groupId + ".pages.admin;\n\nimport " + groupId + ".dashboard.DashboardBasePage;\nimport io.jettra.wui.core.annotations.CrudView;\nimport io.jettra.wui.core.annotations.InjectProperties;\nimport io.jettra.wui.sync.JettraPageSincronized;\nimport io.jettra.wui.sync.SyncType;\nimport java.util.Properties;\nimport io.jettra.wui.complex.Center;\n\n";
        Files.write(new File(pkgDir, "PermisoPage.java").toPath(), (base + "@JettraPageSincronized(SyncType.ALL)\n@CrudView(model = " + groupId + ".model.PermisoModel.class, repository = " + groupId + ".repository.PermisoRepository.class, report = true, reportOrientation = \"LANDSCAPE\", reportTitle = \"REPORTE DE PERMISOS\", reportHeaderColor = \"#007BFF\")\npublic class PermisoPage extends DashboardBasePage {\n    @InjectProperties(name = \"messages\") private Properties msg;\n    public PermisoPage() { super(\"Mantenimiento de Permisos\"); }\n    @Override protected void initCenter(Center center, String username) { }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "RolPage.java").toPath(), (base + "@JettraPageSincronized(SyncType.ALL)\n@CrudView(model = " + groupId + ".model.RolModel.class, repository = " + groupId + ".repository.RolRepository.class, report = true, reportOrientation = \"LANDSCAPE\", reportTitle = \"REPORTE DE ROLES\", reportHeaderColor = \"#007BFF\")\npublic class RolPage extends DashboardBasePage {\n    @InjectProperties(name = \"messages\") private Properties msg;\n    public RolPage() { super(\"Mantenimiento de Roles\"); }\n    @Override protected void initCenter(Center center, String username) { }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "PerfilPage.java").toPath(), (base + "@JettraPageSincronized(SyncType.ALL)\n@CrudView(model = " + groupId + ".model.PerfilModel.class, repository = " + groupId + ".repository.PerfilRepository.class, report = true, reportOrientation = \"LANDSCAPE\", reportTitle = \"REPORTE DE PERFILES\", reportHeaderColor = \"#007BFF\")\npublic class PerfilPage extends DashboardBasePage {\n    @InjectProperties(name = \"messages\") private Properties msg;\n    public PerfilPage() { super(\"Mantenimiento de Perfiles\"); }\n    @Override protected void initCenter(Center center, String username) { }\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(pkgDir, "UsuarioPage.java").toPath(), (base + "@JettraPageSincronized(SyncType.ALL)\n@CrudView(model = " + groupId + ".model.UsuarioModel.class, repository = " + groupId + ".repository.UsuarioRepository.class, report = true, reportOrientation = \"LANDSCAPE\", reportTitle = \"REPORTE DE USUARIOS\", reportHeaderColor = \"#007BFF\")\npublic class UsuarioPage extends DashboardBasePage {\n    @InjectProperties(name = \"messages\") private Properties msg;\n    public UsuarioPage() { super(\"Mantenimiento de Usuarios\"); }\n    @Override protected void initCenter(Center center, String username) { }\n}\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void generateControllers(File pkgDir, String groupId) throws IOException {
        // UsuarioController.java
        StringBuilder uc = new StringBuilder();
        uc.append("package ").append(groupId).append(".controller;\n\n");
        uc.append("import com.sun.net.httpserver.HttpExchange;\n");
        uc.append("import com.sun.net.httpserver.HttpHandler;\n");
        uc.append("import java.io.IOException;\n");
        uc.append("import java.io.OutputStream;\n");
        uc.append("import java.nio.charset.StandardCharsets;\n");
        uc.append("import java.util.List;\n");
        uc.append("import ").append(groupId).append(".repository.UsuarioRepository;\n");
        uc.append("import ").append(groupId).append(".model.UsuarioModel;\n");
        uc.append("import ").append(groupId).append(".seguridad.SecurityManager;\n\n");
        uc.append("public class UsuarioController implements HttpHandler {\n");
        uc.append("    @Override\n");
        uc.append("    public void handle(HttpExchange exchange) throws IOException {\n");
        uc.append("        // Verify JWT Security\n");
        uc.append("        if (!SecurityManager.validateRequest(exchange)) {\n");
        uc.append("            String err = \"{\\\"error\\\":\\\"Unauthorized - Invalid or missing JWT token\\\"}\";\n");
        uc.append("            exchange.getResponseHeaders().add(\"Content-Type\", \"application/json\");\n");
        uc.append("            exchange.sendResponseHeaders(401, err.getBytes(StandardCharsets.UTF_8).length);\n");
        uc.append("            try (OutputStream os = exchange.getResponseBody()) {\n");
        uc.append("                os.write(err.getBytes(StandardCharsets.UTF_8));\n");
        uc.append("            }\n");
        uc.append("            return;\n");
        uc.append("        }\n\n");
        uc.append("        String method = exchange.getRequestMethod();\n");
        uc.append("        exchange.getResponseHeaders().add(\"Content-Type\", \"application/json\");\n\n");
        uc.append("        if (\"GET\".equalsIgnoreCase(method)) {\n");
        uc.append("            List<UsuarioModel> usuarios = UsuarioRepository.findAll();\n");
        uc.append("            StringBuilder sb = new StringBuilder(\"[\");\n");
        uc.append("            for (int i = 0; i < usuarios.size(); i++) {\n");
        uc.append("                UsuarioModel u = usuarios.get(i);\n");
        uc.append("                sb.append(String.format(\"{\\\"id\\\":\\\"%s\\\",\\\"username\\\":\\\"%s\\\",\\\"nombre\\\":\\\"%s\\\",\\\"email\\\":\\\"%s\\\",\\\"activo\\\":%b}\",\n");
        uc.append("                        u.getId(), u.getUsername(), u.getNombre(), u.getEmail(), u.getActivo()));\n");
        uc.append("                if (i < usuarios.size() - 1) sb.append(\",\");\n");
        uc.append("            }\n");
        uc.append("            sb.append(\"]\");\n");
        uc.append("            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);\n");
        uc.append("            exchange.sendResponseHeaders(200, bytes.length);\n");
        uc.append("            try (OutputStream os = exchange.getResponseBody()) {\n");
        uc.append("                os.write(bytes);\n");
        uc.append("            }\n");
        uc.append("        } else {\n");
        uc.append("            exchange.sendResponseHeaders(405, -1);\n");
        uc.append("        }\n");
        uc.append("    }\n");
        uc.append("}\n");
        Files.write(new File(pkgDir, "UsuarioController.java").toPath(), uc.toString().getBytes(StandardCharsets.UTF_8));

        // AuthController.java
        StringBuilder ac = new StringBuilder();
        ac.append("package ").append(groupId).append(".controller;\n\n");
        ac.append("import com.sun.net.httpserver.HttpExchange;\n");
        ac.append("import com.sun.net.httpserver.HttpHandler;\n");
        ac.append("import java.io.IOException;\n");
        ac.append("import java.io.InputStream;\n");
        ac.append("import java.io.OutputStream;\n");
        ac.append("import java.nio.charset.StandardCharsets;\n");
        ac.append("import ").append(groupId).append(".seguridad.SecurityManager;\n\n");
        ac.append("public class AuthController implements HttpHandler {\n");
        ac.append("    @Override\n");
        ac.append("    public void handle(HttpExchange exchange) throws IOException {\n");
        ac.append("        if (!\"POST\".equalsIgnoreCase(exchange.getRequestMethod())) {\n");
        ac.append("            exchange.sendResponseHeaders(405, -1);\n");
        ac.append("            return;\n");
        ac.append("        }\n\n");
        ac.append("        StringBuilder sb = new StringBuilder();\n");
        ac.append("        try (InputStream is = exchange.getRequestBody()) {\n");
        ac.append("            int i;\n");
        ac.append("            while ((i = is.read()) != -1) {\n");
        ac.append("                sb.append((char) i);\n");
        ac.append("            }\n");
        ac.append("        }\n");
        ac.append("        String body = sb.toString();\n");
        ac.append("        String username = extractJsonField(body, \"username\");\n");
        ac.append("        String password = extractJsonField(body, \"password\");\n\n");
        ac.append("        exchange.getResponseHeaders().add(\"Content-Type\", \"application/json\");\n\n");
        ac.append("        if ((\"admin\".equals(username) && \"admin\".equals(password)) ||\n");
        ac.append("            (\"demo\".equals(username) && \"demo\".equals(password))) {\n");
        ac.append("            String token = SecurityManager.generateToken(username);\n");
        ac.append("            String response = String.format(\"{\\\"token\\\":\\\"%s\\\",\\\"status\\\":\\\"success\\\"}\", token);\n");
        ac.append("            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);\n");
        ac.append("            exchange.sendResponseHeaders(200, bytes.length);\n");
        ac.append("            try (OutputStream os = exchange.getResponseBody()) {\n");
        ac.append("                os.write(bytes);\n");
        ac.append("            }\n");
        ac.append("        } else {\n");
        ac.append("            String response = \"{\\\"error\\\":\\\"Invalid credentials\\\",\\\"status\\\":\\\"failure\\\"}\";\n");
        ac.append("            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);\n");
        ac.append("            exchange.sendResponseHeaders(401, bytes.length);\n");
        ac.append("            try (OutputStream os = exchange.getResponseBody()) {\n");
        ac.append("                os.write(bytes);\n");
        ac.append("            }\n");
        ac.append("        }\n");
        ac.append("    }\n\n");
        ac.append("    private String extractJsonField(String json, String field) {\n");
        ac.append("        String search = \"\\\"\" + field + \"\\\":\\\"\";\n");
        ac.append("        int start = json.indexOf(search);\n");
        ac.append("        if (start == -1) return \"\";\n");
        ac.append("        start += search.length();\n");
        ac.append("        int end = json.indexOf(\"\\\"\", start);\n");
        ac.append("        if (end == -1) return \"\";\n");
        ac.append("        return json.substring(start, end);\n");
        ac.append("    }\n");
        ac.append("}\n");
        Files.write(new File(pkgDir, "AuthController.java").toPath(), ac.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void generateSecurity(File pkgDir, String groupId) throws IOException {
        StringBuilder sc = new StringBuilder();
        sc.append("package ").append(groupId).append(".seguridad;\n\n");
        sc.append("import com.jettra.jwt.JettraJWT;\n");
        sc.append("import com.sun.net.httpserver.HttpExchange;\n\n");
        sc.append("public class SecurityManager {\n");
        sc.append("    private static final JettraJWT jwt = new JettraJWT(\"my-super-secret-key-1234567890-jettra-token-secret\", 3600000);\n\n");
        sc.append("    public static String generateToken(String username) {\n");
        sc.append("        return jwt.generateToken(username);\n");
        sc.append("    }\n\n");
        sc.append("    public static boolean validateRequest(HttpExchange exchange) {\n");
        sc.append("        String authHeader = exchange.getRequestHeaders().getFirst(\"Authorization\");\n");
        sc.append("        if (authHeader != null && authHeader.startsWith(\"Bearer \")) {\n");
        sc.append("            String token = authHeader.substring(7);\n");
        sc.append("            try {\n");
        sc.append("                String username = jwt.extractUsername(token);\n");
        sc.append("                return jwt.isTokenValid(token, username);\n");
        sc.append("            } catch (Exception e) {\n");
        sc.append("                return false;\n");
        sc.append("            }\n");
        sc.append("        }\n");
        sc.append("        return false;\n");
        sc.append("    }\n");
        sc.append("}\n");
        Files.write(new File(pkgDir, "SecurityManager.java").toPath(), sc.toString().getBytes(StandardCharsets.UTF_8));
    }
}
