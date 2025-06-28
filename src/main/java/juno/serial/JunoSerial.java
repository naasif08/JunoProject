package juno.serial;

import com.fazecast.jSerialComm.SerialPort;
import juno.config.JunoConfig;
import juno.config.JunoDetector;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class JunoSerial {

    private SerialPort comPort;
    private InputStream in;
    private OutputStream out;

    public boolean connect() {
        // Get port from config or fallback to auto-detection
        String portName = JunoConfig.getInstance().serialPort;
        if (portName == null || portName.trim().isEmpty()) {
            portName = JunoDetector.detectEsp32Port();
            if (portName == null) {
                System.err.println("❌ Could not detect ESP32 serial port.");
                return false;
            }
            System.out.println("⚠️ Using auto-detected port: " + portName);
        }

        comPort = SerialPort.getCommPort(portName);
        comPort.setBaudRate(115200);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 500);

        if (!comPort.openPort()) {
            System.err.println("❌ Failed to open port: " + portName);
            return false;
        }
        in = comPort.getInputStream();
        out = comPort.getOutputStream();
        return true;
    }

    public void startTerminal() {
        if (comPort == null || !comPort.isOpen()) {
            System.err.println("❌ Port not open.");
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String response = readLine();
                // 🔹 Always print whatever ESP32 sends
                if (response != null && !response.isBlank()) {
                    if (!response.trim().equals("juno_read")) {
                        System.out.println("esp32:" + response.trim());
                    }
                }

                // 🔹 But only ask for input when ESP says "juno_read"
                if (response != null && response.trim().equalsIgnoreCase("juno_read")) {
                    System.out.print(">> ");
                    String line = scanner.nextLine();

                    if (line.equalsIgnoreCase("exit")) break;

                    send(line + "\n");
                    String reply = readLine();

                    if (reply != null && !reply.isBlank()) {
                        if (!reply.trim().equals("juno_read")) {
                            System.out.println("esp32: " + reply.trim());
                        }
                    } else {
                        System.out.println("⚠️ ESP32 did not reply after command.");
                    }
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ceasePort();
        }
    }


    public void ceasePort() {
        if (comPort != null && comPort.isOpen()) {
            try {
                in.close();
                out.close();
            } catch (Exception ignored) {
            }
            comPort.closePort();
            System.out.println("🔌 Port closed.");
        }
    }

    public void disconnect() {
        if (comPort != null && comPort.isOpen()) {
            try {
                in.close();
                out.close();
            } catch (Exception ignored) {
            }
            comPort.closePort();
        }
    }

    public void send(String message) {
        try {
            out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            System.err.println("❌ Failed to send: " + e.getMessage());
        }
    }

    public String readLine() {
        StringBuilder sb = new StringBuilder();
        try {
            while (true) {
                if (in.available() > 0) {
                    int b = in.read();
                    if (b == -1) break;
                    if (b == '\n') break;
                    if (b != '\r') sb.append((char) b);
                } else {
                    Thread.sleep(10); // wait for bytes to arrive
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            System.err.println("❌ Failed to read line: " + e.getMessage());
            return null;
        }
    }


    // Flash → Thunder handshake
    private boolean isEspConnected() {
        send("flash");
        String response = readLine();
        return response != null && response.trim().equalsIgnoreCase("thunder");
    }

    public static void startJunoSerial() throws InterruptedException {
        Thread.sleep(500);
        JunoSerial js = new JunoSerial();
        if (js.connect()) {
            if (js.isEspConnected()) {
                System.out.println("⚡ ESP32 is connected and ready (thunder received).");
                js.startTerminal();
            } else {
                js.disconnect();
            }
        } else {
            System.err.println("❌ Could not connect to ESP32.");
        }
    }
}
