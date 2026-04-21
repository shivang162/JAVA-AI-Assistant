package com.javaai.assistant.compiler;

/**
 * Result returned by {@link JavaCompilerService} after a compilation attempt.
 */
public final class CompilationResult {

    private final boolean success;
    private final String  output;
    private final Integer errorLine;

    public CompilationResult(boolean success, String output) {
        this(success, output, null);
    }

    public CompilationResult(boolean success, String output, Integer errorLine) {
        this.success = success;
        this.output  = output;
        this.errorLine = errorLine;
    }

    /** @return {@code true} if compilation produced no errors. */
    public boolean isSuccess() { return success; }

    /** @return Human-readable compiler output (errors, warnings, or success message). */
    public String getOutput()  { return output; }

    /** @return first error line (1-based) if compilation failed, otherwise {@code null}. */
    public Integer getErrorLine() { return errorLine; }
}
