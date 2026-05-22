package com.jettrapluginstore.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class PluginStoreApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Jettra Plugin Store - Cybernetic Control Panel");

        // Load the main visual container
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1100, 720);
        
        // Load stylesheet based on saved preference
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MainView.class);
        String theme = prefs.get("theme", "dark");
        String cssName = theme.equals("dark") ? "PluginStoreStyle.css" : "PluginStoreLightStyle.css";
        
        try {
            String cssPath = getClass().getResource("/css/" + cssName).toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Could not load JFX stylesheet: " + e.getMessage());
            // Fallback load from standard path if resource classloader fails
            try {
                scene.getStylesheets().add("css/" + cssName);
            } catch (Exception ex) {
                System.err.println("Fallback CSS loading failed too.");
            }
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void launchApp(String[] args) {
        Application.launch(PluginStoreApp.class, args);
    }
}
