package com.lellisls;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.quarkiverse.flow.Flow;
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
    PostModerationAgent moderationAgent;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow("post-moderation")
                .tasks(
                        // Step 1: AI moderation, preserving the post id for later DB updates.
                        function("aiModerate", this::moderate, PostContext.class, ModerationContext.class),

                        // Step 2: branch on AI decision
                        switchWhenOrElse(
                                (ModerationContext ctx) -> !ctx.approved(),
                                "markRejected",
                                "markPendingHuman",
                                ModerationContext.class),

                        // Step 3a: AI rejected — update DB and end
                        consume("markRejected", this::markRejected, ModerationContext.class),

                        // Step 3b: AI approved — update DB and wait for human
                        consume("markPendingHuman", this::markPendingHuman, ModerationContext.class),

                        // Step 4: pause until human review event with the same postId arrives.
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
    public record ModerationContext(long postId, boolean approved, String reason) {}
    public record HumanReviewDecision(long postId, boolean approved) {}

    ModerationContext moderate(PostContext ctx) {
        PostModerationAgent.ModerationResult result = moderationAgent.moderate(
                Long.toString(ctx.postId()),
                new PostModerationAgent.PostInput(ctx.author(), ctx.content()));
        return new ModerationContext(ctx.postId(), result.approved(), result.reason());
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
    void markRejected(ModerationContext ctx) {
        Post post = Post.findById(ctx.postId());
        if (post != null) {
            post.status = Post.Status.REJECTED;
            post.aiApproved = ctx.approved();
            post.aiReason = ctx.reason();
        }
    }

    @Transactional
    void markPendingHuman(ModerationContext ctx) {
        Post post = Post.findById(ctx.postId());
        if (post != null) {
            post.status = Post.Status.PENDING_HUMAN;
            post.aiApproved = ctx.approved();
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
