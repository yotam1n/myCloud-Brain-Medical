# AI Thinking / Reply Content Separation Design

> **Date:** 2026-06-29
> **Status:** Approved
> **Goal:** Separate AI model reasoning/thinking content (`reasoning_content`) from final reply content (`content`) across the entire AI pipeline, display thinking content in a collapsible UI section, and persist it to the database.

---

## 1. Problem

DeepSeek-R1 and similar reasoning models emit two distinct fields in their SSE streaming response:

| Field | Meaning | When |
|---|---|---|
| `reasoning_content` | Internal chain-of-thought (thinking) | First N chunks |
| `content` | Final reply intended for the user | Subsequent chunks |

The current implementation in `AbstractOpenAICompatibleProvider` treats `reasoning_content` as a **fallback** for `content` — if `content` is blank, it uses `reasoning_content` instead. Both flow through a single `chunkConsumer` callback and a single `chunk` SSE event. The frontend has no way to distinguish thinking from reply, so everything gets displayed as plain reply text to the user.

### Affected components:
- Doctor AI Chat (ChatService → ChatController → chat.ts store → AiChatMessage.vue)
- Patient Conversational Triage (TriageStreamController → ChatService → ConversationalTriageSection.vue)
- AI Workflows: Medical Record generation, Diagnosis suggestions (AiStreamSessionService → ai-stream.ts store)

---

## 2. Solution Overview

Introduce a **dual-channel** architecture:

```
DeepSeek API
  ├─ reasoning_content ──► thinkingConsumer ──► SSE "thinking" event ──► thinkingBuffer ──► Collapsible UI
  └─ content          ──► chunkConsumer    ──► SSE "chunk" event     ──► streamBuffer   ──► Normal reply UI
```

Key principle: **`reasoning_content` is no longer a fallback for `content`.** It is an independent content type with its own callback, SSE event, frontend buffer, and database column.

---

## 3. Detailed Changes

### 3.1 Backend: Provider Layer

**File:** `backend/src/main/java/com/cloudbrain/application/ai/AIProvider.java`

Add a default method for backward compatibility:

```java
default AIChatResponse chatStream(AIChatRequest request, Consumer<String> chunkConsumer) {
    return chatStream(request, chunkConsumer, null);
}

AIChatResponse chatStream(AIChatRequest request,
                          Consumer<String> chunkConsumer,
                          Consumer<String> thinkingConsumer);
```

**File:** `backend/src/main/java/com/cloudbrain/application/ai/AbstractOpenAICompatibleProvider.java`

Split `extractDeltaText()` into two methods:

```java
// Extracts only content (reply) — no fallback to reasoning_content
private String extractReplyDelta(JsonNode root) {
    JsonNode choices = root.path("choices");
    if (choices.isArray() && !choices.isEmpty()) {
        JsonNode delta = choices.get(0).path("delta");
        return extractContent(delta.path("content"));
    }
    return "";
}

// Extracts only reasoning_content (thinking)
private String extractThinkingDelta(JsonNode root) {
    JsonNode choices = root.path("choices");
    if (choices.isArray() && !choices.isEmpty()) {
        JsonNode delta = choices.get(0).path("delta");
        return extractContent(delta.path("reasoning_content"));
    }
    return "";
}
```

In `parseStreamResponse()`, push extracted text to the appropriate consumer:

```java
String replyDelta = extractReplyDelta(root);
if (!replyDelta.isBlank()) {
    builder.append(replyDelta);
    if (chunkConsumer != null) {
        chunkConsumer.accept(replyDelta);
    }
}
String thinkingDelta = extractThinkingDelta(root);
if (!thinkingDelta.isBlank() && thinkingConsumer != null) {
    thinkingConsumer.accept(thinkingDelta);
}
```

Also update the non-streaming `extractText()` to stop using `reasoning_content` as fallback:

```java
private String extractText(JsonNode root) {
    // Only extract content, not reasoning_content
    JsonNode choices = root.path("choices");
    if (choices.isArray() && !choices.isEmpty()) {
        JsonNode message = choices.get(0).path("message");
        String content = extractContent(message.path("content"));
        if (!content.isBlank()) return content;
    }
    return "";
}
```

### 3.2 Backend: ChatService

**File:** `backend/src/main/java/com/cloudbrain/application/chat/ChatService.java`

In `streamChat()`, add a thinking content accumulator and pass a `thinkingConsumer` to the provider:

```java
StringBuilder fullThinking = new StringBuilder();

provider.chatStream(request,
    chunk -> {
        fullResponse.append(chunk);
        sendChunk(emitter, "chunk", chunk);
    },
    thinking -> {
        fullThinking.append(thinking);
        sendChunk(emitter, "thinking", thinking);
    }
);
```

Update `sendChunk()` to accept an event name parameter:

