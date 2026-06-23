package com.lellisls;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

@Entity
public class Post extends PanacheEntity {

    public enum Status { PENDING_AI, PENDING_HUMAN, PUBLISHED, REJECTED }

    @Column(nullable = false, updatable = false)
    public String author;

    @Column(nullable = false, updatable = false)
    public Instant timestamp;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    public String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status = Status.PENDING_AI;

    @Column
    public String workflowInstanceId;

    @Column
    public Boolean aiApproved;

    @Column(columnDefinition = "TEXT")
    public String aiReason;

    public static Post create(String author, String content) {
        Post post = new Post();
        post.author = author;
        post.content = content;
        post.timestamp = Instant.now();
        post.persist();
        return post;
    }
}
