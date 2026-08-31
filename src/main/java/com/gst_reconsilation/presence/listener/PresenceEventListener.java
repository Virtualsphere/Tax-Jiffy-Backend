package com.gst_reconsilation.presence.listener;

import com.gst_reconsilation.presence.dto.PresenceEvent;
import com.gst_reconsilation.presence.service.PresenceTracker;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

/**
 * Flips a user ONLINE on their first connected session and OFFLINE on their last disconnected one
 * (see PresenceTracker for the multi-session bookkeeping), persisting lastActiveAt both times so
 * "last seen" is meaningful for offline users too, and broadcasting the change to every admin
 * panel subscribed to /topic/presence.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceEventListener {

    private final PresenceTracker presenceTracker;
    private final UserDetailsRepository userDetailsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    @Transactional
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null || event.getUser() == null) return;

        Integer userId = parseUserId(event.getUser().getName());
        if (userId == null) return;

        boolean firstSession = presenceTracker.onConnect(sessionId, userId);
        if (!firstSession) return;

        userDetailsRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userDetailsRepository.save(user);
            broadcast(user, "ONLINE");
        });
    }

    @EventListener
    @Transactional
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) return;

        Integer userId = presenceTracker.userIdForSession(sessionId);
        Boolean nowOffline = presenceTracker.onDisconnect(sessionId);
        if (userId == null || !Boolean.TRUE.equals(nowOffline)) return;

        userDetailsRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userDetailsRepository.save(user);
            broadcast(user, "OFFLINE");
        });
    }

    private void broadcast(UserDetails user, String status) {
        PresenceEvent event = PresenceEvent.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companyName(user.getCompany() != null ? user.getCompany().getCompanyName() : null)
                .status(status)
                .at(user.getLastActiveAt())
                .build();
        messagingTemplate.convertAndSend("/topic/presence", event);
    }

    private Integer parseUserId(String name) {
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            log.warn("Could not parse WebSocket principal as userId: {}", name);
            return null;
        }
    }
}
