package juno.pbuilder;

import java.io.File;

import static juno.config.JunoDetector.searchFileRecursively;

public class CMaker {


    public static String detectXtensaGdbPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String gdbExecutable = os.contains("win") ? "xtensa-esp32-elf-gdb.exe" : "xtensa-esp-elf-gdb";

        File espToolsDir = os.contains("win") ? new File("C:\\Espressif\\tools\\xtensa-esp-elf-gdb") : new File(System.getProperty("user.home") + "/.espressif/tools/xtensa-esp-elf-gdb");

        System.out.println("Printing GDB Path:" + gdbExecutable);
        String gdbPath = searchFileRecursively(espToolsDir, gdbExecutable);
        return gdbPath != null ? new File(gdbPath).getParent() : null; // return the bin directory
    }


    public static void main(String[] arg) {
        System.out.println("Final path:" + CMaker.detectXtensaGdbPath());
    }

}