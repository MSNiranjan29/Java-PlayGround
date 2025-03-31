package com.JavaPlayground.service;

import com.JavaPlayground.model.CompilationResponse;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CompilerService {

    public CompilationResponse compileAndExecute(String code) {
        CompilationResponse response = new CompilationResponse();
        Path tempDir = null;
        
        try {
            // Create temporary directory
            tempDir = Files.createTempDirectory("java-compile-");
            
            // Extract class name from code
            String className = extractClassName(code);
            if (className == null) {
                response.setSuccess(false);
                response.setError("No public class found in code");
                return response;
            }

            // Create Java source file
            Path javaFilePath = tempDir.resolve(className + ".java");
            Files.write(javaFilePath, code.getBytes());

            // Compile Java file
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                response.setSuccess(false);
                response.setError("JDK required to run compiler (JRE is not sufficient)");
                return response;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
                Iterable<? extends JavaFileObject> compilationUnits =
                        fileManager.getJavaFileObjects(javaFilePath.toFile());
                
                JavaCompiler.CompilationTask task = compiler.getTask(
                        null,
                        fileManager,
                        diagnostics,
                        null,
                        null,
                        compilationUnits
                );

                boolean compilationSuccess = task.call();
                
                if (!compilationSuccess) {
                    StringBuilder errorMsg = new StringBuilder();
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        errorMsg.append("Line ")
                                .append(diagnostic.getLineNumber())
                                .append(": ")
                                .append(diagnostic.getMessage(null))
                                .append("\n");
                    }
                    response.setSuccess(false);
                    response.setError("Compilation errors:\n" + errorMsg);
                    return response;
                }
            }

            // Execute compiled class
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "-cp",
                    tempDir.toString(),
                    className
            );
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            
            // Read output in separate thread
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
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
                response.setError("Execution timed out after 5 seconds");
                return response;
            }

            outputReader.join(2000);
            
            response.setOutput(output.toString().trim());
            response.setSuccess(process.exitValue() == 0);
            if (process.exitValue() != 0) {
                response.setError("Non-zero exit code: " + process.exitValue());
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Error: " + e.getMessage());
        } finally {
            // Clean up temporary files
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                         .sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(File::delete);
                } catch (IOException e) {
                    System.err.println("Failed to delete temp directory: " + e.getMessage());
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
