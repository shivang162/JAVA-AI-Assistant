# Java AI Assistant

A production-ready Java CLI assistant with NLP intent detection, context-aware conversation management, command processing, and template-based AI-style responses.

## Project Structure

```
src/main/java/com/aiassistant/
├── Main.java
├── command/
├── config/
├── conversation/
├── nlp/
├── response/
└── util/

src/main/resources/
├── ai.properties
├── commands.properties
├── intents.properties
└── logging.properties
```

## Build & Run

```bash
mvn clean package && java -jar target/ai-assistant-1.0-jar-with-dependencies.jar
```

## Features

- NLP engine for tokenization and intent detection
- Conversation manager with bounded context window
- Command system (`/help`, `/history`, `/clear`, `/exit`)
- Response generator with temperature-driven template variation
- Properties-based configuration and logging setup
- Production packaging as executable fat JAR

## Configuration

Edit `src/main/resources/ai.properties`:

```properties
ai.temperature=0.7
ai.maxTokens=256
ai.contextWindow=20
```

## Example Usage

```text
Java AI Assistant started. Type /help for commands.
You> hello
AI> Hello! I'm your Java AI Assistant. How can I help today?
You> I get a compile error in my Java class
AI> I can help debug that Java issue. Share the relevant snippet and expected behavior.
You> /history
AI> user: hello
assistant: Hello! I'm your Java AI Assistant. How can I help today?
user: I get a compile error in my Java class
assistant: I can help debug that Java issue. Share the relevant snippet and expected behavior.
You> /exit
AI> Goodbye.
```

## Testing

```bash
mvn test
```
