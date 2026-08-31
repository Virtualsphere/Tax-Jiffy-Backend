package com.gst_reconsilation.presence.config;

import com.gst_reconsilation.config.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * The WebSocket-equivalent of JwtAuthFilter: the HTTP filter's SecurityContext from the initial
 * handshake request doesn't propagate to the long-lived STOMP session, so CONNECT frames are
 * authenticated here instead, using the same JwtUtil the rest of the app already relies on. The
 * token travels as a STOMP header (not a query param) so it never lands in access logs.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = (authHeader != null && authHeader.startsWith("Bearer "))
                    ? authHeader.substring(7) : null;

            if (token == null || !jwtUtil.isValid(token)) {
                throw new MessagingException("Invalid or missing token on WebSocket CONNECT");
            }

            Integer userId = jwtUtil.getUserId(token);
            accessor.setUser(new StompPrincipal(userId));
        }

        return message;
    }

    /** Wraps the userId as a java.security.Principal so Spring exposes it as the STOMP session's user. */
    public record StompPrincipal(Integer userId) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(userId);
        }
    }
}
