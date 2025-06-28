package juno.builder;

import juno.config.JunoPaths;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JunoBatchBuilder {

    private static final String IDF_PATH = JunoPaths.idfPath;
    private static final String PYTHON_EXE_PATH = JunoPaths.pythonExecutablePath;
    private static final String PATH = Stream.of(JunoPaths.xtensaGdbPath, JunoPaths.xtensaToolchainPath, JunoPaths.espClangPath, JunoPaths.cMakePath, JunoPaths.openOcdBin, JunoPaths.ninjaPath, JunoPaths.idfPyPath, JunoPaths.cCacheBinPath, JunoPaths.dfuUtilBinPath, JunoPaths.pythonPath, JunoPaths.openOcdScriptsPath).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(";"));

    private static final String OPENOCD_SCRIPTS = JunoPaths.openOcdScriptsPath;
    private static final String GIT_PATH = JunoPaths.gitPath;

    public void writeBuildScripts(File projectDir, String comPort) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            writeBatchFile(projectDir, comPort);
        } else if (osName.contains("mac") || osName.contains("nix") || osName.contains("nux")) {
            writeBashScript(projectDir, comPort);
        } else {
            throw new UnsupportedOperationException("Unsupported OS for script generation: " + osName);
        }
    }

    private void writeBatchFile(File projectDir, String comPort) throws IOException {
        File batchFile = new File(projectDir, "esp32_build_flash.bat");

        String batchContent = """
                
                @echo off
                                REM === Set ESP-IDF Environment Variables ===
                
                                set "IDF_PATH=%s"
                
                                REM Add required tool paths to PATH
                                set "PATH=%s"
                
                                set "OPENOCD_SCRIPTS=%s"              
                
                                set "PYTHON_EXE_PATH=%s"
                
                                set "GIT_PATH=%s"
                
                                REM === Project directory ===
                                cd /d "%s"                
                
                                REM === Build the project from Directory ===
                                echo Building project...
                                call "%%PYTHON_EXE_PATH%%" "%%IDF_PATH%%\\\\tools\\\\idf.py" build
                                if errorlevel 1 (
                                    echo Build failed! Exiting...
                                    pause
                                    exit /b 1
                                )
                
                                REM === Flash the project ===
                                echo Flashing project...
                                call "%%PYTHON_EXE_PATH%%" "%%IDF_PATH%%\\\\tools\\\\idf.py" -p %s flash
                                if errorlevel 1 (
                                    echo Flash failed! Exiting...
                                    pause
                                    exit /b 1
                                )
                                REM === Done ===
                                echo ✅ Operation completed.
                
                
                
                """.formatted(IDF_PATH, PATH, OPENOCD_SCRIPTS, PYTHON_EXE_PATH, GIT_PATH, projectDir.getAbsolutePath(), comPort, comPort);


        try (FileWriter writer = new FileWriter(batchFile)) {
            writer.write(batchContent);
        }

        System.out.println("✅ Batch file written to: " + batchFile.getAbsolutePath());

    }

    private void writeBashScript(File projectDir, String comPort) throws IOException {
        File bashFile = new File(projectDir, "esp32_build_flash.sh");

        String bashContent = """
                #!/bin/bash
                
                # === Set ESP-IDF Environment ===
                export IDF_PATH="%s"
                export OPENOCD_SCRIPTS="%s"
                export PYTHON_EXE_PATH="%s"
                export GIT_PATH="%s"
                export PATH="%s:$IDF_PATH/tools:$PATH"
                
                # === Change to project directory ===
                cd "%s" || {
                    echo "❌ Failed to change to project directory!"
                    exit 1
                }
                
                # === Source ESP-IDF export script ===
                if [ -f "$IDF_PATH/export.sh" ]; then
                    source "$IDF_PATH/export.sh"
                else
                    echo "❌ export.sh not found in IDF_PATH"
                    exit 1
                fi
                
                # === Build the project ===
                echo "🔨 Building project..."
                "$PYTHON_EXE_PATH" "$IDF_PATH/tools/idf.py" build
                if [ $? -ne 0 ]; then
                    echo "❌ Build failed! Exiting..."
                    exit 1
                fi
                
                # === Flash the project ===
                echo "🚀 Flashing project to %s..."
                "$PYTHON_EXE_PATH" "$IDF_PATH/tools/idf.py" -p "%s" flash
                if [ $? -ne 0 ]; then
                    echo "❌ Flash failed! Exiting..."
                    exit 1
                fi
               
                
                # === Done ===
                echo "✅ Operation completed."
                """.formatted(IDF_PATH, OPENOCD_SCRIPTS, PYTHON_EXE_PATH, GIT_PATH, PATH, projectDir.getAbsolutePath(), comPort, comPort, comPort);

        try (FileWriter writer = new FileWriter(bashFile)) {
            writer.write(bashContent);
        }

        bashFile.setExecutable(true); // Make script executable

        System.out.println("✅ Bash script written to: " + bashFile.getAbsolutePath());
    }


}
