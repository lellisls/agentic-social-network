package com.lellisls;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    @GET
    public List<Post> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return Post.find("ORDER BY timestamp DESC").page(page, size).list();
    }

    public record CreatePostRequest(String author, String content) {}

    @POST
    @Transactional
    public Response create(CreatePostRequest req) {
        if (req.author() == null || req.author().isBlank() ||
            req.content() == null || req.content().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.CREATED)
                .entity(Post.create(req.author(), req.content()))
                .build();
    }
}
