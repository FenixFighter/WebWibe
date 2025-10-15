package org.ww2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    private String type;
    private String chatId;
    private String content;
    private String sender;
    private Long timestamp;
    private AiRating rating; // AI response rating
    
    public static WebSocketMessage createUserMessage(String chatId, String content) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("MESSAGE");
        message.setChatId(chatId);
        message.setContent(content);
        message.setSender("USER");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    public static WebSocketMessage createAiMessage(String chatId, String content) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("MESSAGE");
        message.setChatId(chatId);
        message.setContent(content);
        message.setSender("AI");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    public static WebSocketMessage createChatCreated(String chatId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("CHAT_CREATED");
        message.setChatId(chatId);
        message.setContent("Chat created successfully");
        message.setSender("SYSTEM");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    public static WebSocketMessage createError(String error) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("ERROR");
        message.setContent(error);
        message.setSender("SYSTEM");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    public static WebSocketMessage createSupportMessage(String chatId, String content, String supportUsername) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("MESSAGE");
        message.setChatId(chatId);
        message.setContent(content);
        message.setSender("SUPPORT");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    public static WebSocketMessage createEscalationMessage(String chatId, String content) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("ESCALATION");
        message.setChatId(chatId);
        message.setContent(content);
        message.setSender("SYSTEM");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
}
