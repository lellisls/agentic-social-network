package com.lellisls.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lellisls.post.Post;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.Map;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.*;

@ApplicationScoped
public class PostModerationWorkflow extends Flow {

    @Inject
    ModerationPanel moderationPanel;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow("post-moderation")
                .tasks(
                        // Step 1: AI moderation
                        function("aiModerate", this::moderate, PostContext.class, ModerationContext.class),

                        // Step 2a: auto-approve branch (score < 25)
                        switchWhenOrElse(
                                (ModerationContext ctx) -> ctx.decision() == ModerationPanel.Decision.AUTO_APPROVE,
                                "markPublished", "checkAutoReject", ModerationContext.class),

                        // Step 2a-result: published → end
                        consume("markPublished", this::markPublished, ModerationContext.class)
                                .then(FlowDirectiveEnum.END),

                        // Step 2b: auto-reject branch (score > 90)
                        switchWhenOrElse("checkAutoReject",
                                (ModerationContext ctx) -> ctx.decision() == ModerationPanel.Decision.AUTO_REJECT,
                                "markRejected", "markPendingHuman", ModerationContext.class),

                        // Step 2b-result: rejected → end
                        consume("markRejected", this::markRejected, ModerationContext.class)
                                .then(FlowDirectiveEnum.END),

                        // Step 3: 25-90 — wait for human
                        consume("markPendingHuman", this::markPendingHuman, ModerationContext.class),

                        // Step 4: pause until human review event
                        listen("waitHumanReview",
                                toOne(consumed("post.human.review.done")
                                        .correlate("postId", c -> c
                                                .from(".data.postId | tostring")
                                                .expect(".postId | tostring"))))
                                .outputAs(this::toHumanReviewDecision, Collection.class),

                        // Step 5: apply human decision
                        consume("applyHumanDecision", this::applyHumanDecision, HumanReviewDecision.class)
                )
                .build();
    }

    public record PostContext(long postId, String author, String content) {}
    public record ModerationContext(long postId, ModerationPanel.Decision decision, double score, String reason) {}
    public record HumanReviewDecision(long postId, boolean approved) {}

    ModerationContext moderate(PostContext ctx) {
        ModerationPanel.Verdict verdict = moderationPanel.evaluate(ctx.author(), ctx.content());
        return new ModerationContext(ctx.postId(), verdict.decision(), verdict.score(), verdict.reason());
    }

    HumanReviewDecision toHumanReviewDecision(Collection<?> events) {
        Object event = events.iterator().next();
        return toHumanReviewDecision(event);
    }

    private HumanReviewDecision toHumanReviewDecision(Object event) {
        if (event instanceof CloudEvent cloudEvent) {
            CloudEventData data = cloudEvent.getData();
            if (data == null) {
                throw new IllegalArgumentException("Human review event has no data");
            }
            try {
                return objectMapper.readValue(data.toBytes(), HumanReviewDecision.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid human review event data", e);
            }
        }
        if (event instanceof Map<?, ?> eventMap) {
            Object data = eventMap.get("data");
            if (data == null) {
                throw new IllegalArgumentException("Human review event has no data");
            }
            return objectMapper.convertValue(data, HumanReviewDecision.class);
        }
        return objectMapper.convertValue(event, HumanReviewDecision.class);
    }

    @Transactional
    void markPublished(ModerationContext ctx) {
        Post post = Post.findById(ctx.postId());
        if (post != null) {
            post.status = Post.Status.PUBLISHED;
            post.aiApproved = true;
            post.aiScore = ctx.score();
            post.aiReason = ctx.reason();
        }
    }

    @Transactional
    void markRejected(ModerationContext ctx) {
        Post post = Post.findById(ctx.postId());
        if (post != null) {
            post.status = Post.Status.REJECTED;
            post.aiApproved = false;
            post.aiScore = ctx.score();
            post.aiReason = ctx.reason();
        }
    }

    @Transactional
    void markPendingHuman(ModerationContext ctx) {
        Post post = Post.findById(ctx.postId());
        if (post != null) {
            post.status = Post.Status.PENDING_HUMAN;
            post.aiApproved = null;
            post.aiScore = ctx.score();
            post.aiReason = ctx.reason();
        }
    }

    @Transactional
    void applyHumanDecision(HumanReviewDecision decision) {
        updateStatus(decision.postId(), decision.approved() ? Post.Status.PUBLISHED : Post.Status.REJECTED);
    }

    private void updateStatus(long postId, Post.Status status) {
        Post post = Post.findById(postId);
        if (post != null) {
            post.status = status;
        }
    }
}
