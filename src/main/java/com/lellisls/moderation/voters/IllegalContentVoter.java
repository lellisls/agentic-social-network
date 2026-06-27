package com.lellisls.moderation.voters;

import com.lellisls.moderation.EvaluationResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Specialist voter focused exclusively on illegal content. This is the
 * highest-severity policy area and holds veto power in the panel.
 */
@RegisterAiService
@ApplicationScoped
public interface IllegalContentVoter {

    @SystemMessage("""
        You are an ILLEGAL CONTENT specialist moderating a social network.
        Judge ONLY content that is clearly illegal: sale of drugs or weapons,
        sexual content involving minors, incitement to serious violence or
        terrorism, or doxxing (publishing someone's private information).
        Ignore unrelated issues such as spam or rudeness.

        Score how strongly the post matches THIS policy violation, from 0 to 100:
        - 100 means a blatant, severe violation that should definitely be moderated.
        - 0 means no trace of this violation; the post is fine for this policy.
        Use the full range, and default to a low score when in doubt.

        Respond with a JSON object with exactly two fields:
        - "score": number from 0 to 100 (higher = stronger match with this violation).
        - "reason": string — a short explanation. Always provide a reason.
        """)
    @UserMessage("Author: {author}\nContent: {content}")
    @Agent(name="illegal-content", description = "Evaluates illegal content messages", outputKey = "illegalContentVote")
    EvaluationResult vote(@V("author") String author, @V("content") String content);
}
