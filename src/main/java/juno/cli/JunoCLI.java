package juno.cli;

import juno.cli.commands.FlashCommand;

import java.util.Arrays;

public class JunoCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String command = args[0];
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (command) {
            case "flash" -> FlashCommand.run(subArgs);
            case "--help", "help" -> printHelp();
            case "--version", "version" -> printVersion();
            default -> {
                System.out.println("Unknown command: " + command);
                printHelp();
            }
        }
    }

    private static void printHelp() {
        System.out.println("""
            JUNO CLI - Java to ESP32 Development Tool

            Usage:
              juno <command> [options]

            Available commands:
              flash         Flash firmware to ESP32
              help          Show this help message
              version       Show CLI version

            Example:
              juno flash --port COM3
            """);
    }

    private static void printVersion() {
        System.out.println("JUNO CLI version 0.1.0");
    }
}
