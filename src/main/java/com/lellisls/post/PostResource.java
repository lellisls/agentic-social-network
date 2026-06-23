package com.lellisls.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lellisls.event.InMemoryEventBus;
import com.lellisls.moderation.PostModerationWorkflow;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    @Inject
    PostModerationWorkflow moderationWorkflow;

    @Inject
    InMemoryEventBus eventBus;

    @Inject
    PostService postService;

    @Inject
    ObjectMapper objectMapper;

    @GET
    public List<Post> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return Post.find("status = ?1 ORDER BY timestamp DESC", Post.Status.PUBLISHED)
                .page(page, size).list();
    }

    @GET
    @Path("/pending")
    public List<Post> pending() {
        return Post.find("status = ?1 ORDER BY timestamp DESC", Post.Status.PENDING_HUMAN).list();
    }
    public record CreatePostRequest(String author, String content) {}

    @POST
    @Blocking
    public Response create(CreatePostRequest req) {
        if (req.author() == null || req.author().isBlank() ||
            req.content() == null || req.content().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        // Persist post first (temporary workflow ID, updated after)
        Post post = postService.createPost(req.author(), req.content());
        // Create workflow instance with the real postId
        var workflowInstance = moderationWorkflow.instance(
                Map.of("postId", post.id, "author", post.author, "content", post.content));
        // Store the workflow instance ID on the post
        postService.setWorkflowInstanceId(post.id, workflowInstance.id());
        // Start workflow asynchronously
        workflowInstance.start();
        return Response.status(Response.Status.ACCEPTED).entity(post).build();
    }

    public record HumanDecision(boolean approved) {}

    @POST
    @Path("/{id}/moderate")
    @Blocking
    public Uni<Response> humanModerate(@PathParam("id") long postId, HumanDecision decision) {
        Post post = Post.findById(postId);
        if (post == null) {
            return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
        }
        if (post.status != Post.Status.PENDING_HUMAN || post.workflowInstanceId == null) {
            return Uni.createFrom().item(Response.status(Response.Status.CONFLICT).build());
        }
        try {
            JsonCloudEventData payload = JsonCloudEventData.wrap(objectMapper.valueToTree(
                    Map.of("postId", postId, "approved", decision.approved())));
            CloudEvent ce = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create("/posts/" + postId + "/moderate"))
                    .withType("post.human.review.done")
                    .withDataContentType(MediaType.APPLICATION_JSON)
                    .withData(payload)
                    .build();
            return Uni.createFrom().completionStage(eventBus.publish(ce))
                    .replaceWith(Response.ok().build());
        } catch (Exception e) {
            return Uni.createFrom().item(Response.serverError().build());
        }
    }
}
