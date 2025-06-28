package juno.config;

import com.fazecast.jSerialComm.SerialPort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class JunoDetector {

    public static String detectIdfPath() {
        // 1. Check environment variable
        String idfPath = System.getenv("IDF_PATH");
        if (isValidIdfPath(idfPath)) {
            return idfPath;
        }

        // 2. Search in common locations
        List<Path> candidates = new ArrayList<>();

        if (isWindows()) {
            Path baseDir = Paths.get("C:", "Espressif", "frameworks");
            if (Files.exists(baseDir)) {
                try (Stream<Path> stream = Files.list(baseDir)) {
                    stream.filter(Files::isDirectory).filter(p -> p.getFileName().toString().startsWith("esp-idf")).forEach(candidates::add);
                } catch (IOException ignored) {
                }
            }
        } else {
            candidates.add(Paths.get(System.getProperty("user.home"), "esp", "esp-idf"));
            candidates.add(Paths.get(System.getProperty("user.home"), "esp-idf"));
            candidates.add(Paths.get("/opt", "espressif", "esp-idf"));
        }

        for (Path p : candidates) {
            if (isValidIdfPath(p.toString())) {
                return p.toString();
            }
        }

        return null; // No valid IDF path found
    }

    private static boolean isValidIdfPath(String path) {
        if (path == null) return false;

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) return false;

        boolean hasTools = new File(dir, "tools").exists();
        boolean hasComponents = new File(dir, "components").exists();
        boolean hasExportScript = new File(dir, isWindows() ? "export.bat" : "export.sh").exists();

        return hasTools && hasComponents && hasExportScript;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static String detectToolchainBin() {
        String os = System.getProperty("os.name").toLowerCase();
        String gccExecutable = os.contains("win") ? "xtensa-esp-elf-gcc.exe" : "xtensa-esp-elf-gcc";

        // 2. Fallback to common path
        File fallbackDir = os.contains("win") ? new File("C:\\Espressif\\tools\\xtensa-esp-elf") : new File(System.getProperty("user.home") + "/.espressif/tools/xtensa-esp-elf");

        String found = searchIdfToolChainRecursively(fallbackDir, gccExecutable);
        return found != null ? new File(found).getParentFile().getAbsolutePath() : null;
    }

    public static String detectCcacheBin() {
        String os = System.getProperty("os.name").toLowerCase();
        String ccacheExecutable = os.contains("win") ? "ccache.exe" : "ccache";

        // 2. Fallback to common path
        File fallbackDir = os.contains("win") ? new File("C:\\Espressif\\tools\\ccache") : new File(System.getProperty("user.home") + "/.espressif/tools/ccache");

        String found = searchIdfToolChainRecursively(fallbackDir, ccacheExecutable);
        return found != null ? new File(found).getParentFile().getAbsolutePath() : null;
    }


    public static String detectPythonPath() {
        String idfPath = System.getenv("IDF_PATH");
        String idfToolsPath = System.getenv("IDF_TOOLS_PATH");

        if (idfPath != null) {
            File toolsRootDir = new File(idfPath);
            return findEspressifPythonPathOnly(toolsRootDir);
        }
        File toolsRootDir = new File(idfToolsPath);
        return findEspressifPythonPathOnly(toolsRootDir);
    }

    public static String detectPythonExecutable() {
        String idfPath = System.getenv("IDF_PATH");
        String idfToolsPath = System.getenv("IDF_TOOLS_PATH");

        if (idfPath != null) {
            File toolsRootDir = new File(idfPath);
            return findEspressifPythonPath(toolsRootDir);
        }
        File toolsRootDir = new File(idfToolsPath);
        return findEspressifPythonPath(toolsRootDir);
    }

    public static String findEspressifPythonPathOnly(File toolchainBinDir) {
        // Climb up to the Espressif root directory
        File current = toolchainBinDir;
        while (current != null && !current.getName().equalsIgnoreCase("Espressif")) {
            current = current.getParentFile();
        }

        if (current == null || !current.exists()) {
            return null;
        }

        return searchForPythonPath(current);
    }

    public static String findEspressifPythonPath(File toolchainBinDir) {
        // Climb up to the Espressif root directory
        File current = toolchainBinDir;
        while (current != null && !current.getName().equalsIgnoreCase("Espressif")) {
            current = current.getParentFile();
        }

        if (current == null || !current.exists()) {
            return null;
        }

        // Recursively search for python executable
        return searchForPythonExecutable(current);
    }


    private static String searchForPythonPath(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                String found = searchForPythonPath(f);
                if (found != null) return found;
            } else {
                String name = f.getName().toLowerCase();
                String os = System.getProperty("os.name").toLowerCase();
                boolean isPython = os.contains("win") ? name.equals("python.exe") : (name.equals("python3") || name.equals("python"));
                if (isPython) {
                    return dir.getAbsolutePath();
                }
            }
        }

        return null;
    }

    private static String searchForPythonExecutable(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                String found = searchForPythonExecutable(f);
                if (found != null) return found;
            } else {
                String name = f.getName().toLowerCase();
                String os = System.getProperty("os.name").toLowerCase();
                boolean isPython = os.contains("win") ? name.equals("python.exe") : (name.equals("python3") || name.equals("python"));
                if (isPython) return f.getAbsolutePath();
            }
        }

        return null;
    }

    private static File findBinDir(File dir) {
        // Search upwards or known structure for bin folder
        // Simple heuristic: if current dir ends with "bin", return it
        if (dir.getName().equalsIgnoreCase("bin") && dir.exists()) {
            return dir;
        }
        // else check for bin inside dir
        File bin = new File(dir, "bin");
        if (bin.exists()) {
            return bin;
        }
        // climb up one level and try again, max 3 times
        File parent = dir.getParentFile();
        for (int i = 0; i < 3 && parent != null; i++) {
            bin = new File(parent, "bin");
            if (bin.exists()) return bin;
            parent = parent.getParentFile();
        }
        return null;
    }

    public static String detectOpenOcdBin() {
        String os = System.getProperty("os.name").toLowerCase();
        String exeName = os.contains("win") ? "openocd.exe" : "openocd";

        File espToolsDir = os.contains("win") ? new File("C:\\Espressif\\tools") : new File(System.getProperty("user.home") + "/.espressif/tools");

        String openOcdPath = searchFolderRecursively(espToolsDir, exeName);

        return openOcdPath;
    }

    private static String searchFolderRecursively(File dir, String targetFileName) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                String result = searchFolderRecursively(f, targetFileName);
                if (result != null) return result;
            } else if (f.getName().equalsIgnoreCase(targetFileName) && f.canExecute()) {
                return dir.getAbsolutePath();
            }
        }
        return null;
    }

    public static String searchIdfToolChainRecursively(File dir, String targetFileName) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null) return null;

        List<File> fileList = Arrays.asList(files);
        Collections.reverse(fileList);

        Optional<String> result = fileList.stream().map(f -> {
            if (f.isDirectory()) {
                return searchIdfToolChainRecursively(f, targetFileName);
            } else if (f.getName().equalsIgnoreCase(targetFileName) && f.canExecute()) {
                return f.getAbsolutePath();
            } else {
                return null;
            }
        }).filter(path -> path != null).findFirst();

        return result.orElse(null);
    }

    public static String searchFileRecursively(File dir, String targetFileName) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                String result = searchFileRecursively(f, targetFileName);
                if (result != null) return result;
            } else if (f.getName().equalsIgnoreCase(targetFileName) && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    public static String detectEsp32Port() {
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            String desc = port.getDescriptivePortName().toLowerCase();
            String systemName = port.getSystemPortName().toLowerCase();

            // Common USB-to-serial chip identifiers for ESP32 adapters
            if (desc.contains("ch340") || desc.contains("usb serial") || desc.contains("cp210x") || desc.contains("silicon labs") || desc.contains("ftdi") || systemName.contains("usbserial") ||  // catch generic usbserial names
                    systemName.contains("ttyusb") ||     // Linux typical USB serial devices
                    systemName.contains("cu.usbserial") // macOS typical device name prefix
            ) {
                return port.getSystemPortName();
            }
        }

        // If no common ESP32 port found, return null or fallback
        return null;
    }

    public static String test() {
        System.out.println("testing");
        return null;
    }

    public static String detectEspressifGitPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String espressifRoot;

        if (os.contains("win")) {
            espressifRoot = "C:\\Espressif\\tools\\idf-git";
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            // Common install path for ESP-IDF on Unix-like systems
            espressifRoot = System.getProperty("user.home") + "/.espressif/tools/idf-git";
        } else {
            return null;
        }

        File rootDir = new File(espressifRoot);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return null;
        }

        File[] versions = rootDir.listFiles(File::isDirectory);
        if (versions == null) return null;

        for (File versionDir : versions) {
            File candidate;
            if (os.contains("win")) {
                candidate = new File(versionDir, "cmd/git.exe");
            } else {
                candidate = new File(versionDir, "bin/git");  // Typical layout on Unix systems
            }

            if (candidate.exists() && candidate.canExecute()) {
                return candidate.getAbsolutePath();
            }
        }

        return null;
    }

    public static String detectCmakePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String basePath = os.contains("win") ? "C:\\Espressif\\tools\\cmake" : System.getProperty("user.home") + "/.espressif/tools/cmake";

        File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) return null;

        File[] versions = baseDir.listFiles(File::isDirectory);
        if (versions == null) return null;

        for (File version : versions) {
            File candidate = os.contains("win") ? new File(version, "cmake.exe") : new File(version, "bin/cmake");
            if (candidate.exists() && candidate.canExecute()) {
                return version.getAbsolutePath();
            }
        }
        return null;
    }

    public static String detectNinjaPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String basePath = os.contains("win") ? "C:\\Espressif\\tools\\ninja" : System.getProperty("user.home") + "/.espressif/tools/ninja";

        File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) return null;

        File[] versions = baseDir.listFiles(File::isDirectory);
        if (versions == null) return null;

        for (File version : versions) {
            File candidate = os.contains("win") ? new File(version, "ninja.exe") : new File(version, "bin/ninja");
            if (candidate.exists() && candidate.canExecute()) {
                return version.getAbsolutePath();
            }
        }
        return null;
    }


    public static String detectIdfPyPath() {
        String idfPath = detectIdfPath();  // This must return the base ESP-IDF directory
        if (idfPath == null) return null;

        File idfPy = new File(idfPath, "tools/idf.py");
        if (idfPy.exists()) {
            return idfPy.getParent();
        }

        return null;  // Not found
    }


    public static String detectXtensaGdbPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String gdbExecutable = os.contains("win") ? "xtensa-esp32-elf-gdb.exe" : "xtensa-esp32-elf-gdb";

        File espToolsDir = os.contains("win") ? new File("C:\\Espressif\\tools\\xtensa-esp-elf-gdb") : new File(System.getProperty("user.home") + "/.espressif/tools/xtensa-esp-elf-gdb");

        String gdbPath = searchFileRecursively(espToolsDir, gdbExecutable);
        return gdbPath != null ? new File(gdbPath).getParent() : null; // return the bin directory
    }

    public static String detectXtensaToolchainPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String toolExecutable = os.contains("win") ? "xtensa-esp-elf-gcc.exe" : "xtensa-esp-elf-gcc";

        File espToolsDir = os.contains("win") ? new File("C:\\Espressif\\tools\\xtensa-esp-elf") : new File(System.getProperty("user.home") + "/.espressif/tools/xtensa-esp-elf");

        String toolPath = searchIdfToolChainRecursively(espToolsDir, toolExecutable);

        if (toolPath != null) {
            // toolPath is absolute path to executable, get parent dir = bin folder
            return new File(toolPath).getParent();
        }

        return null;
    }

    public static String detectDfuUtilBin() {
        String os = System.getProperty("os.name").toLowerCase();
        String dfuUtilExecutable = os.contains("win") ? "dfu-util.exe" : "dfu-util";

        File fallbackDir = os.contains("win") ? new File("C:\\Espressif\\tools\\dfu-util") : new File(System.getProperty("user.home") + "/.espressif/tools/dfu-util");

        String found = searchIdfToolChainRecursively(fallbackDir, dfuUtilExecutable);
        return found != null ? new File(found).getParentFile().getAbsolutePath() : null;
    }

    public static String detectOpenOcdScriptsPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String targetFolder = "memory.tcl";

        // Start from the root OpenOCD tools directory
        File openocdRoot = os.contains("win") ? new File("C:\\Espressif\\tools\\openocd-esp32") : new File(System.getProperty("user.home") + "/.espressif/tools/openocd-esp32");

        String scriptsPath = searchIdfToolChainRecursively(openocdRoot, targetFolder);
        if (scriptsPath != null) {
            File scriptsDir = new File(scriptsPath);
            return scriptsDir.getParent();
        }
        return null;
    }


    public static String detectEspClangPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String clangExecutable = os.contains("win") ? "clang.exe" : "clang";

        File espClangRoot = os.contains("win") ? new File("C:\\Espressif\\tools\\esp-clang") : new File(System.getProperty("user.home") + "/.espressif/tools/esp-clang");

        String pathToClang = searchFileRecursively(espClangRoot, clangExecutable);
        return pathToClang != null ? new File(pathToClang).getParent() : null;  // returns .../esp-clang/bin
    }

    public static void printDetectedPaths() {
        // Print loaded paths for confirmation
        System.out.println("Printing Detected Paths");
        System.out.println("-----------------------");
        System.out.println("IDF Path → " + JunoDetector.detectIdfPath());
        System.out.println("Python Path → " + JunoDetector.detectPythonPath());
        System.out.println("PythonExe Path → " + JunoDetector.detectPythonExecutable());
        System.out.println("Toolchain Path → " + JunoDetector.detectToolchainBin());
        System.out.println("ESP32 Serial Port → " + JunoDetector.detectEsp32Port());
        System.out.println("Git Path → " + JunoDetector.detectEspressifGitPath());

        System.out.println("Xtensa GDB Path → " + JunoDetector.detectXtensaGdbPath());
        System.out.println("Xtensa Toolchain Path → " + JunoDetector.detectXtensaToolchainPath());
        System.out.println("ESP Clang path → " + JunoDetector.detectEspClangPath());
        System.out.println("Cmake Path → " + JunoDetector.detectCmakePath());
        System.out.println("OPENOCD Path Bin → " + JunoDetector.detectOpenOcdBin());
        System.out.println("Ninja Path → " + JunoDetector.detectNinjaPath());
        System.out.println("idf.py Path → " + JunoDetector.detectIdfPyPath());
        System.out.println("ccache Path → " + JunoDetector.detectCcacheBin());
        System.out.println("dfu-util Path → " + JunoDetector.detectDfuUtilBin());
        System.out.println("OPENOCD_SCRIPTS path → " + JunoDetector.detectOpenOcdScriptsPath());
        System.out.println("-----------------------");
    }

}
