package org.example.profiler.server;


import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArraySet;

public class ProfilerWebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // echo back message (or handle client requests)
        try {
            session.sendMessage(new TextMessage("Server received: " + message.getPayload()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void broadcast(String msg) {
        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(msg));
            } catch (Exception ignored) {}
        });
    }
}
