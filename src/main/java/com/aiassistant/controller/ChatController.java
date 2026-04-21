package com.aiassistant.controller;

import com.aiassistant.command.CommandResult;
import com.aiassistant.conversation.Message;
import com.aiassistant.service.AssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final AssistantService assistantService;

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String message = request == null ? "" : request.message();
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Message must not be blank."));
        }
        return ResponseEntity.ok(new ChatResponse(assistantService.chat(message.trim())));
    }

    @PostMapping("/command")
    public ResponseEntity<CommandResponse> command(@RequestBody CommandRequest request) {
        String command = request == null ? "" : request.command();
        if (command == null || command.isBlank()) {
            return ResponseEntity.badRequest().body(new CommandResponse(false, false, "Command must not be blank."));
        }
        CommandResult result = assistantService.processCommand(command.trim());
        return ResponseEntity.ok(new CommandResponse(result.isHandled(), result.isShouldExit(), result.getMessage()));
    }

    @GetMapping("/history")
    public List<Message> history() {
        return assistantService.getHistory();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    public record ChatRequest(String message) {
    }

    public record ChatResponse(String response) {
    }

    public record CommandRequest(String command) {
    }

    public record CommandResponse(boolean handled, boolean shouldExit, String response) {
    }
}
