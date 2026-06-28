package com.lellisls.post;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

@QuarkusTest
public class PostResourceTest {

    @BeforeEach
    public void cleanDatabase() {
        QuarkusTransaction.run(() -> {
            Post.deleteAll();
        });
    }

    @Test
    public void testDeleteAutoRejectedPostImmediately() {
        // Create an auto-rejected post (status = REJECTED, aiApproved = false)
        Post post = QuarkusTransaction.requiringNew().call(() -> {
            Post p = new Post();
            p.author = "Alice";
            p.content = "Bad content";
            p.timestamp = Instant.now();
            p.status = Post.Status.REJECTED;
            p.aiApproved = false;
            p.persistAndFlush();
            return p;
        });

        // Delete should succeed immediately (204 No Content)
        RestAssured.given()
                .when()
                .delete("/posts/" + post.id)
                .then()
                .statusCode(204);
    }

    @Test
    public void testDeleteHumanRejectedPostUnder24HoursFails() {
        // Create a human-rejected post (status = REJECTED, aiApproved = null/not false)
        Post post = QuarkusTransaction.requiringNew().call(() -> {
            Post p = new Post();
            p.author = "Bob";
            p.content = "Normal content";
            p.timestamp = Instant.now();
            p.status = Post.Status.REJECTED;
            p.aiApproved = null;
            p.persistAndFlush();
            return p;
        });

        // Delete should fail with 403 Forbidden
        RestAssured.given()
                .when()
                .delete("/posts/" + post.id)
                .then()
                .statusCode(403);
    }
}

