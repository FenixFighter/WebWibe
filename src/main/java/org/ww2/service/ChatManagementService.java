package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.ww2.dto.ChatRequest;
import org.ww2.dto.WebSocketMessage;
import org.ww2.entity.ChatAssignment;
import org.ww2.entity.User;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatManagementService {

    private final ChatService chatService;
    private final ChatAssignmentService chatAssignmentService;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;

    public String createOrUpdateChat(ChatRequest request) {
        String chatId = request.getChatId();

        if (chatId == null || chatId.trim().isEmpty()) {
            chatId = generateChatId();
            chatService.createChat(chatId, request.getCustomerName(), request.getCustomerEmail());

            WebSocketMessage chatCreated = WebSocketMessage.createChatCreated(chatId);
            messagingTemplate.convertAndSend("/topic/chat", chatCreated);

            log.info("Created new chat with ID: {}", chatId);
        } else {

            if (request.getCustomerName() != null || request.getCustomerEmail() != null) {
                chatService.createChat(chatId, request.getCustomerName(), request.getCustomerEmail());
                log.info("Updated chat {} with customer info", chatId);
            }
        }

        return chatId;
    }

    public boolean isSupportUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        User user = authService.getUserByToken(token);
        boolean isSupport = (user != null && user.getRole().equals(User.UserRole.SUPPORT));

        log.info("Token check - User: {}, Role: {}, IsSupportUser: {}", 
            user != null ? user.getUsername() : "null", 
            user != null ? user.getRole() : "null", 
            isSupport);

        return isSupport;
    }

    public Optional<ChatAssignment> getChatAssignment(String chatId) {
        return chatAssignmentService.getChatAssignment(chatId);
    }

    public boolean isChatAssignedToSupport(String chatId) {
        return getChatAssignment(chatId).isPresent();
    }

    public void notifySupportAboutChat(String chatId, String message) {
        WebSocketMessage notification = new WebSocketMessage();
        notification.setType("CHAT_ACTIVITY");
        notification.setChatId(chatId);
        notification.setSender("SYSTEM");
        notification.setContent("New message in chat: " + message.substring(0, Math.min(message.length(), 50)) + "...");
        notification.setTimestamp(System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/support/activity", notification);
    }

    private String generateChatId() {
        return UUID.randomUUID().toString();
    }
}
