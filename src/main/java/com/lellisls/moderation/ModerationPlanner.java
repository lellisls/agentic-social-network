package com.lellisls.moderation;

import com.lellisls.moderation.voters.HateSpeechVoter;
import com.lellisls.moderation.voters.IllegalContentVoter;
import com.lellisls.moderation.voters.SpamVoter;
import com.lellisls.moderation.voters.ToxicityVoter;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.service.V;

public interface ModerationPlanner {
    @PlannerAgent(name = "moderationPlanner",
            description = "Agentic moderation of votes",
            outputKey = "moderationResult",
            subAgents = {
                HateSpeechVoter.class,
                IllegalContentVoter.class,
                SpamVoter.class,
                ToxicityVoter.class
            }
    )
    EvaluationResult vote(@V("author") String author, @V("content") String content);

    @PlannerSupplier
    static Planner planner() {
        return new VotingPlanner(VotingStrategy.highest());
    }
}
