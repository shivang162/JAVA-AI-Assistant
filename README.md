# Java AI Assistant

A production-ready Java assistant with NLP intent detection, context-aware conversation management, command processing, and a Spring Boot web UI.

## Project Structure

```
src/main/java/com/aiassistant/
├── Main.java
├── controller/
├── command/
├── config/
├── conversation/
├── nlp/
├── response/
├── service/
└── util/

src/main/resources/
├── static/
│   ├── index.html
│   ├── style.css
│   └── script.js
├── application.properties
├── ai.properties
├── commands.properties
├── intents.properties
├── logging.properties
└── schema.sql
```

## Build & Run

```bash
mvn clean package && java -jar target/ai-assistant-1.0.jar
```

## Features

- NLP engine for tokenization and intent detection
- Conversation manager with bounded context window
- Command system (`/help`, `/history`, `/clear`, `/exit`)
- Response generator with temperature-driven template variation
- Spring Boot REST API (`/api/chat`, `/api/command`, `/api/history`, `/api/health`)
- Multi-device collaboration APIs:
  - `POST /api/device/register`
  - `GET /api/devices`
- Browser P2P collaboration features:
  - Real-time two-device video call via PeerJS + WebRTC
  - Incoming-call accept/reject popup
  - Camera/mic toggle and screen sharing
  - True PeerJS data-channel chat between devices
  - Share Code and Share Output buttons in chat
  - Unread chat badge on the Chat tab
- Browser UI served from `http://localhost:8080`
- Properties-based configuration and logging setup
- Production packaging as executable Spring Boot JAR

## Configuration

Edit `src/main/resources/ai.properties`:

```properties
ai.temperature=0.7
ai.maxTokens=256
ai.contextWindow=20
```

SQLite + JPA settings are in `src/main/resources/application.properties`.

## Example Usage

```bash
# open UI
http://localhost:8080

# REST examples
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}'

curl -X POST http://localhost:8080/api/command \
  -H "Content-Type: application/json" \
  -d '{"command":"/help"}'
```

## Testing

```bash
mvn test
```
