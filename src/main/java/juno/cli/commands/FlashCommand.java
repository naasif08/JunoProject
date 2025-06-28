package juno.cli.commands;

import juno.config.JunoDetector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FlashCommand {

    public static void run(String[] args) {
        String portName = null;
        boolean showHelp = false;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                case "-p":
                    if (i + 1 < args.length) {
                        portName = args[++i];
                    } else {
                        System.err.println("Error: --port requires a value.");
                        return;
                    }
                    break;
                case "--help":
                case "-h":
                    showHelp = true;
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    return;
            }
        }

        if (showHelp) {
            printHelp();
            return;
        }

        // Auto-detect port if not provided
        if (portName == null) {
            portName = JunoDetector.detectEsp32Port();
            if (portName == null) {
                System.err.println("Error: No serial port specified or detected.");
                System.err.println("Use --port <PORT> to specify the port.");
                return;
            } else {
                System.out.println("Auto-detected serial port: " + portName);
            }
        }

        List<String> command = new ArrayList<>();
        command.add("idf.py");
        command.add("-p");
        command.add(portName);
        command.add("flash");

        System.out.println("Running command: " + String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Flashing completed successfully.");
            } else {
                System.err.println("Flashing failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            System.err.println("Error running idf.py flash: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("""
                Usage: juno flash [options]
                
                Options:
                  -p, --port <PORT>    Specify the serial port (e.g., COM3 or /dev/ttyUSB0)
                  -h, --help           Show this help message
                
                Example:
                  juno flash --port COM4
                """);
    }
}
