package com.jettrapluginstore.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitUtils {

    public static final String REPO_URL = "https://github.com/avbravo/JettraAppStore.git";

    public static Git cloneRepository(Path targetDir) throws Exception {
        if (Files.exists(targetDir)) {
            // Delete if exists, or handle differently
            deleteDirectory(targetDir.toFile());
        }
        System.out.println("Cloning repository...");
        return Git.cloneRepository()
                .setURI(REPO_URL)
                .setDirectory(targetDir.toFile())
                .call();
    }

    public static void commitAndPush(Git git, String user, String pat, String message) throws Exception {
        System.out.println("Adding files to index...");
        git.add().addFilepattern(".").call();

        System.out.println("Committing...");
        git.commit().setMessage(message).setAuthor(user, "email@example.com").call();

        System.out.println("Pushing to remote...");
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(user, pat);
        git.push().setCredentialsProvider(credentials).call();
        System.out.println("Push successful.");
    }

    public static void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}