```java
private void sendChunk(SseEmitter emitter, String eventName, String chunk) {
    try {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(Map.of("content", chunk)));
    } catch (IOException e) {
        throw new RuntimeException("SSE send failed", e);
    }
}
```

In the `done` event, include `thinkingContent`:

```java
emitter.send(SseEmitter.event()
    .name("done")
    .data(Map.of(
        "messageId", assistantMsg.getId(),
        "thinkingContent", fullThinking.toString(),
        "meta", meta
    )));
```

Save the thinking content to the entity:

```java
ChatMessageEntity assistantMsg = new ChatMessageEntity(session, "ASSISTANT", responseText);
assistantMsg.setThinkingContent(fullThinking.toString());
assistantMsg.setAiMeta(metaJson);
```

### 3.3 Backend: AiStreamSessionService

**File:** `backend/src/main/java/com/cloudbrain/application/workflow/AiStreamSessionService.java`

In the `stream()` method, add a `thinkingConsumer` that sends `thinking` SSE events:

```java
AtomicBoolean streamedThinking = new AtomicBoolean(false);
java.util.function.Consumer<String> thinkingConsumer = thinking -> {
    try {
        streamedThinking.set(true);
        send(emitter, "thinking", Map.of("text", thinking));
    } catch (IOException exception) {
        throw new RuntimeException(exception);
    }
};
```

Pass the thinking consumer when invoking the workflow:

```java
Object result = "MEDICAL_RECORD".equals(session.taskType())
    ? workflowService.generateMedicalRecord(..., chunkConsumer, thinkingConsumer)
    : workflowService.suggestDiagnosis(..., chunkConsumer, thinkingConsumer);
```

### 3.4 Backend: WorkflowService

**File:** `backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java`

Update `generateMedicalRecord()` and `suggestDiagnosis()` to accept and pass through a `thinkingConsumer` parameter. The thinking consumer is passed to `AIInvocationService.chat()`.

### 3.5 Backend: AIInvocationService

**File:** `backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java`

Update the `chat()` method to accept an optional `thinkingConsumer`, and pass it to `provider.chatStream()`:

```java
public AIModels.AIExecutionOutcome<String> chat(String taskType,
                                                String deptCode,
                                                Map<String, String> variables,
                                                List<AIModels.AIContentPart> attachments,
                                                String fallbackText,
                                                boolean stream,
                                                Consumer<String> chunkConsumer,
                                                Consumer<String> thinkingConsumer) {
    // ...
    if (stream) {
        return provider.chatStream(request, chunkConsumer, thinkingConsumer);
    }
    // non-streaming: extract thinking from raw response body
}
```

### 3.6 Backend: ChatMessageEntity

**File:** `backend/src/main/java/com/cloudbrain/entity/chat/ChatMessageEntity.java`

Add a `thinkingContent` field:

```java
@Column(name = "thinking_content", columnDefinition = "TEXT")
private String thinkingContent;

// New constructor
public ChatMessageEntity(ChatSessionEntity session, String role, String content, String thinkingContent) {
    this.session = Objects.requireNonNull(session);
    this.role = Objects.requireNonNull(role);
    this.content = content;
    this.thinkingContent = thinkingContent;
}

// Getter/Setter
public String getThinkingContent() { return thinkingContent; }
public void setThinkingContent(String thinkingContent) { this.thinkingContent = thinkingContent; }
```

### 3.7 Database Migration

**New file:** `backend/src/main/resources/db/migration/V19__add_chat_message_thinking_content.sql`

```sql
ALTER TABLE chat_message ADD COLUMN thinking_content TEXT AFTER content;
```

### 3.8 Frontend: TypeScript Types

**File:** `frontend/src/types/chat.ts`

```typescript
export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  thinkingContent?: string;
  aiMeta: string | null;
  createdAt: string;
}
```

### 3.9 Frontend: Chat Store

**File:** `frontend/src/stores/chat.ts`

- Add `thinkingBuffer` ref
- Add `thinking` SSE event listener
- Store `thinkingContent` on the assistant message
- Handle `thinkingContent` in the `done` event

```typescript
const streamBuffer = ref('');
const thinkingBuffer = ref('');

// In sendMessage():
eventSource.addEventListener('thinking', (event) => {
    const data = JSON.parse(event.data);
    thinkingBuffer.value += data.content;
    assistantMsg.thinkingContent = thinkingBuffer.value;
});

eventSource.addEventListener('done', (event) => {
    const data = JSON.parse(event.data);
    assistantMsg.id = data.messageId;
    assistantMsg.content = streamBuffer.value;
    assistantMsg.thinkingContent = data.thinkingContent || thinkingBuffer.value;
    assistantMsg.aiMeta = JSON.stringify(data.meta as ChatMeta);
    // ...
});
```

### 3.10 Frontend: AI Stream Store

**File:** `frontend/src/stores/ai-stream.ts`

Add `thinkingText` ref and `thinking` event listener in `subscribeToEvents()`:

