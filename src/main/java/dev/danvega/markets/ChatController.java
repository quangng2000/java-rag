package dev.danvega.markets;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    @GetMapping("/chat")
    public SseEmitter chat(@RequestParam("prompt") String prompt) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                // Optional: Send an initial event to notify the client that processing has started.
                emitter.send(SseEmitter.event().name("start").data("Processing your request..."));

                // Use the user's prompt in the ChatClient call.
                String content = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                // Send the final response as an SSE message.
                emitter.send(SseEmitter.event().name("message").data(content));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }).start();
        return emitter;
    }
}
