package com.lellisls.moderation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ModerationPanel {

    private static final double REJECTION_THRESHOLD = 60.0;

    @Inject
    ModerationPlanner planner;

    public Verdict evaluate(String author, String content) {
        EvaluationResult result = planner.vote(author, content);
        boolean approved = result.score() < REJECTION_THRESHOLD;
        return new Verdict(approved, result.reason());
    }

    public record Verdict(boolean approved, String reason) {}
}
