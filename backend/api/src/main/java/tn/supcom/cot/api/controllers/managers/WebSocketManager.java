package tn.supcom.cot.api.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.Session;
import tn.supcom.cot.api.boundaries.Dtos.Envelope;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Garde les sessions WebSocket par vue ("driver" / "admin") et leur diffuse les changements. */
@ApplicationScoped
public class WebSocketManager {
    private static final Logger LOG = Logger.getLogger(WebSocketManager.class.getName());
    public static final String DRIVER = "driver";
    public static final String ADMIN = "admin";

    private final Map<String, Set<Session>> sessions = new ConcurrentHashMap<>();
    private final Jsonb jsonb = JsonbBuilder.create();

    public void register(String view, Session session) {
        sessions.computeIfAbsent(view, v -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String view, Session session) {
        var set = sessions.get(view);
        if (set != null) set.remove(session);
    }

    /** Envoie {type, data, ts} aux vues indiquées. */
    public void broadcast(String type, Object data, String... views) {
        var json = jsonb.toJson(Envelope.of(type, data));
        for (var view : views) {
            for (var s : sessions.getOrDefault(view, Set.of())) {
                if (!s.isOpen()) continue;
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "WebSocket : envoi impossible, session ignorée", e);
                }
            }
        }
    }
}
