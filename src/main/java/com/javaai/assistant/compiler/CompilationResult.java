package com.javaai.assistant.compiler;

/**
 * Result returned by {@link JavaCompilerService} after a compilation attempt.
 */
public final class CompilationResult {

    private final boolean success;
    private final String  output;

    public CompilationResult(boolean success, String output) {
        this.success = success;
        this.output  = output;
    }

    /** @return {@code true} if compilation produced no errors. */
    public boolean isSuccess() { return success; }

    /** @return Human-readable compiler output (errors, warnings, or success message). */
    public String getOutput()  { return output; }
}
