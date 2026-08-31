package com.gst_reconsilation.presence.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory (single-instance, no Redis — matches this app's current deployment) tracker of which
 * users have at least one live STOMP session open. A user can have multiple tabs/devices connected
 * at once, so ONLINE/OFFLINE only flips on the first connect / last disconnect for that user, not
 * on every individual session event.
 */
@Component
public class PresenceTracker {

    private final ConcurrentHashMap<String, Integer> sessionToUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<String>> userToSessions = new ConcurrentHashMap<>();

    /** @return true iff this is the user's first open session (caller should broadcast ONLINE). */
    public boolean onConnect(String sessionId, Integer userId) {
        sessionToUser.put(sessionId, userId);
        Set<String> sessions = userToSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        sessions.add(sessionId);
        return sessions.size() == 1;
    }

    /** @return true iff the user has no more open sessions (caller should broadcast OFFLINE), or null if the session was unknown. */
    public Boolean onDisconnect(String sessionId) {
        Integer userId = sessionToUser.remove(sessionId);
        if (userId == null) return null;
        Set<String> sessions = userToSessions.get(userId);
        if (sessions == null) return true;
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            userToSessions.remove(userId);
            return true;
        }
        return false;
    }

    public Integer userIdForSession(String sessionId) {
        return sessionToUser.get(sessionId);
    }

    public Set<Integer> onlineUserIds() {
        return Set.copyOf(userToSessions.keySet());
    }

    public boolean isOnline(Integer userId) {
        return userToSessions.containsKey(userId);
    }
}
