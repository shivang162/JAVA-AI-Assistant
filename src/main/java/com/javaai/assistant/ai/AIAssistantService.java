package com.javaai.assistant.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Calls the OpenAI Chat Completions API to provide Java-focused AI assistance.
 *
 * <h3>Key behaviour</h3>
 * <ul>
 *   <li>The system prompt restricts the assistant to Java-only topics.</li>
 *   <li>Chat history is kept (up to {@value #MAX_HISTORY} turns) for context.</li>
 *   <li>All network calls are made on a dedicated daemon thread pool so the
 *       Swing EDT is never blocked.</li>
 *   <li>Requires Java 11+ ({@code java.net.http.HttpClient}).</li>
 *   <li>No third-party JSON library – request/response JSON is handled with
 *       purpose-built string helpers to keep the project dependency-free.</li>
 * </ul>
 */
public class AIAssistantService {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------
    private static final String API_URL   = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL     = "gpt-3.5-turbo";
    private static final int    MAX_HISTORY = 20;   // user+assistant turns combined

    /** System prompt that restricts the assistant to Java topics. */
    private static final String SYSTEM_PROMPT =
            "You are an expert Java programming assistant embedded in a Java code editor. " +
            "You help users with:\n" +
            "  • Writing, reviewing, and refactoring Java code\n" +
            "  • Explaining Java concepts, syntax, APIs, and best practices\n" +
            "  • Diagnosing and fixing Java compilation errors\n" +
            "  • Recommending Java libraries and design patterns\n" +
            "  • Java performance, concurrency, and architecture advice\n\n" +
            "IMPORTANT: You ONLY assist with Java-related topics. " +
            "If asked about other programming languages, politely decline and offer to help " +
            "with the Java equivalent instead.\n\n" +
            "Keep responses concise and practical. Use fenced ```java code blocks when showing code.";

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    private String apiKey;
    private final HttpClient     httpClient;
    private final List<ChatMessage> history = new ArrayList<>();
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ai-assistant-thread");
                t.setDaemon(true);
                return t;
            });

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public AIAssistantService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Sends {@code userMessage} to the AI asynchronously.
     *
     * @return a future that resolves to the assistant reply (never null)
     */
    public CompletableFuture<String> sendMessageAsync(String userMessage) {
        return CompletableFuture.supplyAsync(() -> sendMessage(userMessage), executor);
    }

    /**
     * Synchronous send (blocks the calling thread). Prefer
     * {@link #sendMessageAsync} from the Swing EDT.
     */
    public String sendMessage(String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            return "⚠️  No API key configured.\n" +
                   "Enter your OpenAI API key in the toolbar field and click Set Key, " +
                   "or set the OPENAI_API_KEY environment variable before starting.";
        }
        if (userMessage == null || userMessage.isBlank()) {
            return "⚠️  Empty message – please type something.";
        }

        // Add the user turn to history before the request (for context building)
        history.add(new ChatMessage("user", userMessage));
        trimHistory();

        try {
            String requestBody = buildRequestBody();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return switch (response.statusCode()) {
                case 200 -> {
                    String content = extractContent(response.body());
                    history.add(new ChatMessage("assistant", content));
                    yield content;
                }
                case 401 -> {
                    history.remove(history.size() - 1); // roll back
                    yield "⚠️  Unauthorized – check your OpenAI API key.";
                }
                case 429 -> {
                    history.remove(history.size() - 1);
                    yield "⚠️  Rate limit exceeded – please wait a moment and try again.";
                }
                default -> {
                    history.remove(history.size() - 1);
                    yield "⚠️  API error " + response.statusCode() + ": " +
                          extractError(response.body());
                }
            };

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            history.remove(history.size() - 1);
            return "⚠️  Request interrupted.";
        } catch (Exception e) {
            history.remove(history.size() - 1);
            return "⚠️  Connection error: " + e.getMessage();
        }
    }

    /** Remove all conversation history. */
    public void clearHistory() {
        history.clear();
    }

    /** Update the API key at runtime (e.g., from the toolbar). */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    // -----------------------------------------------------------------------
    // JSON building / parsing (no external library)
    // -----------------------------------------------------------------------

    private String buildRequestBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(MODEL).append("\",");
        sb.append("\"max_tokens\":2048,");
        sb.append("\"temperature\":0.7,");
        sb.append("\"messages\":[");

        // System message
        sb.append("{\"role\":\"system\",\"content\":\"")
          .append(escapeJson(SYSTEM_PROMPT)).append("\"}");

        // Conversation history (already includes the current user turn)
        for (ChatMessage msg : history) {
            sb.append(",{\"role\":\"").append(msg.getRole())
              .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * Extracts the {@code content} field from an OpenAI Chat Completions response.
     * Format: {@code {"choices":[{"message":{"content":"…"}}]}}
     */
    private String extractContent(String json) {
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return "Could not parse API response. Raw: " + json;
        }
        start += marker.length();

        StringBuilder result = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char esc = json.charAt(i + 1);
                switch (esc) {
                    case '"'  -> result.append('"');
                    case '\\' -> result.append('\\');
                    case 'n'  -> result.append('\n');
                    case 'r'  -> result.append('\r');
                    case 't'  -> result.append('\t');
                    case 'b'  -> result.append('\b');
                    case 'f'  -> result.append('\f');
                    case 'u'  -> {
                        if (i + 5 < json.length()) {
                            try {
                                result.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                                i += 6;
                                continue;
                            } catch (NumberFormatException ignored) {
                                result.append(esc);
                            }
                        }
                    }
                    default -> { result.append('\\'); result.append(esc); }
                }
                i += 2;
            } else if (c == '"') {
                break; // end of content string
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    /** Extracts the {@code message} field from an OpenAI error response. */
    private String extractError(String json) {
        String marker = "\"message\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return json;
        start += marker.length();
        int end = json.indexOf('"', start);
        return (end < 0) ? json.substring(start) : json.substring(start, end);
    }

    /** Escapes a Java string for embedding inside a JSON string literal. */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    private void trimHistory() {
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }
}
