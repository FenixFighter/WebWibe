package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ww2.entity.Chat;
import org.ww2.entity.ChatAssignment;
import org.ww2.entity.ChatMessage;
import org.ww2.entity.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {

    private final AuthService authService;
    private final ChatAssignmentService chatAssignmentService;
    private final ChatService chatService;

    public User validateSupportUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        String cleanToken = token.replace("Bearer ", "");
        User user = authService.getUserByToken(cleanToken);

        if (user == null || !user.getRole().equals(User.UserRole.SUPPORT)) {
            return null;
        }

        return user;
    }

    public List<ChatInfo> getAllChats() {
        List<Chat> allChats = chatService.getAllChats();

        return allChats.stream()
            .map(this::mapChatToInfo)
            .collect(Collectors.toList());
    }

    public List<ChatInfo> getAssignedChats(Long userId) {
        List<ChatAssignment> assignments = chatAssignmentService.getAssignedChats(userId);

        return assignments.stream()
            .map(this::mapAssignmentToInfo)
            .collect(Collectors.toList());
    }

    public List<MessageInfo> getChatHistory(String chatId) {
        List<ChatMessage> messages = chatService.getChatHistory(chatId);

        return messages.stream()
            .map(this::mapMessageToInfo)
            .collect(Collectors.toList());
    }

    public ChatAssignment assignChatToSupport(String chatId) {
        return chatAssignmentService.assignChatToSupport(chatId);
    }

    public void resolveChat(String chatId) {
        chatAssignmentService.resolveChat(chatId);
    }

    private ChatInfo mapChatToInfo(Chat chat) {
        ChatInfo info = new ChatInfo();
        info.setChatId(chat.getChatId());
        info.setStatus("ACTIVE");
        info.setAssignedAt(chat.getCreatedAt());
        info.setCustomerName(chat.getCustomerName());
        info.setCustomerEmail(chat.getCustomerEmail());

        List<ChatMessage> recentMessages = chatService.getChatHistory(chat.getChatId());
        boolean hasLowRating = recentMessages.stream()
            .filter(msg -> msg.getSenderType() == ChatMessage.SenderType.AI)
            .anyMatch(msg -> {
                String content = msg.getContent().toLowerCase();
                return content.contains("i don't know") ||
                       content.contains("i can't help") ||
                       content.contains("contact support") ||
                       content.contains("human assistance") ||
                       content.contains("escalate") ||
                       content.contains("no answers found") ||
                       content.contains("cannot help");
            });
        info.setLowRating(hasLowRating);

        return info;
    }

    private ChatInfo mapAssignmentToInfo(ChatAssignment assignment) {
        ChatInfo info = new ChatInfo();
        info.setChatId(assignment.getChatId());
        info.setStatus(assignment.getStatus().name());
        info.setAssignedAt(assignment.getAssignedAt());

        return info;
    }

    private MessageInfo mapMessageToInfo(ChatMessage message) {
        MessageInfo info = new MessageInfo();
        info.setContent(message.getContent());
        info.setSenderType(message.getSenderType().name());
        info.setCreatedAt(message.getCreatedAt());
        return info;
    }

    public static class ChatInfo {
        private String chatId;
        private String status;
        private java.time.LocalDateTime assignedAt;
        private String customerName;
        private String customerEmail;
        private boolean lowRating;

        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.LocalDateTime getAssignedAt() { return assignedAt; }
        public void setAssignedAt(java.time.LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public boolean isLowRating() { return lowRating; }
        public void setLowRating(boolean lowRating) { this.lowRating = lowRating; }
    }

    public static class MessageInfo {
        private String content;
        private String senderType;
        private java.time.LocalDateTime createdAt;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
