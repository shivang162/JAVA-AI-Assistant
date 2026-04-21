package com.javaai.assistant.compiler;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles Java source code using the built-in {@link JavaCompiler} API
 * ({@code javax.tools}). Only {@code .java} files are accepted; the service
 * writes source to a temporary directory, compiles it, and cleans up.
 *
 * <p><strong>Note:</strong> the code is compiled but never executed – this
 * keeps the tool safe for untrusted input.
 *
 * <p><strong>Requirement:</strong> the application must be run with a JDK
 * (not just a JRE) so that {@code javax.tools.ToolProvider.getSystemJavaCompiler()}
 * returns a non-null value.
 */
public class JavaCompilerService {

    /** Regex that extracts the first public type name from Java source. */
    private static final Pattern CLASS_NAME_PATTERN =
            Pattern.compile("\\bpublic\\s+(?:class|interface|enum|record|@interface)\\s+(\\w+)");

    /**
     * Compiles {@code sourceCode} and returns a {@link CompilationResult}.
     *
     * @param sourceCode Java source code (must contain a public class / interface / enum / record)
     * @return compilation result with success flag and human-readable output
     */
    public CompilationResult compile(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return new CompilationResult(false, "No source code to compile.");
        }

        // ---- Obtain the system Java compiler --------------------------------
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompilationResult(false,
                    "Java compiler not found.\n" +
                    "Please make sure you are running with a JDK (not just a JRE).");
        }

        // ---- Resolve the public class name ----------------------------------
        String className = extractClassName(sourceCode);
        if (className == null) {
            // Fall back to a generic name if no public type is declared
            className = "Main";
        }

        // ---- Write source to a temp directory -------------------------------
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("javaai_compile_");
            Path sourceFile = tempDir.resolve(className + ".java");
            Files.write(sourceFile, sourceCode.getBytes(StandardCharsets.UTF_8));

            // ---- Capture diagnostic messages --------------------------------
            StringWriter diagnosticsWriter = new StringWriter();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            try (StandardJavaFileManager fileManager =
                         compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

                Iterable<? extends JavaFileObject> compilationUnits =
                        fileManager.getJavaFileObjects(sourceFile.toFile());

                JavaCompiler.CompilationTask task = compiler.getTask(
                        diagnosticsWriter,
                        fileManager,
                        diagnostics,
                        Arrays.asList("-d", tempDir.toString()), // compiler options: output dir
                        null,
                        compilationUnits
                );

                boolean success = task.call();

                // ---- Format diagnostics output ------------------------------
                StringBuilder sb = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    sb.append(formatDiagnostic(d)).append('\n');
                }
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                    sb.deleteCharAt(sb.length() - 1);
                }

                if (success && sb.length() == 0) {
                    return new CompilationResult(true,
                            "✔ Compilation successful – class: " + className);
                } else if (success) {
                    return new CompilationResult(true,
                            "✔ Compilation successful (with warnings)\n\n" + sb);
                } else {
                    return new CompilationResult(false,
                            "✘ Compilation failed\n\n" + sb);
                }
            }

        } catch (IOException e) {
            return new CompilationResult(false, "I/O error during compilation: " + e.getMessage());
        } finally {
            // ---- Clean up temp directory ------------------------------------
            if (tempDir != null) {
                deleteDirectory(tempDir.toFile());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String extractClassName(String source) {
        Matcher m = CLASS_NAME_PATTERN.matcher(source);
        return m.find() ? m.group(1) : null;
    }

    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> d) {
        String kind = switch (d.getKind()) {
            case ERROR   -> "ERROR";
            case WARNING -> "WARNING";
            case NOTE    -> "NOTE";
            default      -> d.getKind().name();
        };
        long line = d.getLineNumber();
        long col  = d.getColumnNumber();
        String location = (line > 0) ? " (line " + line + ", col " + col + ")" : "";
        return "[" + kind + "]" + location + "  " + d.getMessage(null);
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f); else f.delete();
            }
        }
        dir.delete();
    }
}
