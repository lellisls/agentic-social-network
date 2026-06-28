package com.lellisls.post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ServerEndpoint("/posts/events")
@ApplicationScoped
public class PostWebSocket {

    private final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    public void onPostEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostEvent event) {
        broadcast("refresh");
    }

    private void broadcast(String message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            }
        }
    }
}
