package com.jettrapluginstore.commands;

import picocli.CommandLine.Command;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

@Command(name = "prepareplugin", description = "Generates the plugin-descriptor.md file in the current project.")
public class PreparePluginCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Preparing plugin...");
        File pomFile = new File("pom.xml");
        if (!pomFile.exists()) {
            System.err.println("Error: pom.xml not found in the current directory. Are you in a Maven project?");
            return 1;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile);
        doc.getDocumentElement().normalize();

        String artifactId = getTagValue(doc, "artifactId", "unknown");
        String version = getTagValue(doc, "version", "1.0-SNAPSHOT");
        String name = getTagValue(doc, "name", artifactId);
        String description = getTagValue(doc, "description", "Descripción general del plugin");
        String groupId = getTagValue(doc, "groupId", "com.jettrapluginstore");

        // Format DateTime
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTime = formatter.format(new Date());

        String pluginPackage = groupId + "." + artifactId.toLowerCase();

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(name).append("\n");
        sb.append("ArtifactId: ").append(artifactId.toLowerCase()).append("\n");
        sb.append("Plugin-Package: ").append(pluginPackage).append("\n");
        sb.append("Versión: ").append(version).append("\n");
        sb.append("Autor: Autor\n");
        sb.append("Email: email@example.com\n");
        sb.append("WebSite: https://example.com\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Dependencies: jettraServer, JettraReport, JettraWUI\n");
        sb.append("DateTime: ").append(dateTime).append("\n");

        Files.write(Paths.get("plugin-descriptor.md"), sb.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Created plugin-descriptor.md successfully.");

        return 0;
    }

    private String getTagValue(Document doc, String tagName, String defaultValue) {
        NodeList nList = doc.getElementsByTagName(tagName);
        if (nList != null && nList.getLength() > 0) {
            // First level tag is better if multiple exists (like dependencies), but this is a simple approximation
            for (int i = 0; i < nList.getLength(); i++) {
                if (nList.item(i).getParentNode().getNodeName().equals("project")) {
                    return nList.item(i).getTextContent();
                }
            }
        }
        return defaultValue;
    }
}
