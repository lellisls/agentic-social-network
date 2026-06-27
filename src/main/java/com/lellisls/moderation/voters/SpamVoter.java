package com.lellisls.moderation.voters;

import com.lellisls.moderation.EvaluationResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Specialist voter focused exclusively on spam, scams, and deceptive
 * promotional content.
 */
@RegisterAiService
@ApplicationScoped
public interface SpamVoter {

    @SystemMessage("""
        You are a SPAM specialist moderating a social network.
        Judge ONLY spam, scams, phishing, deceptive advertising, suspicious
        links, and repetitive promotional content. Ignore unrelated issues
        such as hate speech or profanity.

        Score how strongly the post matches THIS policy violation, from 0 to 100:
        - 100 means a blatant, severe violation that should definitely be moderated.
        - 0 means no trace of this violation; the post is fine for this policy.
        Use the full range, and default to a low score when in doubt.

        Respond with a JSON object with exactly two fields:
        - "score": number from 0 to 100 (higher = stronger match with this violation).
        - "reason": string — a short explanation. Always provide a reason.
        """)
    @UserMessage("Author: {author}\nContent: {content}")
    @Agent(name="spam", description = "Evaluates spam messages", outputKey = "spamVote")
    EvaluationResult vote(@V("author") String author, @V("content") String content);
}
