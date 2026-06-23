package com.lellisls;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostService {

    @Transactional
    public Post createPost(String author, String content) {
        return Post.create(author, content);
    }

    @Transactional
    public void setWorkflowInstanceId(Long postId, String workflowInstanceId) {
        Post post = Post.findById(postId);
        if (post != null) {
            post.workflowInstanceId = workflowInstanceId;
        }
    }

    @Transactional
    public Post createAndLinkWorkflow(String author, String content, String workflowInstanceId) {
        Post post = createPost(author, content);
        post.workflowInstanceId = workflowInstanceId;
        return post;
    }
}
