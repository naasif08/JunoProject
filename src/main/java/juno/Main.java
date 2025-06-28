package juno;

import juno.builder.JunoBatchBuilder;
import juno.config.JunoConfig;
import juno.flasher.JunoFlasher;
import juno.pbuilder.JunoProjectCreator;
import juno.serial.JunoSerial;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        JunoConfig.load();
        String serialPort = JunoConfig.updateSerialPortAndSave();

        // Create project structure (in .juno/ESP32Project)
        File projectDir = JunoProjectCreator.createProject();

        JunoBatchBuilder batchBuilder = new JunoBatchBuilder();
        batchBuilder.writeBuildScripts(projectDir, serialPort);

        // Run the flasher (executes the batch file)
        JunoFlasher flasher = new JunoFlasher();
        flasher.flashProject(projectDir);

        //For Serial communication
        JunoSerial.startJunoSerial();

        System.out.println("✅ Done!");
    }
}
