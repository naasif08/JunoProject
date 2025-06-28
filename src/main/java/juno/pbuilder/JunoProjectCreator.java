package juno.pbuilder;

import juno.config.JunoPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JunoProjectCreator {

    private static String PROJECT_DIR = "Null Project";

    public static File createProject() throws IOException {
        File projectDir = JunoPaths.getProjectDir("ESP32Project");
        JunoProjectCreator.PROJECT_DIR = projectDir.getAbsolutePath();
        Path mainDir = projectDir.toPath().resolve("main");
        Files.createDirectories(mainDir);
        System.out.println("Created project directory: " + projectDir.getAbsolutePath());
        writeFiles();
        return projectDir;
    }

    private static void writeFiles() throws IOException {
        writeTextFile(PROJECT_DIR + "\\CMakeLists.txt", cmakeListsTxtContent());
        writeTextFile(PROJECT_DIR + "\\LICENSE", licenseContent());
        writeTextFile(PROJECT_DIR + "\\README.md", readmeContent());
        writeTextFile(PROJECT_DIR + "\\.clangd", clangdContent());
        writeTextFile(PROJECT_DIR + "\\.clang-format", clangFormatContent());
        writeTextFile(PROJECT_DIR + "\\.cproject", cprojectContent());
        writeTextFile(PROJECT_DIR + "\\.project", projectContent());
        writeTextFile(PROJECT_DIR + "\\.gitignore", gitignoreContent());

        writeTextFile(PROJECT_DIR + "\\main\\CMakeLists.txt", mainCMakeListsTxtContent());
        writeTextFile(PROJECT_DIR + "\\main\\Kconfig.projbuild", kconfigProjbuildContent());
        writeTextFile(PROJECT_DIR + "\\main\\juno_serial.h", junoSerialHeaderfile());
        writeTextFile(PROJECT_DIR + "\\main\\juno_serial.c", junoSerialSourceFile());
        writeTextFile(PROJECT_DIR + "\\main\\main.c", mainCContent());

        System.out.println("Created all project files.");
    }

    private static void writeTextFile(String path, String content) throws IOException {
        Files.write(Paths.get(path), content.getBytes());
        System.out.println("Created: " + path);
    }

    private static String cmakeListsTxtContent() {
        return """
                cmake_minimum_required(VERSION 3.16)
                include($ENV{IDF_PATH}/tools/cmake/project.cmake)
                project(ESP32Project)
                """;
    }

    private static String licenseContent() {
        return "/* MIT License - Example */\n";
    }

    private static String readmeContent() {
        return "# ESP32Project\nMinimal ESP-IDF project created by Java\n";
    }

    private static String clangdContent() {
        return "CompileFlags:\n  Add: [-I${workspaceFolder}/main]\n";
    }

    private static String clangFormatContent() {
        return """
                BasedOnStyle: Google
                IndentWidth: 4
                """;
    }

    private static String cprojectContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- Minimal .cproject file -->\n";
    }

    private static String projectContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- Minimal .project file -->\n";
    }

    private static String gitignoreContent() {
        return """
                # Ignore build output folder
                /build/
                
                # Ignore SDK config file
                /sdkconfig
                
                # Ignore Eclipse project files (if used)
                .cproject
                .project
                
                # Ignore temporary files
                *.log
                *.tmp
                
                # Ignore compiled object files and binaries
                *.o
                *.elf
                *.bin
                *.map
                
                # Ignore Python bytecode files
                __pycache__/
                *.pyc
                
                # Ignore VSCode settings
                .vscode/
                
                # Ignore any local user-specific files
                *.user
                """;
    }

    private static String mainCMakeListsTxtContent() {
        return """
                idf_component_register(SRCS "main.c" juno_serial.c
                                       INCLUDE_DIRS "")
                """;
    }

    private static String kconfigProjbuildContent() {
        return "# Minimal Kconfig.projbuild\n";
    }

    private static String junoSerialHeaderfile() {
        return """
                 #ifndef JUNO_SERIAL_H
                  #define JUNO_SERIAL_H
                  \s
                  /**
                   * @brief Initializing UART0.
                   * @return Pointer to a static null-terminated string. Overwritten on next call.
                   */\s
                  void juno_serial_init();\s
                
                  /**
                   * @brief Reads a line from UART0.
                   * @return Pointer to a static null-terminated string. Overwritten on next call.
                   */
                  const char *juno_serial_read();
                
                  /**
                   * @brief Sends a null-terminated string to UART0.
                   */
                  void juno_serial_write(const char *message);
                
                  #endif // JUNO_SERIAL_H
                
                """;
    }

    private static String junoSerialSourceFile() {
        return """
                #include "juno_serial.h"
                #include "driver/uart.h"
                #include "freertos/FreeRTOS.h"
                #include "freertos/task.h"
                #include <string.h>
                
                #define BUF_SIZE 512
                #define UART_NUM UART_NUM_0              
                
                static char internal_buffer[BUF_SIZE];              
                
                bool juno_serial_read_line(char *out, size_t maxLen) {
                    int index = 0;
                    char c;
                    while (true) {
                        int len = uart_read_bytes(UART_NUM_0, (uint8_t *)&c, 1, 100 / portTICK_PERIOD_MS);
                        if (len > 0) {
                            if (c == '\\n') {
                                out[index] = '\\0';
                                return true;
                            }
                            if (c != '\\r' && index < maxLen - 1) {
                                out[index++] = c;
                            }
                        }
                    }
                }
                
                
                void juno_serial_init() {                \s
                    static bool initialized = false;
                    if (initialized) return;
                
                    uart_config_t uart_config = {
                        .baud_rate = 115200,
                        .data_bits = UART_DATA_8_BITS,
                        .parity = UART_PARITY_DISABLE,
                        .stop_bits = UART_STOP_BITS_1,
                        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE
                    };
                
                    uart_param_config(UART_NUM_0, &uart_config);
                    uart_set_pin(UART_NUM_0, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE,
                                 UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE);
                    uart_driver_install(UART_NUM_0, BUF_SIZE * 2, 0, 0, NULL, 0);
                
                    // Block here until "flash" is received
                    char buffer[64];
                    while (true) {
                        if (juno_serial_read_line(buffer, sizeof(buffer))) {
                            if (strcmp(buffer, "flash") == 0) {
                               
                                uart_write_bytes(UART_NUM_0, "thunder\\n", strlen("thunder\\n"));
                                uart_wait_tx_done(UART_NUM_0, pdMS_TO_TICKS(50));
                                break;
                            }
                        }
                    }
                
                    initialized = true;
                }
                
                const char* juno_serial_read() {
                    static char buffer[BUF_SIZE];
                    static int index = 0;
                
                    // Send "ready" to host to indicate ESP32 is ready for a command
                   
                    uart_write_bytes(UART_NUM, "juno_read\\n", strlen("juno_read\\n"));
                     uart_wait_tx_done(UART_NUM, pdMS_TO_TICKS(50));
                
                    while (1) {
                        uint8_t ch;
                        int len = uart_read_bytes(UART_NUM_0, &ch, 1, portMAX_DELAY); // wait forever
                
                        if (len > 0) {
                            if (ch == '\\r') continue; // skip CR
                
                            if (ch == '\\n') {
                                buffer[index] = '\\0';
                                index = 0;
                
                                // Handle internal JUNO command
                                if (strcmp(buffer, "flash") == 0) {
                                    juno_serial_write("thunder");
                                    return NULL; // Skip this one, ask for next
                                }
                
                                return buffer;
                            }
                
                            if (index < BUF_SIZE - 1) {
                                buffer[index++] = ch;
                            }
                        }
                    }
                }
                
                
                void juno_serial_write(const char *message) {
                    if (message && strlen(message) > 0) {
                        char buffer[512];  // Adjust size as needed
                        snprintf(buffer, sizeof(buffer), "%s\\n", message);
                      
                        uart_write_bytes(UART_NUM, buffer, strlen(buffer));
                        uart_wait_tx_done(UART_NUM, pdMS_TO_TICKS(50));
                    }
                }
                
                
                """;
    }

    private static String mainCContent() {
        return """
                                #include "esp_log.h"
                                #include "juno_serial.h"
                                #include <stdbool.h>
                                #include <stdio.h>
                                #include <string.h>
                                #include <sys/unistd.h>
                                #include <unistd.h>
                
                                void app_main(void) {
                                  juno_serial_init();
                                  while (true) {                                      
                                          const char* line = juno_serial_read();                
                                              juno_serial_write(line); 
                                              juno_serial_write("hello world"); 
                                              juno_serial_write("new world"); 
                                      sleep(0.5);
                                  }
                                }
                
                """;
    }

}
