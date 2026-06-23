package com.lellisls;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("""
        You are a content moderator for a social network.
        Evaluate whether a post is appropriate (no hate speech, spam, or illegal content).
        """)
public interface PostModerationAgent {

    record PostInput(String author, String content) {}
    record ModerationResult(boolean approved, String reason) {}

    @UserMessage("Author: {{input.author}}\nContent: {{input.content}}")
    ModerationResult moderate(@MemoryId String memoryId, @V("input") PostInput input);
}