```typescript
const thinkingText = ref('');

// In subscribeToEvents():
source.addEventListener('thinking', (event) => {
    if (activeRunId !== runId || cancelRequested) return;
    const payload = parseSsePayload<{ text?: string }>(event.data);
    thinkingText.value += payload?.text ?? event.data;
});
```

### 3.11 Frontend: AiChatMessage Component

**File:** `frontend/src/components/chat/AiChatMessage.vue`

Add a collapsible thinking section above the reply content (for ASSISTANT messages only):

```
┌─────────────────────────────────────────┐
│ 💭 AI 思考过程                    ▸     │  ← Collapsed by default
├─────────────────────────────────────────┤
│ 根据您的症状，建议前往呼吸内科就诊。      │  ← Reply (always visible)
│                        │ model │ 120ms  │
└─────────────────────────────────────────┘
```

Expanded state:

```
┌─────────────────────────────────────────┐
│ 💭 AI 思考过程                    ▾     │
│ ┌─────────────────────────────────────┐ │
│ │ 让我分析患者的主诉：发热、咳嗽...    │ │  ← Thinking content
│ │ 需要考虑呼吸系统疾病的可能性...     │ │    (styled differently)
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ 根据您的症状，建议前往呼吸内科就诊。      │
│                        │ model │ 120ms  │
└─────────────────────────────────────────┘
```

Implementation details:
- Use a local `ref` for `thinkingExpanded` (default `false`)
- Thinking content rendered with `simpleMarkdown()` like reply content
- Thinking area uses a muted/italic style to visually distinguish from the reply
- Include `ChevronDown`/`ChevronUp` icons from lucide-vue-next for the toggle

### 3.12 Frontend: ConversationalTriageSection

**File:** `frontend/src/views/patient/panels/ConversationalTriageSection.vue`

- Add thinking content accumulation similar to chat store
- Filter thinking content out before `parseResultMarker()` — only reply content should be parsed for `[TRIAGE_RESULT]`
- Add a collapsible thinking section in ASSISTANT messages (same pattern as AiChatMessage)

### 3.13 Frontend: DoctorConsultationPanel (Workflow Display)

**File:** `frontend/src/views/doctor/panels/DoctorConsultationPanel.vue`

- Read `thinkingText` from `useAiStreamStore`
- Display thinking text in a collapsible section above the streaming workflow output

---

## 4. Backward Compatibility

| Scenario | Behavior |
|---|---|
| Models without `reasoning_content` (e.g., GPT-4, Doubao, Tongyi) | `thinkingConsumer` never invoked; no `thinking` SSE events; `thinkingContent` is empty/null; UI shows no thinking section |
| Old frontend connecting to new backend | Ignores unknown `thinking` SSE events; works exactly as before |
| Non-streaming API calls | `extractText()` only returns `content`, no thinking extraction needed |
| Existing chat_message rows | `thinking_content` column is NULL for old rows; frontend treats undefined/null as "no thinking content" |

---

## 5. Non-Streaming Thinking Content

For non-streaming `chat()` calls (used in triage fallback and some workflows), the model may also return `reasoning_content` in the `message` object. The non-streaming `extractText()` is updated to **not** use `reasoning_content` as fallback. If we later want thinking content from non-streaming calls, we can add it — but for now, the primary use case is streaming.

---

## 6. Files Changed (Summary)

| File | Change |
|---|---|
| `AIProvider.java` | Add `chatStream()` overload with `thinkingConsumer` |
| `AbstractOpenAICompatibleProvider.java` | Split extract methods, push to dual consumers |
| `ChatService.java` | Dual SSE events (`thinking` + `chunk`), save thinking to entity |
| `AiStreamSessionService.java` | Add `thinking` SSE events |
| `WorkflowService.java` | Pass `thinkingConsumer` through |
| `AIInvocationService.java` | Accept `thinkingConsumer` parameter |
| `ChatMessageEntity.java` | Add `thinkingContent` field |
| `V19__add_chat_message_thinking_content.sql` | New migration |
| `frontend/src/types/chat.ts` | Add `thinkingContent` field |
| `frontend/src/stores/chat.ts` | Add `thinkingBuffer`, `thinking` event listener |
| `frontend/src/stores/ai-stream.ts` | Add `thinkingText`, `thinking` event listener |
| `frontend/src/components/chat/AiChatMessage.vue` | Collapsible thinking section |
| `frontend/src/views/patient/panels/ConversationalTriageSection.vue` | Collapsible thinking section + filter from result parsing |
| `frontend/src/views/doctor/panels/DoctorConsultationPanel.vue` | Display thinking text |

---

## 7. Self-Review Checklist

- [x] No TBD, TODO, or placeholder sections
- [x] All file paths are exact
- [x] Architecture matches feature description
- [x] Scope is focused — single coherent change
- [x] Every requirement has a corresponding change description
- [x] Backward compatibility addressed
- [x] Database migration included
