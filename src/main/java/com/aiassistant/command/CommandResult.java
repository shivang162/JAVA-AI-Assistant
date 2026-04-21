package com.aiassistant.command;

/** Result of processing CLI command input. */
public class CommandResult {
    private final boolean handled;
    private final boolean shouldExit;
    private final String message;

    public CommandResult(boolean handled, boolean shouldExit, String message) {
        this.handled = handled;
        this.shouldExit = shouldExit;
        this.message = message;
    }

    public static CommandResult ignored() {
        return new CommandResult(false, false, "");
    }

    public static CommandResult handled(String message) {
        return new CommandResult(true, false, message);
    }

    public static CommandResult exit(String message) {
        return new CommandResult(true, true, message);
    }

    public boolean isHandled() {
        return handled;
    }

    public boolean isShouldExit() {
        return shouldExit;
    }

    public String getMessage() {
        return message;
    }
}
