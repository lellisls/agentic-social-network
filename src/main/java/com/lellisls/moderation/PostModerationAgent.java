package com.lellisls.moderation;

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
        A post is appropriate UNLESS it clearly contains hate speech, harassment,
        spam, or illegal content. Normal everyday posts must be approved.
        Default to approving when in doubt.

        Respond with a JSON object containing exactly two fields:
        - "approved": boolean — true if the post is appropriate, false otherwise.
        - "reason": string — a short explanation for your decision. Always provide
          a reason, whether the post is approved or rejected.
        """)
public interface PostModerationAgent {

    record PostInput(String author, String content) {}
    record ModerationResult(boolean approved, String reason) {}

    @UserMessage("Author: {{input.author}}\nContent: {{input.content}}")
    ModerationResult moderate(@MemoryId String memoryId, @V("input") PostInput input);
}
