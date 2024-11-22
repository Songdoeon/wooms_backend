package com.ee06.wooms.domain.chat.common;

import com.ee06.wooms.domain.chat.ChannelRepository;
import com.ee06.wooms.domain.chat.SessionRepository;
import com.ee06.wooms.domain.chat.entity.Channel;
import com.ee06.wooms.domain.chat.entity.Woom;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelDisconnectListener {
    private final ChannelRepository channelRepository;
    private final SessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("Handling disconnect for STOMP session: {}", sessionId);

        try {
            // Get user information from session repository
            Woom woom = sessionRepository.get(sessionId);
            if (woom != null && woom.getWoomsId() != null) {
                // Remove from channel
                Channel channel = channelRepository.get(woom.getWoomsId());
                if (channel != null) {
                    channel.removeWoom(woom);
                    channelRepository.put(woom.getWoomsId(), channel);

                    // Notify others about disconnection
                    messagingTemplate.convertAndSend(
                            "/ws/wooms/disconnect/" + woom.getWoomsId(),
                            woom
                    );

                    log.info("Removed user {} from channel {}",
                            woom.getNickname(), woom.getWoomsId());
                }
            }

            // Clean up session
            sessionRepository.remove(sessionId);
            log.info("Cleaned up session: {}", sessionId);

        } catch (Exception e) {
            log.error("Error handling disconnect: ", e);
        }
    }
//    @EventListener
//    public void handleSessionDisconnect(SessionDisconnectEvent event) {
////        String sessionId = event.getSessionId();
////        Woom woom = sessionRepository.get(sessionId);
////        channelRepository.get(woom.getWoomsId()).removeWoom(woom);
////        sessionRepository.remove(sessionId);
//        String sessionId = event.getSessionId();
//        log.info("Handling disconnect for session: {}", sessionId);
//        try {
//            Woom woom = sessionRepository.get(sessionId);
//            if (woom != null && woom.getWoomsId() != null) {
//                Channel channel = channelRepository.get(woom.getWoomsId());
//                if (channel != null) {
//                    // Remove woom from channel
//                    channel.removeWoom(woom);
//                    channelRepository.put(woom.getWoomsId(), channel);
//
//                    // Notify other users about the disconnection
//                    messagingTemplate.convertAndSend(
//                            "/ws/wooms/disconnect/" + woom.getWoomsId(),
//                            objectMapper.writeValueAsString(woom)
//                    );
//
//                    log.info("Successfully removed user {} from channel {}",
//                            woom.getNickname(), woom.getWoomsId());
//                }
//            }
//
//            // Clean up session
//            sessionRepository.remove(sessionId);
//            log.info("Session cleanup completed for: {}", sessionId);
//
//        } catch (Exception e) {
//            log.error("Error handling disconnect for session {}: {}",
//                    sessionId, e.getMessage(), e);
//        }
//    }
}


