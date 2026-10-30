package com.emb.server.controller;

import com.emb.server.dto.ApiErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * 官方标准接口控制器: /v1/chat/completions
 */
@RestController
@RequestMapping("/v1")
public class ChatCompletionController {

    private final ChatClient chatClient;
    private final String defaultModel;

    public ChatCompletionController(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.openai.chat.options.model:deepseek-chat}") String defaultModel
    ) {
        this.chatClient = chatClientBuilder.build();
        this.defaultModel = defaultModel;
    }

    /**
     * 官方标准聊天补全接口: POST /v1/chat/completions
     */
    @PostMapping(value = "/chat/completions")
    public ResponseEntity<?> chatCompletions(@RequestBody ChatCompletionRequest request) {
        String chatId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String model = (request.model() != null && !request.model().isBlank()) ? request.model() : defaultModel;
        String userMessage = request.extractUserMessage();
        String systemPrompt = request.extractSystemPrompt();

        var promptSpec = chatClient.prompt().user(userMessage);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            promptSpec.system(systemPrompt);
        }

        // 流式响应 (stream = true)，采用 Spring 官方 ServerSentEvent 规范，由 Spring 容器自动完成 JSON 序列化
        if (Boolean.TRUE.equals(request.stream())) {
            Flux<ServerSentEvent<Object>> sseFlux = promptSpec.stream()
                    .content()
                    .map(chunk -> ServerSentEvent.<Object>builder(ChatCompletionChunk.of(chatId, model, chunk)).build())
                    .concatWith(Flux.just(
                            ServerSentEvent.<Object>builder(ChatCompletionChunk.finish(chatId, model)).build(),
                            ServerSentEvent.<Object>builder("[DONE]").build()
                    ))
                    .onErrorResume(ex -> Flux.just(
                            ServerSentEvent.<Object>builder(ApiErrorResponse.serverError("流式生成异常: " + ex.getMessage())).build(),
                            ServerSentEvent.<Object>builder("[DONE]").build()
                    ));

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseFlux);
        }

        // 非流式同步响应 (stream = false)
        String reply = promptSpec.call().content();
        ChatCompletionResponse response = ChatCompletionResponse.of(chatId, model, reply);
        return ResponseEntity.ok(response);
    }

    /**
     * 官方模型列表接口: GET /v1/models
     */
    @GetMapping("/models")
    public ModelsResponse models() {
        return ModelsResponse.of(defaultModel, "deepseek-chat", "gpt-4o");
    }

    // ================== 官方标准协议 DTO ==================

    public record ChatMessage(String role, String content) {}

    public record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            Boolean stream,
            Double temperature
    ) {
        public String extractUserMessage() {
            if (messages == null || messages.isEmpty()) {
                return "你好";
            }
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage m = messages.get(i);
                if ("user".equalsIgnoreCase(m.role())) {
                    return m.content();
                }
            }
            return messages.get(messages.size() - 1).content();
        }

        public String extractSystemPrompt() {
            if (messages == null || messages.isEmpty()) {
                return null;
            }
            for (ChatMessage m : messages) {
                if ("system".equalsIgnoreCase(m.role())) {
                    return m.content();
                }
            }
            return null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionChunk(
            String id,
            String object,
            long created,
            String model,
            List<ChunkChoice> choices
    ) {
        public record ChunkChoice(int index, Delta delta, String finish_reason) {}
        public record Delta(String role, String content) {}

        public static ChatCompletionChunk of(String id, String model, String content) {
            return new ChatCompletionChunk(
                    id,
                    "chat.completion.chunk",
                    System.currentTimeMillis() / 1000,
                    model,
                    List.of(new ChunkChoice(0, new Delta(null, content), null))
            );
        }

        public static ChatCompletionChunk finish(String id, String model) {
            return new ChatCompletionChunk(
                    id,
                    "chat.completion.chunk",
                    System.currentTimeMillis() / 1000,
                    model,
                    List.of(new ChunkChoice(0, new Delta(null, null), "stop"))
            );
        }
    }

    public record ChatCompletionResponse(
            String id,
            String object,
            long created,
            String model,
            List<Choice> choices,
            Usage usage
    ) {
        public record Choice(int index, ChatMessage message, String finish_reason) {}
        public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {}

        public static ChatCompletionResponse of(String id, String model, String content) {
            return new ChatCompletionResponse(
                    id,
                    "chat.completion",
                    System.currentTimeMillis() / 1000,
                    model,
                    List.of(new Choice(0, new ChatMessage("assistant", content), "stop")),
                    new Usage(10, 20, 30)
            );
        }
    }

    public record ModelsResponse(String object, List<ModelData> data) {
        public record ModelData(String id, String object, long created, String owned_by) {}

        public static ModelsResponse of(String... models) {
            var list = java.util.Arrays.stream(models)
                    .map(m -> new ModelData(m, "model", System.currentTimeMillis() / 1000, "emb-server"))
                    .toList();
            return new ModelsResponse("list", list);
        }
    }
}
