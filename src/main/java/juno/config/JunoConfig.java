package juno.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Singleton class to manage global configuration for Juno.
 */
public class JunoConfig {

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.dir"), ".juno", "juno_config.json");
    private static final JunoConfig INSTANCE = new JunoConfig();  // Singleton instance

    // Config fields (public for Gson)
    public String idfPath;
    public String idfPyPath;
    public String pythonPath;
    public String pythonExecutablePath;
    public String toolchainPath;
    public String cMakePath;
    public String ninjaPath;
    public String serialPort;

    // Required by Gson
    public JunoConfig() {
    }

    /**
     * Returns the singleton instance.
     */
    public static JunoConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Load values from another config into this one.
     */
    private void loadFrom(JunoConfig other) {
        this.idfPath = other.idfPath;
        this.idfPyPath = other.idfPyPath;
        this.pythonPath = other.pythonPath;
        this.pythonExecutablePath = other.pythonExecutablePath;
        this.toolchainPath = other.toolchainPath;
        this.cMakePath = other.cMakePath;
        this.ninjaPath = other.ninjaPath;
        this.serialPort = other.serialPort;
    }

    /**
     * Loads the configuration from disk, or runs detection if not found.
     * Also syncs values to JunoPaths.
     */
    public static void load() {
        try {

            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                Gson gson = new Gson();
                JunoConfig parsed = gson.fromJson(json, JunoConfig.class);
                INSTANCE.loadFrom(parsed);

                // Push values to JunoPaths
                JunoPaths.idfPath = INSTANCE.idfPath;
                JunoPaths.idfPyPath = INSTANCE.idfPyPath;
                JunoPaths.pythonPath = INSTANCE.pythonPath;
                JunoPaths.pythonExecutablePath = INSTANCE.pythonExecutablePath;
                JunoPaths.toolchainPath = INSTANCE.toolchainPath;
                JunoPaths.cMakePath = INSTANCE.cMakePath;
                JunoPaths.ninjaPath = INSTANCE.ninjaPath;
                JunoPaths.serialPort = INSTANCE.serialPort;

                System.out.println("✅ Loaded config from: " + CONFIG_PATH);
            } else {
                System.out.println("⚠️  Config not found. Running auto-detection...");

                if (!JunoPaths.isInitialized()) JunoPaths.init();  // internally calls JunoDetector

                // Copy detected values to config
                INSTANCE.idfPath = JunoPaths.idfPath;
                INSTANCE.idfPyPath = JunoPaths.idfPyPath;
                INSTANCE.pythonPath = JunoPaths.pythonPath;
                INSTANCE.pythonExecutablePath = JunoPaths.pythonExecutablePath;
                INSTANCE.toolchainPath = JunoPaths.toolchainPath;
                INSTANCE.cMakePath = JunoPaths.cMakePath;
                INSTANCE.ninjaPath = JunoPaths.ninjaPath;
                INSTANCE.serialPort = JunoPaths.serialPort;

                save(); // save detected config for next time
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to load Juno config from " + CONFIG_PATH, e);
        }
    }

    /**
     * Saves the current config to disk in JSON format.
     */
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(INSTANCE);
            Files.writeString(CONFIG_PATH, json);
            System.out.println("✅ Config saved to: " + CONFIG_PATH);
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to save Juno config to " + CONFIG_PATH, e);
        }
    }

    /**
     * Returns the full path to the config file.
     */
    public static Path getConfigPath() {
        return CONFIG_PATH;
    }

    public static String updateSerialPortAndSave() {
        String detectedPort = JunoDetector.detectEsp32Port();
        if (detectedPort != null && !detectedPort.trim().isEmpty()) {
            detectedPort = detectedPort.trim();

            INSTANCE.serialPort = detectedPort;
            JunoPaths.serialPort = detectedPort;  // Keep JunoPaths in sync too

            try {
                save();
                System.out.println("✅ Serial port updated to: " + detectedPort + " and config saved.");
            } catch (RuntimeException e) {
                System.err.println("⚠️ Failed to save config after updating serial port: " + e.getMessage());
            }

        } else {
            System.err.println("❌ No serial port detected! Please connect your ESP32 and try again.");
            System.exit(1);
        }
        return detectedPort;
    }


}
