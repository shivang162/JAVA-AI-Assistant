package com.aiassistant.controller;

import com.aiassistant.command.CommandResult;
import com.aiassistant.conversation.Message;
import com.aiassistant.service.AssistantService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
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

    @GetMapping("/server-info")
    public Map<String, String> serverInfo(HttpServletRequest request) {
        String ip = resolveLocalIpAddress();
        int port = request.getServerPort();
        return Map.of(
                "ip", ip,
                "port", String.valueOf(port),
                "url", "http://" + ip + ":" + port
        );
    }

    private String resolveLocalIpAddress() {
        String fallback = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!(addr instanceof Inet4Address)) continue;
                        String host = addr.getHostAddress();
                        if (isPrivateIpv4(host)) {
                            return host;
                        }
                        if (fallback == null) {
                            fallback = host;
                        }
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return fallback != null ? fallback : "localhost";
    }

    private boolean isPrivateIpv4(String ip) {
        return ip.startsWith("192.168.") || ip.startsWith("10.")
                || (ip.startsWith("172.") && isInRange172(ip));
    }

    private boolean isInRange172(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length < 2) return false;
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
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
