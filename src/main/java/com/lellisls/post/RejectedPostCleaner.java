package com.lellisls.post;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class RejectedPostCleaner {

    @Scheduled(every = "1h")
    @Transactional
    void deleteOldRejectedPosts() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        Post.delete("status = ?1 and timestamp < ?2", Post.Status.REJECTED, cutoff);
    }
}
