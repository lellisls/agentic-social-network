package com.lellisls.moderation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ModerationPanel {

    private static final double AUTO_APPROVE_THRESHOLD = 25.0;
    private static final double AUTO_REJECT_THRESHOLD  = 90.0;

    @Inject
    ModerationPlanner planner;

    public Verdict evaluate(String author, String content) {
        EvaluationResult result = planner.vote(author, content);
        double score = result.score();
        Decision decision = score < AUTO_APPROVE_THRESHOLD ? Decision.AUTO_APPROVE
                          : score > AUTO_REJECT_THRESHOLD  ? Decision.AUTO_REJECT
                          : Decision.NEEDS_HUMAN;
        return new Verdict(decision, score, result.reason());
    }

    public enum Decision { AUTO_APPROVE, NEEDS_HUMAN, AUTO_REJECT }
    public record Verdict(Decision decision, double score, String reason) {
        public boolean approved() { return decision != Decision.AUTO_REJECT; }
    }
}
