package com.lellisls.moderation;

/**
 * A single specialist's evaluation of a post for its own policy area.
 *
 * <p>Implements {@link Comparable} by {@link #score} so a collection of
 * evaluations can be aggregated with langchain4j's
 * {@code VotingStrategy.highest()}, which selects the naturally-greatest vote —
 * here, the most concerned specialist (the highest score, i.e. the strongest
 * match with a policy violation).
 *
 * @param score  how strongly the post matches this specialist's policy
 *               violation, from 0 to 100, where 100 means a blatant violation
 *               that should be moderated and 0 means no issue at all — higher
 *               means more likely to require moderation
 * @param reason short human-readable explanation for the score (always present)
 */
public record EvaluationResult(double score, String reason)
        implements Comparable<EvaluationResult> {

    public EvaluationResult(double score, String reason)
    {
        this.score = Math.min(score, 100);
        this.reason = reason;
    }

    @Override
    public int compareTo(EvaluationResult other) {
        return Double.compare(this.score, other.score);
    }
}
