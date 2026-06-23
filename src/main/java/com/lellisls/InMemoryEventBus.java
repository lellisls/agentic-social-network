package com.lellisls;

import io.serverlessworkflow.impl.events.InMemoryEvents;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-process event bus: registers as both EventConsumer and EventPublisher,
 * allowing the workflow engine to subscribe and the REST layer to publish events.
 */
@ApplicationScoped
public class InMemoryEventBus extends InMemoryEvents {
}
