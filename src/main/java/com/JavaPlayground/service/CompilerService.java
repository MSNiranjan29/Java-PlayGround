package com.JavaPlayground.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Service;

import com.JavaPlayground.model.CompilationResponse;

@Service
public class CompilerService {

    public CompilationResponse compileAndExecute(String code, String input) {
        CompilationResponse response = new CompilationResponse();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("java-compile-");

            String className = extractClassName(code);
            if (className == null) {
                response.setSuccess(false);
                response.setError("No public class found in code");
                return response;
            }

            Path javaFilePath = tempDir.resolve(className + ".java");
            Files.write(javaFilePath, code.getBytes());

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                response.setSuccess(false);
                response.setError("JDK required (JRE is not sufficient)");
                return response;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(javaFilePath.toFile());
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

                boolean success = task.call();
                if (!success) {
                    StringBuilder errorMsg = new StringBuilder();
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        errorMsg.append("Line ").append(diagnostic.getLineNumber()).append(": ")
                                .append(diagnostic.getMessage(null)).append("\n");
                    }
                    response.setSuccess(false);
                    response.setError("Compilation errors:\n" + errorMsg);
                    return response;
                }
            }

            // --- RUN Logic ---
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", tempDir.toString(), className);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // --- CRITICAL INPUT FIX ---
            // We must open the writer, write input (if any), and then CLOSE it.
            // Closing it sends EOF (End of File), so Scanner stops waiting.
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                if (input != null && !input.isEmpty()) {
                    writer.write(input);
                    writer.newLine(); // Ensure newline at end of input
                }
                // The try-with-resources block automatically calls writer.close() here.
                // This is ESSENTIAL to prevent "Execution Timed Out".
            } catch (IOException e) {
                // If program finished immediately, writing might fail. This is normal.
            }

            // Read Output
            StringBuilder output = new StringBuilder();
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            outputReader.start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                response.setSuccess(false);
                response.setError("Execution timed out (Program waited too long for input or infinite loop)");
                return response;
            }

            outputReader.join(2000);

            // Send back raw output (preserves spaces for patterns)
            response.setOutput(output.toString());
            response.setSuccess(process.exitValue() == 0);

            if (process.exitValue() != 0) {
                // Add extra hint for users if exit code is non-zero
                String err = output.toString();
                if (err.contains("NoSuchElementException")) {
                    err += "\n\n[Hint]: You used Scanner but didn't provide enough input in the Input box.";
                }
                response.setError(err);
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Error: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException ignored) {
                }
            }
        }
        return response;
    }

    private String extractClassName(String code) {
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.group(1) : null;
    }
}
