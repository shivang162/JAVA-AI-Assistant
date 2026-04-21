# ☕ Java AI Assistant

A Java Swing desktop application that combines a **Java code editor** with an
**AI-powered chat assistant** – both tightly focused on the Java language.

---

## Features

| Feature | Details |
|---|---|
| **Java Code Editor** | Syntax-highlighted `JTextPane` (keywords, strings, comments, annotations, numbers) with line numbers |
| **Compile** | One-click compilation via `javax.tools.JavaCompiler` – errors displayed inline; code is never executed |
| **AI Chat** | OpenAI GPT chat assistant constrained to Java topics only |
| **Ask AI** | Send selected (or all) editor code directly to the AI for review / explanation |
| **Dark theme** | VS Code–inspired dark palette throughout |

---

## Requirements

| Requirement | Version |
|---|---|
| JDK | 11 or later (**JDK**, not just JRE – required for `javax.tools`) |
| Maven | 3.6+ |
| OpenAI API key | Required for the AI assistant |

---

## Quick Start

### 1 – Clone

```bash
git clone https://github.com/shivang162/JAVA-AI-Assistant.git
cd JAVA-AI-Assistant
```

### 2 – Set your OpenAI API key

```bash
# Linux / macOS
export OPENAI_API_KEY=sk-...

# Windows (Command Prompt)
set OPENAI_API_KEY=sk-...

# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-..."
```

Alternatively, enter the key at runtime in the toolbar **API Key** field and click **Set Key**.

### 3 – Build

```bash
mvn clean package -q
```

This produces `target/java-ai-assistant.jar`.

### 4 – Run

```bash
java -jar target/java-ai-assistant.jar
```

---

## Usage

### Code Editor (left panel)

* Write Java code in the editor.
* Press **Ctrl+B** or click **▶ Compile** to compile.
* Compilation output (errors / success) appears in the **Compiler Output** panel at the bottom.
* Click **🤖 Ask AI** to send the selected code (or all code) to the chat assistant for review.

### AI Chat (right panel)

* Type a Java question in the input box and press **Ctrl+Enter** (or click **Send ▶**).
* The assistant is restricted to Java topics only.
* Click **Clear History** to start a new conversation.
* Code blocks in AI responses are rendered in a monospaced code style.

### Keyboard shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+B` | Compile the Java code |
| `Ctrl+Enter` | Send chat message |
| `F5` | Toggle compiler output panel |
| `Ctrl+Q` (macOS: `⌘Q`) | Exit |

---

## Project Structure

```
src/main/java/com/javaai/assistant/
├── Main.java                          Entry point
├── ui/
│   ├── MainFrame.java                 Main JFrame, toolbar, menu bar
│   ├── CodeEditorPanel.java           Syntax-highlighted editor + compile button
│   ├── ChatPanel.java                 AI chat history + input
│   └── LineNumberComponent.java       Gutter line-number renderer
├── compiler/
│   ├── JavaCompilerService.java       javax.tools compilation wrapper
│   └── CompilationResult.java        Compilation result POJO
├── ai/
│   ├── AIAssistantService.java        OpenAI API client (Java 11 HttpClient)
│   └── ChatMessage.java              Chat turn POJO
└── editor/
    └── JavaSyntaxHighlighter.java     State-machine Java syntax highlighter
```

---

## Environment Variables

| Variable | Purpose |
|---|---|
| `OPENAI_API_KEY` | OpenAI API key used for the AI assistant |

---

## Security Notes

* Java code is **compiled only** – it is never executed, so there is no risk of running arbitrary code.
* The AI assistant prompt is hard-coded to Java topics; non-Java requests are declined.
* The API key is held in memory only and is never persisted to disk.
