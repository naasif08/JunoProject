package juno.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class JunoPaths {

    private static boolean initialized = false;

    public static String idfPath;
    public static String pythonPath;
    public static String pythonExecutablePath;
    public static String toolchainPath;
    public static String serialPort;
    public static String gitPath;
    public static String xtensaGdbPath;
    public static String xtensaToolchainPath;
    public static String espClangPath;
    public static String cMakePath;
    public static String openOcdBin;
    public static String ninjaPath;
    public static String idfPyPath;
    public static String cCacheBinPath;
    public static String dfuUtilBinPath;
    public static String openOcdScriptsPath;

    public static void init() {
        if (initialized) return;
        idfPath = JunoDetector.detectIdfPath();
        pythonPath = JunoDetector.detectPythonPath();
        pythonExecutablePath = JunoDetector.detectPythonExecutable();
        toolchainPath = JunoDetector.detectToolchainBin();
        serialPort = JunoDetector.detectEsp32Port();
        gitPath = JunoDetector.detectEspressifGitPath();
        xtensaGdbPath = JunoDetector.detectXtensaGdbPath();
        xtensaToolchainPath = JunoDetector.detectXtensaToolchainPath();
        espClangPath = JunoDetector.detectEspClangPath();
        cMakePath = JunoDetector.detectCmakePath();
        openOcdBin = JunoDetector.detectOpenOcdBin();
        ninjaPath = JunoDetector.detectNinjaPath();
        idfPyPath = JunoDetector.detectIdfPyPath();
        cCacheBinPath = JunoDetector.detectCcacheBin();
        dfuUtilBinPath = JunoDetector.detectDfuUtilBin();
        openOcdScriptsPath = JunoDetector.detectOpenOcdScriptsPath();
        ensureJunoPropertiesTemplate();
        loadPropertiesOverrides();
        validatePaths();
        initialized = true;
    }

    private static void loadPropertiesOverrides() {
        Path propPath = Paths.get(System.getProperty("user.dir"), ".juno", "juno.properties");
        if (!Files.exists(propPath)) return;

        try (InputStream in = Files.newInputStream(propPath)) {
            Properties props = new Properties();
            props.load(in);

            System.out.println("✅ Loaded manual overrides from juno.properties");

            if (idfPath == null || idfPath.equals("null")) idfPath = props.getProperty("juno.idfPath", idfPath);
            if (pythonPath == null || pythonPath.equals("null"))
                pythonPath = props.getProperty("juno.pythonPath", pythonPath);
            if (pythonExecutablePath == null || pythonExecutablePath.equals("null"))
                pythonExecutablePath = props.getProperty("juno.pythonExecutablePath", pythonExecutablePath);
            if (toolchainPath == null || toolchainPath.equals("null"))
                toolchainPath = props.getProperty("juno.toolchainPath", toolchainPath);
            if (serialPort == null || serialPort.equals("null"))
                serialPort = props.getProperty("juno.serialPort", serialPort);
            if (gitPath == null || gitPath.equals("null")) gitPath = props.getProperty("juno.gitPath", gitPath);
            if (xtensaGdbPath == null || xtensaGdbPath.equals("null"))
                xtensaGdbPath = props.getProperty("juno.xtensaGdbPath", xtensaGdbPath);
            if (xtensaToolchainPath == null || xtensaToolchainPath.equals("null"))
                xtensaToolchainPath = props.getProperty("juno.xtensaToolchainPath", xtensaToolchainPath);
            if (espClangPath == null || espClangPath.equals("null"))
                espClangPath = props.getProperty("juno.espClangPath", espClangPath);
            if (cMakePath == null || cMakePath.equals("null"))
                cMakePath = props.getProperty("juno.cMakePath", cMakePath);
            if (openOcdBin == null || openOcdBin.equals("null"))
                openOcdBin = props.getProperty("juno.openOcdBin", openOcdBin);
            if (ninjaPath == null || ninjaPath.equals("null"))
                ninjaPath = props.getProperty("juno.ninjaPath", ninjaPath);
            if (idfPyPath == null || idfPyPath.equals("null"))
                idfPyPath = props.getProperty("juno.idfPyPath", idfPyPath);
            if (cCacheBinPath == null || cCacheBinPath.equals("null"))
                cCacheBinPath = props.getProperty("juno.cCacheBinPath", cCacheBinPath);
            if (dfuUtilBinPath == null || dfuUtilBinPath.equals("null"))
                dfuUtilBinPath = props.getProperty("juno.dfuUtilBinPath", dfuUtilBinPath);
            if (openOcdScriptsPath == null || openOcdScriptsPath.equals("null"))
                openOcdScriptsPath = props.getProperty("juno.openOcdScriptsPath", openOcdScriptsPath);

        } catch (IOException e) {
            System.err.println("⚠️ Failed to read juno.properties: " + e.getMessage());
        }
    }

    private static void validatePaths() {
        check("idfPath", idfPath);
        check("idfPyPath", idfPyPath);
        check("pythonPath", pythonPath);
        check("pythonExecutablePath", pythonExecutablePath);
        check("toolchainPath", toolchainPath);
        check("cMakePath", cMakePath);
        check("ninjaPath", ninjaPath);
        check("serialPort", serialPort);
    }

    private static void check(String name, String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            System.err.println("\uD83D\uDD27 Configuration Error:");
            System.err.println("\u001B[31m❌ Missing required path: juno." + name + "\u001B[0m");
            System.err.println("Please fix this issue by one of the following options:\n");
            System.err.println("  1. Open the generated file at:");
            System.err.println("     .juno/juno.properties\n");
            System.err.println("     → Set the value for: juno." + name);
            System.err.println("     → Example for Windows:");
            System.err.println("        juno.idfPath=C:/Espressif/frameworks/esp-idf-v5.3.1\n");
            System.err.println("  2. Or, ensure your ESP-IDF environment is properly set in your system.\n");
            System.err.println("Tip: This file is auto-generated and can be edited manually anytime.");

            System.exit(1);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static File getProjectDir(String projectName) {
        Path projectDir = Paths.get(System.getProperty("user.dir"), ".juno", projectName);
        if (!Files.exists(projectDir)) {
            try {
                Files.createDirectories(projectDir);
            } catch (Exception e) {
                throw new RuntimeException("❌ Failed to create/access project directory: " + projectDir, e);
            }
        }
        return projectDir.toFile();
    }


    private static void ensureJunoPropertiesTemplate() {
        Path propPath = Paths.get(System.getProperty("user.dir"), ".juno", "juno.properties");
        if (Files.exists(propPath)) return;

        try {
            Files.createDirectories(propPath.getParent());
            String template = """
                    # Juno Properties Template
                    # This file is auto-generated by Juno. You can edit it to override paths.
                    # If JunoDetector doesn't find mandatory paths, you can edit this file manually.
                    
                    # Note: From 'juno.idfPath' to 'juno.serialPort' these are mandatory paths.
                    # You will only be asked to fill them out manually if JunoDetector fails to find them automatically.
                    
                    # --- Examples ---
                    # Windows:
                    # juno.idfPath=C:/Espressif/frameworks/esp-idf-v5.3.1
                    # juno.toolchainPath=C:/Espressif/tools/xtensa-esp-elf/esp-13.2.0_20240530/xtensa-esp-elf/bin
                    # juno.pythonPath=C:/Espressif/python_env/idf5.3_py3.11_env/Scripts
                    # juno.pythonExecutablePath=C:/Espressif/python_env/idf5.3_py3.11_env/Scripts/python.exe
                    
                    # Linux:
                    # juno.idfPath=/home/username/esp/esp-idf
                    # juno.pythonExecutablePath=/home/username/.espressif/python_env/idf5.x_py3.x_env/bin/python
                    
                    # macOS:
                    # juno.idfPath=/Users/username/esp/esp-idf
                    # juno.pythonExecutablePath=/Users/username/.espressif/python_env/idf5.x_py3.x_env/bin/python
                    # ----------------
                    
                    juno.idfPath=
                    juno.idfPyPath=
                    juno.pythonPath=
                    juno.pythonExecutablePath=
                    juno.toolchainPath=
                    juno.cMakePath=
                    juno.ninjaPath=
                    juno.serialPort=
                    
                    # These are optional paths, can be left empty if not needed.
                    juno.gitPath=
                    juno.xtensaGdbPath=
                    juno.xtensaToolchainPath=
                    juno.espClangPath=
                    juno.openOcdBin=
                    juno.cCacheBinPath=
                    juno.dfuUtilBinPath=
                    juno.openOcdScriptsPath=
                    # Optional: If you want to edit this properties file manually, you can add any other custom properties here.
                    """;

            Files.writeString(propPath, template);
            System.out.println("📝 Created template .juno/juno.properties file.");
        } catch (IOException e) {
            System.err.println("❌ Failed to create juno.properties: " + e.getMessage());
        }
    }


    private JunoPaths() {
        // Prevent instantiation
    }
}
