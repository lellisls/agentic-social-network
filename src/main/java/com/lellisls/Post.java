package com.lellisls;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
public class Post extends PanacheEntity {

    @Column(nullable = false, updatable = false)
    public String author;

    @Column(nullable = false, updatable = false)
    public Instant timestamp;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    public String content;

    public static Post create(String author, String content) {
        Post post = new Post();
        post.author = author;
        post.content = content;
        post.timestamp = Instant.now();
        post.persist();
        return post;
    }
}
