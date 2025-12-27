package com.JavaPlayground.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.tools.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    // Store active processes so we can send input to them later
    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private final Map<String, Path> sessionTempDirs = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        stopProcess(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 1. RUN COMMAND: Client sends "RUN:<code>"
        if (payload.startsWith("RUN:")) {
            String code = payload.substring(4);
            runCode(session, code);
        }
        // 2. INPUT COMMAND: Client sends "INPUT:<data>"
        else if (payload.startsWith("INPUT:")) {
            String inputData = payload.substring(6);
            sendInputToProcess(session.getId(), inputData);
        }
    }

    private void runCode(WebSocketSession session, String code) {
        Thread thread = new Thread(() -> {
            Path tempDir = null;
            try {
                // A. Setup
                tempDir = Files.createTempDirectory("java-ws-");
                sessionTempDirs.put(session.getId(), tempDir);

                String className = extractClassName(code);
                if (className == null) {
                    session.sendMessage(new TextMessage("ERROR:No public class found"));
                    return;
                }

                Path javaFile = tempDir.resolve(className + ".java");
                Files.write(javaFile, code.getBytes());

                // B. Compile
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) {
                    session.sendMessage(new TextMessage("ERROR:Server JDK missing"));
                    return;
                }

                ByteArrayOutputStream errStream = new ByteArrayOutputStream();
                int result = compiler.run(null, null, errStream, javaFile.toString());

                if (result != 0) {
                    session.sendMessage(new TextMessage("ERROR:Compilation Failed:\n" + errStream.toString()));
                    return;
                }

                // C. Run Process
                ProcessBuilder pb = new ProcessBuilder("java", "-cp", tempDir.toString(), className);
                pb.redirectErrorStream(true); // Merge stdout and stderr
                Process process = pb.start();
                activeProcesses.put(session.getId(), process);

                // Start thread to read Output and send to Client
                InputStream is = process.getInputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    if (!session.isOpen()) break;
                    String output = new String(buffer, 0, read);
                    session.sendMessage(new TextMessage("OUTPUT:" + output));
                }

                // Wait for exit
                int exitCode = process.waitFor();
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("EXIT:Program finished with exit code " + exitCode));
                }

            } catch (Exception e) {
                try {
                    if (session.isOpen()) session.sendMessage(new TextMessage("ERROR:" + e.getMessage()));
                } catch (IOException ignored) {}
            } finally {
                stopProcess(session.getId());
            }
        });
        thread.start();
    }

    private void sendInputToProcess(String sessionId, String input) {
        Process process = activeProcesses.get(sessionId);
        if (process != null && process.isAlive()) {
            try {
                OutputStream os = process.getOutputStream();
                os.write((input + "\n").getBytes()); // Append newline!
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopProcess(String sessionId) {
        Process process = activeProcesses.remove(sessionId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        Path tempDir = sessionTempDirs.remove(sessionId);
        if (tempDir != null) {
            try {
                Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException ignored) {}
        }
    }

    private String extractClassName(String code) {
        Matcher m = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : null;
    }
}