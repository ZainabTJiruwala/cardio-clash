package com.ecg.analyzer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages user preferences and settings for the ECG Analyzer application.
 * Preferences are stored in the user's home directory.
 */
public class UserPreferences {
    
    private static final Logger logger = LoggerFactory.getLogger(UserPreferences.class);
    private static final String APP_DIR_NAME = ".ecg-analyzer";
    private static final String SETTINGS_FILE_NAME = "settings.properties";
    
    private final Path settingsPath;
    private final Properties properties;
    
    public UserPreferences() {
        // Get user home directory
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, APP_DIR_NAME);
        
        // Create app directory if it doesn't exist
        try {
            if (!Files.exists(appDir)) {
                Files.createDirectories(appDir);
                logger.info("Created settings directory: {}", appDir);
            }
        } catch (IOException e) {
            logger.error("Failed to create settings directory", e);
        }
        
        this.settingsPath = appDir.resolve(SETTINGS_FILE_NAME);
        this.properties = new Properties();
        loadSettings();
    }
    
    /**
     * Loads settings from disk.
     */
    private void loadSettings() {
        if (Files.exists(settingsPath)) {
            try (InputStream input = new FileInputStream(settingsPath.toFile())) {
                properties.load(input);
                logger.info("Loaded user preferences from: {}", settingsPath);
            } catch (IOException e) {
                logger.error("Failed to load settings", e);
            }
        } else {
            logger.info("No existing settings file found, using defaults");
        }
    }
    
    /**
     * Saves settings to disk.
     */
    private void saveSettings() {
        try (OutputStream output = new FileOutputStream(settingsPath.toFile())) {
            properties.store(output, "ECG Analyzer User Preferences");
            logger.info("Saved user preferences to: {}", settingsPath);
        } catch (IOException e) {
            logger.error("Failed to save settings", e);
        }
    }
    
    /**
     * Gets a string preference.
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Sets a string preference.
     */
    public void setString(String key, String value) {
        properties.setProperty(key, value);
        saveSettings();
    }
    
    /**
     * Gets a boolean preference.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    /**
     * Sets a boolean preference.
     */
    public void setBoolean(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
        saveSettings();
    }
    
    /**
     * Gets the current theme preference.
     */
    public String getTheme() {
        return getString("theme", "light");
    }
    
    /**
     * Sets the theme preference.
     */
    public void setTheme(String theme) {
        setString("theme", theme);
    }
    
    /**
     * Checks if dark mode is enabled.
     */
    public boolean isDarkMode() {
        return "dark".equals(getTheme());
    }
}
